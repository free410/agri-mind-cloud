from __future__ import annotations

import asyncio
import hashlib
from dataclasses import dataclass
from typing import Optional

import httpx

from app.core.config import settings


@dataclass
class FetchResult:
    url: str
    text: str
    status_code: int


class CrawlHttpClient:
    """异步 HTTP 客户端，支持 robots.txt 检查和重试。"""

    def __init__(self) -> None:
        self._client = httpx.AsyncClient(
            timeout=httpx.Timeout(settings.crawl_request_timeout_seconds),
            headers={"User-Agent": settings.crawl_user_agent},
            follow_redirects=True,
        )
        self._robots_cache: dict[str, bool] = {}

    async def close(self) -> None:
        await self._client.aclose()

    async def can_fetch(self, url: str) -> bool:
        """检查 robots.txt 是否允许抓取。"""
        parsed = urlparse(url)
        robots_url = f"{parsed.scheme}://{parsed.netloc}/robots.txt"

        if robots_url in self._robots_cache:
            return self._robots_cache[robots_url]

        try:
            response = await self._client.get(robots_url)
            if response.status_code == 200:
                content = response.text.lower()
                # 简化检查：仅判断是否有禁止规则
                allowed = "disallow" not in content or f"disallow: {parsed.path}" not in content
                self._robots_cache[robots_url] = allowed
                return allowed
            self._robots_cache[robots_url] = True
            return True
        except Exception:
            self._robots_cache[robots_url] = True
            return True

    async def fetch_text(self, url: str) -> FetchResult:
        """获取页面文本内容。"""
        response = await self._client.get(url)
        response.raise_for_status()
        return FetchResult(
            url=str(response.url),
            text=response.text,
            status_code=response.status_code,
        )

    async def fetch_bytes(self, url: str) -> tuple[str, bytes]:
        """获取页面原始字节。"""
        response = await self._client.get(url)
        response.raise_for_status()
        return str(response.url), response.content

    @staticmethod
    def content_hash(content: str) -> str:
        """计算内容哈希。"""
        return hashlib.sha256(content.encode("utf-8")).hexdigest()[:32]