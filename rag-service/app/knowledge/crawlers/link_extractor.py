from __future__ import annotations

import re
from urllib.parse import urljoin, urlparse

from bs4 import BeautifulSoup

from app.schemas.crawl import CrawlSourceConfig


class LinkExtractor:
    def extract_article_links(self, html: str, page_url: str, source: CrawlSourceConfig) -> list[str]:
        soup = BeautifulSoup(html, "lxml")
        allowed_domains = {domain.lower() for domain in source.allowed_domains}
        compiled_patterns = [re.compile(pattern) for pattern in source.list_page_patterns]
        links: list[str] = []
        seen: set[str] = set()

        for anchor in soup.select("a[href]"):
            href = anchor.get("href", "").strip()
            if not href or href.startswith(("javascript:", "mailto:", "#")):
                continue

            absolute_url = urljoin(page_url, href)
            parsed = urlparse(absolute_url)
            if parsed.scheme not in {"http", "https"}:
                continue
            if parsed.netloc.lower() not in allowed_domains:
                continue
            if compiled_patterns and not any(pattern.search(absolute_url) for pattern in compiled_patterns):
                continue
            normalized_url = absolute_url.split("#", 1)[0]
            if normalized_url in seen:
                continue
            seen.add(normalized_url)
            links.append(normalized_url)

        return links
