from __future__ import annotations

import re
from datetime import datetime
from pathlib import Path

from fastapi import APIRouter, File, Form, HTTPException, Query, UploadFile
from starlette.concurrency import run_in_threadpool

from app.core.config import settings
from app.core.response import success
from app.schemas.crawl import CrawlRequest
from app.services.knowledge.crawl_service import KnowledgeCrawlService
from app.services.knowledge.document_loader import KnowledgeDocumentLoader
from app.services.knowledge.index_service import KnowledgeIndexService
from app.services.qdrant_service import qdrant_service

router = APIRouter(prefix="/knowledge", tags=["knowledge"])

MAX_UPLOAD_BYTES = 20 * 1024 * 1024
ALLOWED_CATEGORIES = {"base", "crop", "tech", "sop", "device"}


@router.get("/crawl/status")
def get_crawl_status() -> dict:
    service = KnowledgeCrawlService()
    return success(service.get_status())


@router.post("/crawl")
async def crawl_public_knowledge(request: CrawlRequest) -> dict:
    service = KnowledgeCrawlService()
    summary = await service.crawl(request)
    return success(summary, message="crawl finished")


@router.post("/upload")
async def upload_knowledge(
    file: UploadFile = File(...),
    category: str = Form("crop"),
) -> dict:
    """上传知识文档并增量写入向量库。

    文件会保存到 `knowledge/<category>/` 下，然后对该目录做一次增量索引
    （切片 id 由文档路径与序号决定，重复导入不会产生重复向量）。
    """
    filename = Path(file.filename or "").name
    if not filename:
        raise HTTPException(status_code=400, detail="缺少文件名")

    ext = Path(filename).suffix.lower()
    if ext not in KnowledgeDocumentLoader.SUPPORTED_EXTS:
        supported = "、".join(sorted(KnowledgeDocumentLoader.SUPPORTED_EXTS))
        raise HTTPException(status_code=400, detail=f"不支持的文件类型 {ext or '(无扩展名)'}，支持：{supported}")

    content = await file.read()
    if not content:
        raise HTTPException(status_code=400, detail="文件内容为空")
    if len(content) > MAX_UPLOAD_BYTES:
        raise HTTPException(status_code=400, detail="文件超过 20MB 限制")

    safe_category = (category or "").strip().lower()
    if safe_category not in ALLOWED_CATEGORIES:
        safe_category = "crop"
    safe_category = re.sub(r"[^a-z0-9_-]", "", safe_category) or "crop"

    root_dir = Path(settings.knowledge_root_dir)
    target_dir = root_dir / safe_category
    target_dir.mkdir(parents=True, exist_ok=True)
    target_path = target_dir / filename
    target_path.write_bytes(content)

    service = KnowledgeIndexService()
    stats = await run_in_threadpool(service.run, root_dir=root_dir, include_cleaned_jsonl=False)

    return success(
        {
            "file_name": filename,
            "saved_path": target_path.relative_to(root_dir).as_posix(),
            "file_count": stats.file_count,
            "document_count": stats.document_count,
            "chunk_count": stats.chunk_count,
            "upserted_count": stats.upserted_count,
        },
        message="upload finished",
    )


@router.get("/documents")
def list_documents() -> dict:
    """列出知识库中的文档及对应的向量条数"""
    root_dir = Path(settings.knowledge_root_dir)
    items: list[dict] = []
    if root_dir.exists():
        for path in sorted(root_dir.rglob("*")):
            if not path.is_file():
                continue
            if path.name.startswith(".") or path.suffix.lower() not in KnowledgeDocumentLoader.SUPPORTED_EXTS:
                continue
            relative_path = path.relative_to(root_dir).as_posix()
            stat = path.stat()
            items.append(
                {
                    "path": relative_path,
                    "name": path.name,
                    "category": relative_path.split("/")[0] if "/" in relative_path else "root",
                    "size_bytes": stat.st_size,
                    "updated_at": datetime.fromtimestamp(stat.st_mtime).isoformat(timespec="seconds"),
                    "vector_count": qdrant_service.count_points_by_source(relative_path),
                }
            )
    return success({"items": items, "total": len(items)})


@router.delete("/documents")
def delete_document(path: str = Query(..., description="相对知识目录的路径")) -> dict:
    """删除知识文档，并同步清除其全部向量"""
    root_dir = Path(settings.knowledge_root_dir).resolve()
    target = (root_dir / path).resolve()
    if root_dir not in target.parents or not target.is_file():
        raise HTTPException(status_code=404, detail="文档不存在或路径非法")

    relative_path = target.relative_to(root_dir).as_posix()
    target.unlink()
    deleted_points = qdrant_service.delete_points_by_source(relative_path)
    return success(
        {"path": relative_path, "deleted_points": deleted_points},
        message="deleted",
    )
