# Docker 一键启动脚本 - 智慧农业管理系统
# 适用于 Windows PowerShell

$ErrorActionPreference = "Stop"
$root = if ($PSScriptRoot) { $PSScriptRoot } else { (Get-Location).Path }
$envFile = Join-Path $root ".env"
$stackEnv = Join-Path $root "stack.env"
$stackEnvExample = Join-Path $root "docker\stack.env.example"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "智慧农业管理系统 Docker 一键启动" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# 检查 Docker 是否运行
Write-Host "`n[1/5] 检查 Docker 状态..." -ForegroundColor Yellow
$dockerStatus = docker ps 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Host "错误: Docker 未运行或权限不足" -ForegroundColor Red
    Write-Host "请启动 Docker Desktop 后重试" -ForegroundColor Red
    exit 1
}
Write-Host "✓ Docker 运行正常" -ForegroundColor Green

# 准备 .env（优先使用仓库根目录 .env，其次 stack.env，最后示例文件）
Write-Host "`n[2/5] 准备配置文件..." -ForegroundColor Yellow
if (-not (Test-Path $envFile)) {
    if (Test-Path $stackEnv) {
        Copy-Item $stackEnv $envFile
        Write-Host "  已复制 stack.env -> .env" -ForegroundColor Cyan
    } elseif (Test-Path $stackEnvExample) {
        Copy-Item $stackEnvExample $envFile
        Write-Host "  已复制 docker/stack.env.example -> .env" -ForegroundColor Cyan
    } else {
        Write-Host "错误: 未找到 .env / stack.env / stack.env.example" -ForegroundColor Red
        exit 1
    }
}

# 自动补齐必填密钥（避免使用仓库内置的开发默认值）
function New-RandomSecret([int]$byteLength) {
    $bytes = New-Object byte[] $byteLength
    [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    return [Convert]::ToBase64String($bytes)
}

function Ensure-EnvSecret([string]$path, [string]$key, [int]$minLength) {
    $content = Get-Content -Path $path -Raw
    $pattern = "(?m)^" + [regex]::Escape($key) + "=(.*)$"
    $match = [regex]::Match($content, $pattern)
    $current = if ($match.Success) { $match.Groups[1].Value.Trim() } else { "" }
    $isPlaceholder = $current -eq "" -or $current.StartsWith("请替换") -or $current.StartsWith("changeme")
    if (-not $isPlaceholder -and $current.Length -ge $minLength) {
        return
    }
    $secret = New-RandomSecret 32
    if ($match.Success) {
        $content = [regex]::Replace($content, $pattern, { param($m) "$key=$secret" })
    } else {
        $content = $content.TrimEnd() + "`n$key=$secret`n"
    }
    Set-Content -Path $path -Value $content -NoNewline -Encoding utf8
    Write-Host "  已为 $key 生成随机密钥" -ForegroundColor Cyan
}

Ensure-EnvSecret $envFile "JWT_SECRET_KEY" 32
Ensure-EnvSecret $envFile "NEO4J_PASSWORD" 16
Write-Host "✓ 配置文件就绪: $envFile" -ForegroundColor Green

# 检查并拉取所需镜像
Write-Host "`n[3/5] 检查所需镜像..." -ForegroundColor Yellow
$requiredImages = @(
    "mysql:8.4",
    "neo4j:5.26-community",
    "qdrant/qdrant:v1.12.4",
    "maven:3.9-eclipse-temurin-17",
    "eclipse-temurin:17-jre-jammy",
    "python:3.12-slim",
    "node:20-alpine",
    "nginx:1.27-alpine"
)

$missingImages = @()
foreach ($img in $requiredImages) {
    $exists = docker images -q $img 2>$null
    if (-not $exists) {
        $missingImages += $img
        Write-Host "  需要拉取: $img" -ForegroundColor Cyan
    } else {
        Write-Host "  ✓ $img" -ForegroundColor Green
    }
}

if ($missingImages.Count -gt 0) {
    Write-Host "`n  拉取缺失镜像..." -ForegroundColor Yellow
    foreach ($img in $missingImages) {
        Write-Host "  正在拉取 $img ..." -ForegroundColor Cyan
        docker pull $img
    }
}

# 启动服务
Write-Host "`n[4/5] 启动 Docker Compose 服务..." -ForegroundColor Yellow
Set-Location $root

Write-Host "  停止旧容器..." -ForegroundColor Cyan
docker compose down 2>$null

Write-Host "  启动所有服务（首次构建可能需要几分钟）..." -ForegroundColor Cyan
docker compose up -d --build

# 检查服务状态
Write-Host "`n[5/5] 等待服务就绪..." -ForegroundColor Yellow
$services = @(
    @{Name="RAG Service"; Url="http://localhost:9001/health"},
    @{Name="Web UI"; Url="http://localhost:8088"}
)

for ($i = 0; $i -lt 30; $i++) {
    $allReady = $true
    foreach ($svc in $services) {
        try {
            $response = Invoke-WebRequest -Uri $svc.Url -UseBasicParsing -TimeoutSec 3 -ErrorAction Stop
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 400) {
                continue
            }
        } catch {
            # 未就绪，继续等待
        }
        $allReady = $false
    }
    if ($allReady) { break }
    Start-Sleep -Seconds 5
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "启动完成！" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "访问地址:" -ForegroundColor White
Write-Host "  前端:    http://localhost:8088" -ForegroundColor Cyan
Write-Host "  Swagger: http://localhost:8090/api/swagger-ui.html" -ForegroundColor Cyan
Write-Host "  Qdrant:  http://localhost:6333/dashboard" -ForegroundColor Cyan
Write-Host "  Neo4j:   http://localhost:7474" -ForegroundColor Cyan
Write-Host ""
Write-Host "初始账号: admin / 123456（首次登录后请修改）" -ForegroundColor Yellow
Write-Host ""
Write-Host "查看日志: docker compose logs -f" -ForegroundColor Gray
Write-Host "停止服务: docker compose down" -ForegroundColor Gray
Write-Host ""
Write-Host "提示: 若曾用旧密码初始化过 Neo4j 数据卷，修改 NEO4J_PASSWORD 后需执行 docker compose down -v 重建数据卷。" -ForegroundColor Yellow
Write-Host ""
