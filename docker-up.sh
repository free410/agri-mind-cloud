#!/bin/bash
# ============================================================
# 智慧农业 Docker 统一启动脚本
# 使用前提：
#   1. 确保已安装 Docker Desktop
#   2. 将 docker/stack.env.example 复制为 .env 并按需修改配置（脚本会自动补齐随机密钥）
#   3. 确保前端项目 smart-agriculture-web 存在
# ============================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "============================================"
echo "  智慧农业 Docker 统一启动脚本"
echo "============================================"

# 检查 .env 是否存在
if [ ! -f ".env" ]; then
    echo "⚠️  .env 文件不存在，从 .env.example 复制..."
    if [ -f "docker/stack.env.example" ]; then
        cp "docker/stack.env.example" ".env"
        echo "✅ .env 已创建，请编辑 .env 文件配置必要的密钥"
    else
        echo "❌ 找不到 docker/stack.env.example"
        exit 1
    fi
fi

# 自动补齐必填密钥（避免使用仓库内置的开发默认值）
ensure_secret() {
    local key="$1"
    local min_length="$2"
    local current
    current="$(grep -E "^${key}=" .env | head -n1 | cut -d= -f2-)"
    if [[ -n "${current}" && "${current}" != 请替换* && "${current}" != changeme* && ${#current} -ge ${min_length} ]]; then
        return
    fi
    local secret
    if command -v openssl >/dev/null 2>&1; then
        secret="$(openssl rand -base64 48 | tr -d '\n')"
    else
        secret="$(head -c 48 /dev/urandom | base64 | tr -d '\n')"
    fi
    if grep -qE "^${key}=" .env; then
        sed -i "s|^${key}=.*|${key}=${secret}|" .env
    else
        printf "\n%s=%s\n" "${key}" "${secret}" >> .env
    fi
    echo "  已为 ${key} 生成随机密钥"
}

ensure_secret JWT_SECRET_KEY 32
ensure_secret NEO4J_PASSWORD 16

# 检查 Docker 是否运行
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker 未运行，请先启动 Docker Desktop"
    exit 1
fi

echo "🚀 开始构建并启动所有服务..."
echo ""
echo "服务包括："
echo "  - MySQL (端口 3307)"
echo "  - Neo4j (端口 7474/7687)"
echo "  - Qdrant (端口 6333/6334)"
echo "  - RAG Service (端口 9001)"
echo "  - Spring Boot Backend (端口 8090)"
echo "  - 前端 Nginx (端口 8088)"
echo ""

# 构建并启动所有服务
docker compose up -d --build

echo ""
echo "============================================"
echo "  启动完成！"
echo "============================================"
echo ""
echo "访问地址："
echo "  前端：http://localhost:8088"
echo "  后端 API：http://localhost:8090/api/swagger-ui.html"
echo "  Neo4j Browser：http://localhost:7474"
echo "  Qdrant Dashboard：http://localhost:6333/dashboard"
echo ""
echo "查看日志：docker compose logs -f"
echo "停止服务：docker compose down"
echo "删除数据：docker compose down -v"
