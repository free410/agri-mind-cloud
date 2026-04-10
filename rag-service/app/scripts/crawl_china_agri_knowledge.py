from __future__ import annotations

import argparse
import asyncio
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from app.schemas.crawl import CrawlRequest
from app.services.knowledge.crawl_service import KnowledgeCrawlService


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="抓取中国农业公开知识并保存为结构化文档")
    parser.add_argument("--source", action="append", dest="source_names", help="按 source_name 指定抓取来源，可重复传入")
    parser.add_argument("--limit", type=int, default=None, help="每个来源抓取的文章上限")
    parser.add_argument("--force", action="store_true", help="忽略增量去重，强制重抓")
    return parser.parse_args()


async def main() -> None:
    args = parse_args()
    request = CrawlRequest(
        source_names=args.source_names,
        limit_per_source=args.limit,
        force=args.force,
    )
    service = KnowledgeCrawlService()
    summary = await service.crawl(request)

    print("抓取完成")
    print(f"开始时间: {summary.started_at}")
    print(f"结束时间: {summary.finished_at}")
    print(f"发现链接总数: {summary.total_discovered_links}")
    print(f"清洗文章总数: {summary.total_cleaned_articles}")
    print(f"重复过滤总数: {summary.total_duplicates}")
    print(f"异常总数: {summary.total_errors}")
    print("")
    for source_summary in summary.source_summaries:
        print(f"[{source_summary.source_name}]")
        if source_summary.skipped_reason:
            print(f"  跳过原因: {source_summary.skipped_reason}")
        print(f"  列表页: {source_summary.list_page_count}")
        print(f"  发现链接: {source_summary.discovered_link_count}")
        print(f"  清洗文章: {source_summary.cleaned_article_count}")
        print(f"  重复: {source_summary.duplicate_count}")
        print(f"  过滤: {source_summary.filtered_count}")
        print(f"  错误: {source_summary.error_count}")


if __name__ == "__main__":
    asyncio.run(main())
