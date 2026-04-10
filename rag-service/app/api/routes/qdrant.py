from fastapi import APIRouter, HTTPException

from app.core.response import success
from app.schemas.qdrant import (
    QdrantFilterSearchRequest,
    QdrantInitCollectionRequest,
    QdrantSearchRequest,
    QdrantUpsertRequest,
)
from app.services.qdrant_service import qdrant_service

router = APIRouter(prefix="/rag/qdrant")


@router.post("/init")
def init_qdrant_collection(request: QdrantInitCollectionRequest):
    try:
        result = qdrant_service.init_collection(recreate=request.recreate)
        return success(data=result.model_dump(), message="collection 初始化成功")
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"collection 初始化失败: {exc}") from exc


@router.post("/upsert")
def qdrant_upsert(request: QdrantUpsertRequest):
    try:
        result = qdrant_service.upsert_documents(request)
        return success(data=result.model_dump(), message="文档写入成功")
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"文档写入失败: {exc}") from exc


@router.post("/search")
def qdrant_similarity_search(request: QdrantSearchRequest):
    try:
        result = qdrant_service.similarity_search(request)
        return success(data=result.model_dump())
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"相似检索失败: {exc}") from exc


@router.post("/search/filter")
def qdrant_filter_search(request: QdrantFilterSearchRequest):
    try:
        result = qdrant_service.filter_search(request)
        return success(data=result.model_dump())
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"过滤检索失败: {exc}") from exc
