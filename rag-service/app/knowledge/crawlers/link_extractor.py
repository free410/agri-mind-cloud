from __future__ import annotations

import re
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from app.schemas.crawl import CrawlSourceConfig


class LinkExtractor:
    """从列表页面提取文章链接。"""

    # 常见文章链接模式
    ARTICLE_PATTERNS = [
        re.compile(r'href=["\']([^"\']*(?:\.html?|\.htm)[^"\']*)["\']', re.IGNORECASE),
        re.compile(r'href=["\']([^"\']*article[^"\']*)["\']', re.IGNORECASE),
        re.compile(r'href=["\']([^"\']*news[^"\']*)["\']', re.IGNORECASE),
        re.compile(r'href=["\']([^"\']*info[^"\']*)["\']', re.IGNORECASE),
    ]

    # 需要排除的链接关键词
    EXCLUDE_KEYWORDS = ["login", "register", "about", "contact", "sitemap", "404"]

    def extract_article_links(
        self,
        html: str,
        list_page_url: str,
        source: CrawlSourceConfig,
    ) -> list[str]:
        """从列表页面提取文章链接。"""
        links: set[str] = set()

        # 尝试从 source 配置中获取正则
        if source.list_page_link_pattern:
            pattern = re.compile(source.list_page_link_pattern)
            matches = pattern.findall(html)
            for match in matches:
                if self._is_valid_link(match):
                    links.add(self._normalize_url(match, list_page_url))

        # 使用通用模式提取
        for pattern in self.ARTICLE_PATTERNS:
            matches = pattern.findall(html)
            for match in matches:
                if self._is_valid_link(match):
                    normalized = self._normalize_url(match, list_page_url)
                    if self._matches_base_url(normalized, str(source.base_url)):
                        links.add(normalized)

        return list(links)

    def _is_valid_link(self, link: str) -> bool:
        """验证链接是否有效。"""
        if not link or link.startswith("#") or link.startswith("javascript:"):
            return False
        if any(keyword in link.lower() for keyword in self.EXCLUDE_KEYWORDS):
            return False
        if link.startswith("/") and len(link) < 3:
            return False
        return True

    def _normalize_url(self, link: str, base_url: str) -> str:
        """将相对路径转换为绝对 URL。"""
        from urllib.parse import urljoin

        return urljoin(base_url, link)

    def _matches_base_url(self, url: str, base_url: str) -> bool:
        """检查 URL 是否属于源站点。"""
        from urllib.parse import urlparse

        try:
            target_parsed = urlparse(url)
            base_parsed = urlparse(base_url)
            return target_parsed.netloc == base_parsed.netloc
        except Exception:
            return False