#!/usr/bin/env bash
# 删除 Neo4j 数据卷并重启容器。在仓库根目录执行：bash docker/reset-neo4j.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "[1/3] 停止并移除 Neo4j 容器..."
docker compose stop smartagri-neo4j 2>/dev/null || true
docker compose rm -f smartagri-neo4j 2>/dev/null || true
docker rm -f smartagri-neo4j 2>/dev/null || true

echo "[2/3] 删除名称包含 smartagri_neo4j_data 的卷..."
for v in $(docker volume ls -q | grep smartagri_neo4j_data || true); do
  docker volume rm -f "$v" && echo "  已删除: $v"
done

echo "[3/3] 启动 Neo4j..."
docker compose up -d smartagri-neo4j
echo "完成。请确认 Spring 侧 neo4j 密码与 NEO4J_PASSWORD 一致后重启后端。"
