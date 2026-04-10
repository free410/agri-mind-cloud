# -*- coding: utf-8 -*-
"""
知识图谱自动入库：可选先运行 generate_agri_graph_seed.py，再将 triples + entities POST 到后端。

用法示例：
  set SMART_AGRI_KG_PIPELINE_KEY=your-secret
  python kg_ingest_pipeline.py --generate

或使用管理员 JWT（可不调 pipeline 接口）：
  set SMART_AGRI_ADMIN_JWT=eyJ...
  python kg_ingest_pipeline.py --generate --use-jwt-import
"""
from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
from pathlib import Path

try:
    import urllib.error
    import urllib.request
except ImportError:
    pass


def _post_json(url: str, body: dict, headers: dict) -> tuple[int, str]:
    data = json.dumps(body, ensure_ascii=False).encode("utf-8")
    req = urllib.request.Request(
        url,
        data=data,
        headers={**headers, "Content-Type": "application/json; charset=utf-8"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=120) as resp:
            return resp.getcode(), resp.read().decode("utf-8", errors="replace")
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", errors="replace") if e.fp else ""
        return e.code, raw


def main() -> int:
    root = Path(__file__).resolve().parent.parent
    parser = argparse.ArgumentParser(description="知识图谱 bundle 入库")
    parser.add_argument(
        "--generate",
        action="store_true",
        help="先执行 generate_agri_graph_seed.py 写 resources/knowledge-graph/*.json",
    )
    parser.add_argument(
        "--base-url",
        default=os.environ.get("SMART_AGRI_API_BASE", "http://localhost:8080/api"),
        help="后端 context-path 含 /api",
    )
    parser.add_argument(
        "--seed-json",
        default=str(
            root / "src/main/resources/knowledge-graph/agri-graph-seed.json"
        ),
        help="三元组 JSON 数组路径",
    )
    parser.add_argument(
        "--entities-json",
        default=str(
            root / "src/main/resources/knowledge-graph/agri-graph-entities.json"
        ),
        help="实体说明 JSON 路径",
    )
    parser.add_argument(
        "--use-jwt-import",
        action="store_true",
        help="调用 /knowledge-graph/import（需 Authorization: Bearer）而非 pipeline",
    )
    args = parser.parse_args()

    if args.generate:
        gen = Path(__file__).resolve().parent / "generate_agri_graph_seed.py"
        subprocess.run([sys.executable, str(gen)], check=True, cwd=str(gen.parent))

    seed_path = Path(args.seed_json)
    ent_path = Path(args.entities_json)
    if not seed_path.is_file():
        print(f"缺少种子文件: {seed_path}", file=sys.stderr)
        return 2
    triples = json.loads(seed_path.read_text(encoding="utf-8"))
    entities = {}
    if ent_path.is_file():
        entities = json.loads(ent_path.read_text(encoding="utf-8"))
    bundle = {"triples": triples, "entities": entities}

    base = args.base_url.rstrip("/")
    if args.use_jwt_import:
        token = os.environ.get("SMART_AGRI_ADMIN_JWT", "").strip()
        if not token:
            print("请设置环境变量 SMART_AGRI_ADMIN_JWT", file=sys.stderr)
            return 2
        url = f"{base}/knowledge-graph/import"
        headers = {"Authorization": f"Bearer {token}"}
    else:
        key = os.environ.get("SMART_AGRI_KG_PIPELINE_KEY", "").strip()
        if not key:
            print("请设置 SMART_AGRI_KG_PIPELINE_KEY，或使用 --use-jwt-import", file=sys.stderr)
            return 2
        url = f"{base}/knowledge-graph/pipeline/ingest"
        headers = {"X-Pipeline-Ingest-Key": key}

    code, text = _post_json(url, bundle, headers)
    print(text)
    return 0 if 200 <= code < 300 else 1


if __name__ == "__main__":
    raise SystemExit(main())
