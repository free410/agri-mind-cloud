"""
知识库导入脚本

示例：
python scripts/index_knowledge.py
python scripts/index_knowledge.py --recreate
python scripts/index_knowledge.py --dry-run
python scripts/index_knowledge.py --chunk-size 520 --overlap 100
"""

import argparse
import sys
from pathlib import Path


CURRENT_FILE = Path(__file__).resolve()
PROJECT_ROOT = CURRENT_FILE.parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

from app.services.knowledge.index_service import knowledge_index_service  # noqa: E402


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="将知识库文档切片后导入 Qdrant")
    parser.add_argument("--knowledge-dir", default="knowledge", help="知识库目录，默认: knowledge")
    parser.add_argument("--chunk-size", type=int, default=500, help="切片大小，建议 300~600")
    parser.add_argument("--overlap", type=int, default=80, help="切片重叠字符数")
    parser.add_argument("--batch-size", type=int, default=64, help="写入 Qdrant 的批大小")
    parser.add_argument("--recreate", action="store_true", help="重建 collection")
    parser.add_argument("--dry-run", action="store_true", help="仅统计，不写入 Qdrant")
    parser.add_argument(
        "--skip-cleaned",
        action="store_true",
        help="不合并 data/cleaned/*.jsonl（仅用 knowledge/ 下文件）",
    )
    return parser.parse_args()


def main():
    args = parse_args()
    stats = knowledge_index_service.run(
        root_dir=args.knowledge_dir,
        chunk_size=args.chunk_size,
        overlap=args.overlap,
        batch_size=args.batch_size,
        recreate_collection=args.recreate,
        dry_run=args.dry_run,
        include_cleaned_jsonl=not args.skip_cleaned,
    )

    print("\n===== 知识库导入结果 =====")
    print(f"知识库目录: {stats.root_dir}")
    print(f"文件数量: {stats.file_count}")
    print(f"文档数量: {stats.document_count}")
    print(f"切片数量: {stats.chunk_count}")
    print(f"写入数量: {stats.upserted_count}")
    print(f"写入批次: {stats.batch_count}")
    print(f"模式: {'dry-run' if stats.dry_run else 'write'}")
    print("=========================\n")


if __name__ == "__main__":
    main()
