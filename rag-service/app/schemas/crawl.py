from __future__ import annotations

from datetime import datetime
from typing import Any

from pydantic import BaseModel, Field, HttpUrl


class ArticleSelectorRules(BaseModel):
    title_selectors: list[str] = Field(default_factory=lambda: ["h1", "title"])
    content_selectors: list[str] = Field(default_factory=lambda: ["article", "main", ".content"])
    publish_time_selectors: list[str] = Field(default_factory=list)
    remove_selectors: list[str] = Field(default_factory=list)


class CrawlSourceConfig(BaseModel):
    source_name: str
    base_url: HttpUrl
    allowed_domains: list[str] = Field(default_factory=list)
    list_pages: list[HttpUrl] = Field(default_factory=list)
    list_page_link_pattern: str | None = None
    topic_tags: list[str] = Field(default_factory=list)
    source_type: str = "crop_guide"
    topic: str = "agriculture"
    required_keywords: list[str] = Field(default_factory=list)
    enabled: bool = True
    min_content_length: int | None = None
    notes: str = ""


class SourceCollectionConfig(BaseModel):
    sources: list[CrawlSourceConfig] = Field(default_factory=list)


class CrawlRequest(BaseModel):
    source_names: list[str] | None = None
    force: bool = False
    limit_per_source: int | None = None


class CrawlStatusResponse(BaseModel):
    crawl_enabled: bool
    source_count: int
    processed_url_count: int
    processed_content_count: int
    last_run_summary: dict[str, Any] | None = None
    updated_at: str | None = None


class RawArticleRecord(BaseModel):
    doc_id: str
    title: str
    source_name: str
    source_url: str
    domain: str
    publish_time: str | None = None
    crawl_time: str = Field(default_factory=lambda: datetime.now().isoformat(timespec="seconds"))
    topic_tags: list[str] = Field(default_factory=list)
    html: str
    text_preview: str = ""
    url_hash: str


class CleanedKnowledgeRecord(BaseModel):
    doc_id: str
    title: str
    content: str
    source_name: str
    source_url: str
    domain: str
    publish_time: str | None = None
    crawl_time: str = Field(default_factory=lambda: datetime.now().isoformat(timespec="seconds"))
    source_type: str
    topic: str
    crop_type: str | None = None
    warning_type: str | None = None
    topic_tags: list[str] = Field(default_factory=list)
    url_hash: str
    content_hash: str


class CrawlRunSourceSummary(BaseModel):
    source_name: str
    enabled: bool
    skipped_reason: str | None = None
    list_page_count: int = 0
    discovered_link_count: int = 0
    fetched_article_count: int = 0
    cleaned_article_count: int = 0
    duplicate_count: int = 0
    filtered_count: int = 0
    error_count: int = 0


class CrawlRunSummary(BaseModel):
    started_at: str
    finished_at: str
    source_summaries: list[CrawlRunSourceSummary] = Field(default_factory=list)
    total_discovered_links: int = 0
    total_cleaned_articles: int = 0
    total_duplicates: int = 0
    total_errors: int = 0
