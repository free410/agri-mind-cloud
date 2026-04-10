from __future__ import annotations

from pathlib import Path

import yaml

from app.core.config import settings
from app.schemas.crawl import CrawlSourceConfig, SourceCollectionConfig


class CrawlSourceLoader:
    def __init__(self, config_path: str | None = None) -> None:
        self.config_path = Path(config_path or settings.crawl_sources_config_path)

    def load(self) -> list[CrawlSourceConfig]:
        if not self.config_path.exists():
            raise FileNotFoundError(f"未找到抓取配置文件: {self.config_path}")

        with self.config_path.open("r", encoding="utf-8") as file:
            content = yaml.safe_load(file) or {}

        data = SourceCollectionConfig.model_validate(content)
        return data.sources
