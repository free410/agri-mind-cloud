from __future__ import annotations

from fastapi import APIRouter

from app.core.response import success
from app.schemas.crawl import CrawlRequest
from app.services.knowledge.crawl_service import KnowledgeCrawlService

router = APIRouter(prefix="/knowledge", tags=["knowledge"])


@router.get("/crawl/status")
def get_crawl_status() -> dict:
    service = KnowledgeCrawlService()
    return success(service.get_status())


@router.post("/crawl")
async def crawl_public_knowledge(request: CrawlRequest) -> dict:
    service = KnowledgeCrawlService()
    summary = await service.crawl(request)
    return success(summary, message="crawl finished")
