from __future__ import annotations

from datetime import datetime
from typing import Iterable
from urllib.parse import urlparse

from app.core.config import settings
from app.knowledge.crawlers.http_client import CrawlHttpClient
from app.knowledge.crawlers.link_extractor import LinkExtractor
from app.knowledge.crawlers.source_loader import CrawlSourceLoader
from app.knowledge.parsers.article_parser import ArticleParser
from app.knowledge.storage.json_storage import CrawlStorage
from app.schemas.crawl import (
    CleanedKnowledgeRecord,
    CrawlRequest,
    CrawlRunSourceSummary,
    CrawlRunSummary,
    CrawlSourceConfig,
    CrawlStatusResponse,
)


class KnowledgeCrawlService:
    def __init__(self) -> None:
        self.source_loader = CrawlSourceLoader()
        self.link_extractor = LinkExtractor()
        self.article_parser = ArticleParser()
        self.storage = CrawlStorage()

    def list_sources(self) -> list[CrawlSourceConfig]:
        return self.source_loader.load()

    def get_status(self) -> CrawlStatusResponse:
        return self.storage.build_status(len(self.list_sources()))

    async def crawl(self, request: CrawlRequest) -> CrawlRunSummary:
        sources = self._filter_sources(self.list_sources(), request.source_names)
        http_client = CrawlHttpClient()
        started_at = datetime.now().isoformat(timespec="seconds")
        source_summaries: list[CrawlRunSourceSummary] = []
        total_discovered_links = 0
        total_cleaned_articles = 0
        total_duplicates = 0
        total_errors = 0

        try:
            for source in sources:
                source_summary = CrawlRunSourceSummary(source_name=source.source_name, enabled=source.enabled)
                if not settings.crawl_enabled:
                    source_summary.skipped_reason = "CRAWL_ENABLED=false"
                    source_summaries.append(source_summary)
                    continue
                if not source.enabled:
                    source_summary.skipped_reason = "source disabled"
                    source_summaries.append(source_summary)
                    continue

                try:
                    if not await http_client.can_fetch(str(source.base_url)):
                        source_summary.skipped_reason = "robots.txt denied"
                        source_summaries.append(source_summary)
                        continue

                    links = await self._collect_links(http_client, source, source_summary)
                    total_discovered_links += len(links)
                    limit = request.limit_per_source or settings.crawl_max_articles_per_source

                    for article_url in links[:limit]:
                        try:
                            if not await http_client.can_fetch(article_url):
                                source_summary.filtered_count += 1
                                continue
                            source_summary.fetched_article_count += 1
                            crawl_result = await self._crawl_article(http_client, source, article_url, request.force)
                            if crawl_result == "duplicate":
                                source_summary.duplicate_count += 1
                                continue
                            if crawl_result == "filtered":
                                source_summary.filtered_count += 1
                                continue
                            cleaned = crawl_result
                            source_summary.cleaned_article_count += 1
                            total_cleaned_articles += 1
                        except Exception:
                            source_summary.error_count += 1
                            total_errors += 1
                except Exception:
                    source_summary.error_count += 1
                    total_errors += 1

                total_duplicates += source_summary.duplicate_count
                source_summaries.append(source_summary)
        finally:
            await http_client.close()

        summary = CrawlRunSummary(
            started_at=started_at,
            finished_at=datetime.now().isoformat(timespec="seconds"),
            source_summaries=source_summaries,
            total_discovered_links=total_discovered_links,
            total_cleaned_articles=total_cleaned_articles,
            total_duplicates=total_duplicates,
            total_errors=total_errors,
        )
        self.storage.update_last_run(summary)
        return summary

    async def _collect_links(
        self,
        http_client: CrawlHttpClient,
        source: CrawlSourceConfig,
        summary: CrawlRunSourceSummary,
    ) -> list[str]:
        links: list[str] = []
        seen: set[str] = set()

        for list_page in source.list_pages:
            list_page_url = str(list_page)
            if not await http_client.can_fetch(list_page_url):
                continue
            try:
                result = await http_client.fetch_text(list_page_url)
            except Exception:
                summary.error_count += 1
                continue
            summary.list_page_count += 1
            extracted_links = self.link_extractor.extract_article_links(result.text, list_page_url, source)
            for link in extracted_links:
                if link in seen:
                    continue
                seen.add(link)
                links.append(link)

        summary.discovered_link_count = len(links)
        return links

    async def _crawl_article(
        self,
        http_client: CrawlHttpClient,
        source: CrawlSourceConfig,
        article_url: str,
        force: bool,
    ) -> CleanedKnowledgeRecord | str:
        url_hash = self.article_parser.build_raw_record(
            source=source,
            article_url=article_url,
            html="",
            title="",
            publish_time=None,
            topic_tags=source.topic_tags,
        ).url_hash
        if not force and self.storage.has_url(url_hash):
            return "duplicate"

        result = await http_client.fetch_text(article_url)
        cleaned_record = self.article_parser.parse_cleaned_record(
            source=source,
            article_url=result.url,
            html=result.text,
        )
        if cleaned_record is None:
            return "filtered"
        if source.required_keywords:
            merged_text = f"{cleaned_record.title}\n{cleaned_record.content}"
            if not any(keyword in merged_text for keyword in source.required_keywords):
                return "filtered"
        if not force and (self.storage.has_url(cleaned_record.url_hash) or self.storage.has_content(cleaned_record.content_hash)):
            return "duplicate"

        raw_record = self.article_parser.build_raw_record(
            source=source,
            article_url=result.url,
            html=result.text,
            title=cleaned_record.title,
            publish_time=cleaned_record.publish_time,
            topic_tags=cleaned_record.topic_tags,
        )
        self.storage.append_raw(raw_record)
        self.storage.append_cleaned(cleaned_record)
        self.storage.mark_processed(cleaned_record)
        return cleaned_record

    def _filter_sources(
        self,
        sources: Iterable[CrawlSourceConfig],
        source_names: list[str] | None,
    ) -> list[CrawlSourceConfig]:
        if not source_names:
            return list(sources)
        source_name_set = {name.strip() for name in source_names if name.strip()}
        return [source for source in sources if source.source_name in source_name_set]
