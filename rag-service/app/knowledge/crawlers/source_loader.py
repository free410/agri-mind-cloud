from __future__ import annotations

import yaml
from pathlib import Path
from typing import Any

from app.core.config import settings
from app.schemas.crawl import CrawlSourceConfig


class CrawlSourceLoader:
    """加载抓取源配置。"""

    def load(self) -> list[CrawlSourceConfig]:
        """从 YAML 配置文件加载所有源。"""
        config_path = self._find_config_file()
        if not config_path or not config_path.exists():
            return self._get_default_sources()

        try:
            with open(config_path, "r", encoding="utf-8") as f:
                data = yaml.safe_load(f)
                sources_data = data.get("sources", []) if isinstance(data, dict) else data
                return [self._parse_source(s) for s in sources_data]
        except Exception:
            return self._get_default_sources()

    def _find_config_file(self) -> Path | None:
        """查找配置文件。"""
        # 优先使用环境变量指定的配置
        if settings.crawl_sources_config:
            path = Path(settings.crawl_sources_config)
            if path.exists():
                return path

        # 尝试默认路径
        default_path = Path("sources/china_agri_sources.yaml")
        if default_path.exists():
            return default_path

        # 尝试 sources 目录
        sources_dir = Path("sources")
        if sources_dir.exists():
            yaml_files = list(sources_dir.glob("*.yaml")) + list(sources_dir.glob("*.yml"))
            return yaml_files[0] if yaml_files else None

        return None

    def _parse_source(self, data: dict[str, Any]) -> CrawlSourceConfig:
        """解析单个源配置。"""
        return CrawlSourceConfig(
            source_name=data.get("source_name", "unknown"),
            enabled=data.get("enabled", True),
            base_url=data.get("base_url", ""),
            list_pages=data.get("list_pages", []),
            topic_tags=data.get("topic_tags", []),
            required_keywords=data.get("required_keywords", []),
            list_page_link_pattern=data.get("list_page_link_pattern"),
        )

    def _get_default_sources(self) -> list[CrawlSourceConfig]:
        """获取默认源配置。"""
        return [
            CrawlSourceConfig(
                source_name="中国农业农村信息网",
                enabled=True,
                base_url="http://www.scagri.gov.cn",
                list_pages=[
                    "http://www.scagri.gov.cn/nykj/",
                ],
                topic_tags=["农业技术", "种植技术"],
                required_keywords=["水稻", "小麦", "玉米", "种植", "病虫害", "施肥", "灌溉"],
            ),
        ]