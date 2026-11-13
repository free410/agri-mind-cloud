from __future__ import annotations

import re
from datetime import datetime
from typing import Iterable

from bs4 import BeautifulSoup, Tag

from app.core.config import settings
from app.knowledge.parsers.metadata_enricher import detect_crop_type, detect_warning_type, infer_topic
from app.knowledge.parsers.text_utils import domain_from_url, hash_text, normalize_text
from app.services.knowledge.knowledge_text_cleaner import clean_for_index
from app.schemas.crawl import CleanedKnowledgeRecord, CrawlSourceConfig, RawArticleRecord


PUBLISH_TIME_PATTERN = re.compile(r"(20\d{2}[-/.年]\d{1,2}[-/.月]\d{1,2}(?:日)?(?:\s+\d{1,2}:\d{2}(?::\d{2})?)?)")


class ArticleParser:
    def build_raw_record(
        self,
        *,
        source: CrawlSourceConfig,
        article_url: str,
        html: str,
        title: str,
        publish_time: str | None,
        topic_tags: list[str],
    ) -> RawArticleRecord:  
        doc_id = hash_text(article_url)[:24]
        preview = normalize_text(BeautifulSoup(html, "lxml").get_text(" ", strip=True))[:200]
        return RawArticleRecord(
            doc_id=doc_id,
            title=title,
            source_name=source.source_name,
            source_url=article_url,
            domain=domain_from_url(article_url),
            publish_time=publish_time,
            topic_tags=topic_tags,
            html=html,
            text_preview=preview,
            url_hash=hash_text(article_url),
        )

    def parse_cleaned_record(
        self,
        *,
        source: CrawlSourceConfig,
        article_url: str,
        html: str,
    ) -> CleanedKnowledgeRecord | None:
        soup = BeautifulSoup(html, "lxml")
        rules = source.article_selector_rules

        title = self._extract_first_text(soup, rules.title_selectors) or ""
        content_node = self._extract_content_node(soup, rules.content_selectors)
        if content_node is None:
            return None

        self._remove_noise(content_node, rules.remove_selectors)
        content = self._extract_structured_text(content_node)
        content = normalize_text(content)
        content = clean_for_index(content)
        if len(content) < (source.min_content_length or settings.crawl_min_content_length):
            return None

        publish_time = self._extract_publish_time(soup, rules.publish_time_selectors)
        merged_text = f"{title}\n{content}"
        return CleanedKnowledgeRecord(
            doc_id=hash_text(article_url)[:24],
            title=title or content[:30],
            content=content,
            source_name=source.source_name,
            source_url=article_url,
            domain=domain_from_url(article_url),
            publish_time=publish_time,
            crawl_time=datetime.now().isoformat(timespec="seconds"),
            source_type=source.source_type,
            topic=infer_topic(source, merged_text),
            crop_type=detect_crop_type(merged_text),
            warning_type=detect_warning_type(merged_text),
            topic_tags=source.topic_tags,
            url_hash=hash_text(article_url),
            content_hash=hash_text(content),
        )

    def _extract_first_text(self, soup: BeautifulSoup, selectors: Iterable[str]) -> str | None:
        for selector in selectors:
            node = soup.select_one(selector)
            if node:
                text = normalize_text(node.get_text(" ", strip=True))
                if text:
                    return text
        return None

    def _extract_content_node(self, soup: BeautifulSoup, selectors: Iterable[str]) -> Tag | None:
        for selector in selectors:
            node = soup.select_one(selector)
            if node:
                return node
        return soup.body

    def _remove_noise(self, node: Tag, selectors: Iterable[str]) -> None:
        for selector in selectors:
            for nested in node.select(selector):
                nested.decompose()
        for nested in node.select("script,style,noscript,iframe,footer,nav,form"):
            nested.decompose()

    def _extract_structured_text(self, node: Tag) -> str:
        blocks: list[str] = []
        for child in node.find_all(["h1", "h2", "h3", "h4", "p", "li"], recursive=True):
            text = normalize_text(child.get_text(" ", strip=True))
            if not text:
                continue
            blocks.append(text)
        if not blocks:
            blocks.append(normalize_text(node.get_text("\n", strip=True)))
        return "\n\n".join(blocks)

    def _extract_publish_time(self, soup: BeautifulSoup, selectors: Iterable[str]) -> str | None:
        for selector in selectors:
            node = soup.select_one(selector)
            if not node:
                continue
            text = normalize_text(node.get_text(" ", strip=True))
            match = PUBLISH_TIME_PATTERN.search(text)
            if match:
                return match.group(1)

        meta_candidates = [
            soup.find("meta", attrs={"name": "PubDate"}),
            soup.find("meta", attrs={"name": "publishdate"}),
            soup.find("meta", attrs={"property": "article:published_time"}),
        ]
        for meta in meta_candidates:
            if meta and meta.get("content"):
                return str(meta.get("content")).strip()
        return None
