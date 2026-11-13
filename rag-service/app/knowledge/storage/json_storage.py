from __future__ import annotations

import json
from pathlib import Path
from typing import Any
import re

from app.core.config import settings
from app.schemas.crawl import CleanedKnowledgeRecord, CrawlRunSummary, CrawlStatusResponse, RawArticleRecord


class CrawlStorage:
    def __init__(self) -> None:
        self.raw_dir = Path(settings.crawl_raw_dir)
        self.cleaned_dir = Path(settings.crawl_cleaned_dir)
        self.manifest_path = Path(settings.crawl_manifest_path)
        self.raw_dir.mkdir(parents=True, exist_ok=True)
        self.cleaned_dir.mkdir(parents=True, exist_ok=True)
        self._state = self._load_state()

    def has_url(self, url_hash: str) -> bool:
        return url_hash in self._state["processed_urls"]

    def has_content(self, content_hash: str) -> bool:
        return content_hash in self._state["processed_contents"]

    def append_raw(self, record: RawArticleRecord) -> None:
        file_path = self.raw_dir / f"{self._safe_file_stem(record.source_name)}.jsonl"
        with file_path.open("a", encoding="utf-8") as file:
            file.write(json.dumps(record.model_dump(), ensure_ascii=False) + "\n")

    def append_cleaned(self, record: CleanedKnowledgeRecord) -> None:
        file_path = self.cleaned_dir / f"{self._safe_file_stem(record.source_name)}.jsonl"
        with file_path.open("a", encoding="utf-8") as file:
            file.write(json.dumps(record.model_dump(), ensure_ascii=False) + "\n")

    def mark_processed(self, record: CleanedKnowledgeRecord) -> None:
        self._state["processed_urls"][record.url_hash] = {
            "source_url": record.source_url,
            "title": record.title,
            "crawl_time": record.crawl_time,
            "source_name": record.source_name,
        }
        self._state["processed_contents"][record.content_hash] = {
            "source_url": record.source_url,
            "title": record.title,
            "source_name": record.source_name,
        }

    def update_last_run(self, summary: CrawlRunSummary) -> None:
        self._state["last_run_summary"] = summary.model_dump()
        self._state["updated_at"] = summary.finished_at
        self._save_state()

    def build_status(self, source_count: int) -> CrawlStatusResponse:
        return CrawlStatusResponse(
            crawl_enabled=settings.crawl_enabled,
            source_count=source_count,
            processed_url_count=len(self._state["processed_urls"]),
            processed_content_count=len(self._state["processed_contents"]),
            last_run_summary=self._state.get("last_run_summary"),
            updated_at=self._state.get("updated_at"),
        )

    def _load_state(self) -> dict[str, Any]:
        if not self.manifest_path.exists():
            return {
                "processed_urls": {},
                "processed_contents": {},
                "last_run_summary": None,
                "updated_at": None,
            }
        with self.manifest_path.open("r", encoding="utf-8-sig") as file:
            data = json.load(file)
        return {
            "processed_urls": data.get("processed_urls", {}),
            "processed_contents": data.get("processed_contents", {}),
            "last_run_summary": data.get("last_run_summary"),
            "updated_at": data.get("updated_at"),
        }

    def _save_state(self) -> None:
        self.manifest_path.parent.mkdir(parents=True, exist_ok=True)
        with self.manifest_path.open("w", encoding="utf-8") as file:
            json.dump(self._state, file, ensure_ascii=False, indent=2)

    def _safe_file_stem(self, source_name: str) -> str:
        normalized = re.sub(r"[^\w\u4e00-\u9fff-]+", "_", source_name.strip())
        return normalized.strip("_") or "source"
