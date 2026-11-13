import json
import logging
from typing import Any

from fastapi import APIRouter, HTTPException, Request
from pydantic import ValidationError
from starlette.concurrency import run_in_threadpool

from app.api.rag_java_body import normalize_rag_answer_body
from app.core.response import success
from app.schemas.rag import (
    RagAnswerRequest,
    RagAnswerResponse,
    RagBriefingRequest,
    RagSearchRequest,
    UserFeedbackRequest,
)
from app.services.rag_service import rag_service

router = APIRouter(prefix="/rag")
logger = logging.getLogger(__name__)


@router.post("/search")
def rag_search(request: RagSearchRequest):
    result = rag_service.search(request)
    return success(data=result.model_dump())


@router.post("/answer")
async def rag_answer(http_request: Request):
    """
    手动读 body：避免 Body(dict) 在部分客户端下出现 body 未注入（loc=body required）。
    """
    raw = await http_request.body()
    if not raw.strip():
        logger.warning(
            "rag/answer 收到空 body: client=%s",
            http_request.client.host if http_request.client else None,
        )
        raise HTTPException(status_code=422, detail="请求体为空")
    try:
        payload: Any = json.loads(raw.decode("utf-8"))
    except (json.JSONDecodeError, UnicodeDecodeError) as exc:
        raise HTTPException(status_code=422, detail=f"JSON 解析失败: {exc}") from exc
    if not isinstance(payload, dict):
        raise HTTPException(status_code=422, detail="请求体必须是 JSON 对象")
    try:
        rag_req = RagAnswerRequest.model_validate(normalize_rag_answer_body(payload))
    except ValidationError as exc:
        logger.warning("rag/answer 字段校验失败: %s", exc.errors())
        raise HTTPException(status_code=422, detail=exc.errors()) from exc
    # RAG 内部使用同步 httpx / 同步向量检索，放到线程池避免阻塞事件循环
    result = await run_in_threadpool(rag_service.answer, rag_req)
    return success(data=result.model_dump())


@router.post("/briefing")
def rag_briefing(request: RagBriefingRequest):
    result = rag_service.briefing(request)
    return success(data=result.model_dump())


@router.post("/feedback")
def rag_feedback(request: UserFeedbackRequest):
    """
    用户反馈接口：用于「有用/无用」反馈埋点，支持迭代优化检索质量。

    请求示例：
    {
        "session_id": "rag-abc123",
        "question": "黄瓜叶子发黄有斑点是什么病？",
        "answer_id": "optional-answer-id",
        "is_helpful": true,
        "feedback_text": "回答很专业，但缺少具体用药建议",
        "retrieved_refs": ["chunk-001", "chunk-002"]
    }
    """
    result = rag_service.submit_feedback(request)
    return success(data=result.model_dump())
