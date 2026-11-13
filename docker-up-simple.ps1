# ============================================================
# 智慧农业 Docker 启动脚本 (Windows PowerShell) - 离线版
# 适用于：无法从 Docker Hub 拉取镜像的环境
# ============================================================

$SCRIPT_DIR = $PSScriptRoot
Set-Location $SCRIPT_DIR

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  智慧农业 Docker 启动脚本 (离线版)" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan

# 检查 Docker 是否运行
try {
    $null = docker info 2>$null
} catch {
    Write-Host "❌ Docker 未运行，请先启动 Docker Desktop" -ForegroundColor Red
    exit 1
}

# 检查 .env 是否存在
if (-not (Test-Path ".env")) {
    Write-Host "⚠️  .env 文件不存在，从 .env.example 复制..." -ForegroundColor Yellow
    if (Test-Path "docker\stack.env.example") {
        Copy-Item "docker\stack.env.example" ".env"
        Write-Host "✅ .env 已创建" -ForegroundColor Green
    } else {
        Write-Host "❌ 找不到 docker\stack.env.example" -ForegroundColor Red
        exit 1
    }
}

# 检查前端 dist 目录
$frontendDistPath = "smart-agriculture-web\dist"
if (-not (Test-Path $frontendDistPath)) {
    Write-Host "⚠️  前端未构建，正在构建前端..." -ForegroundColor Yellow
    if (Test-Path "smart-agriculture-web\package.json") {
        Set-Location $frontendDistPath
        # 检查 npm 是否可用
        $npmCheck = Get-Command npm -ErrorAction SilentlyContinue
        if ($npmCheck) {
            Set-Location $SCRIPT_DIR
            Set-Location "smart-agriculture-web"
            Write-Host "运行 npm install..." -ForegroundColor Gray
            npm install
            Write-Host "运行 npm run build..." -ForegroundColor Gray
            npm run build
            Set-Location $SCRIPT_DIR
        } else {
            Write-Host "❌ npm 不可用，请先安装 Node.js" -ForegroundColor Red
            exit 1
        }
    } else {
        Write-Host "❌ 前端目录不存在: smart-agriculture-web" -ForegroundColor Red
        exit 1
    }
}

Set-Location $SCRIPT_DIR

Write-Host "🚀 启动服务（使用已有镜像 + 前端 dist）..." -ForegroundColor Green
Write-Host ""
Write-Host "服务包括：" -ForegroundColor Cyan
Write-Host "  - Neo4j (端口 7474/7687) - 已有镜像"
Write-Host "  - Qdrant (端口 6333/6334) - 已有镜像"
Write-Host "  - 前端 Nginx (端口 8088)"
Write-Host ""
Write-Host "注意：MySQL、RAG Service、Spring Boot 未启动（需要 Docker Hub 镜像）" -ForegroundColor Yellow
Write-Host ""

# 启动 Neo4j 和 Qdrant（已有镜像）
docker compose -f "d:\codex\zhihuinongye\docker-compose-simple.yml" up -d

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  启动完成！" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "访问地址：" -ForegroundColor White
Write-Host "  前端：http://localhost:8088"
Write-Host "  Neo4j Browser：http://localhost:7474"
Write-Host "  Qdrant Dashboard：http://localhost:6333/dashboard"
Write-Host ""
Write-Host "查看日志：docker compose -f docker-compose-simple.yml logs -f" -ForegroundColor Gray
Write-Host "停止服务：docker compose -f docker-compose-simple.yml down" -ForegroundColor Gray
