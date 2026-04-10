from __future__ import annotations

import asyncio
from dataclasses import dataclass
from urllib.parse import urljoin, urlparse
from urllib.robotparser import RobotFileParser

import httpx

from app.core.config import settings


@dataclass(slots=True)
class FetchResult:
    url: str
    text: str
    status_code: int


class CrawlHttpClient:
    def __init__(self) -> None:
        self._client = httpx.AsyncClient(
            timeout=settings.crawl_request_timeout_seconds,
            headers={"User-Agent": settings.crawl_user_agent, "Accept-Language": "zh-CN,zh;q=0.9"},
            follow_redirects=True,
        )
        self._robots_cache: dict[str, RobotFileParser | None] = {}

    async def close(self) -> None:
        await self._client.aclose()

    async def fetch_text(self, url: str) -> FetchResult:
        response = await self._client.get(url)
        response.raise_for_status()
        await asyncio.sleep(settings.crawl_request_interval_seconds)
        return FetchResult(url=str(response.url), text=response.text, status_code=response.status_code)

    async def can_fetch(self, url: str) -> bool:
        parsed = urlparse(url)
        robots = await self._load_robots(parsed.scheme, parsed.netloc)
        if robots is None:
            return True
        return robots.can_fetch(settings.crawl_user_agent, url)

    async def _load_robots(self, scheme: str, netloc: str) -> RobotFileParser | None:
        key = f"{scheme}://{netloc}"
        if key in self._robots_cache:
            return self._robots_cache[key]

        robots_url = urljoin(key, "/robots.txt")
        try:
            response = await self._client.get(robots_url)
            if response.status_code == 404:
                self._robots_cache[key] = None
                return None
            response.raise_for_status()
        except httpx.HTTPError:
            parser = RobotFileParser()
            parser.parse(["User-agent: *", "Disallow: /"])
            self._robots_cache[key] = parser
            return self._robots_cache[key]

        parser = RobotFileParser()
        parser.set_url(robots_url)
        parser.parse(response.text.splitlines())
        self._robots_cache[key] = parser
        return parser
