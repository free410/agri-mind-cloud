# 删除 Neo4j 数据卷并重启容器，使 NEO4J_AUTH / .env 中的密码重新生效，并清除「认证尝试过多」。
# 在仓库根目录执行：powershell -ExecutionPolicy Bypass -File docker/reset-neo4j.ps1
$ErrorActionPreference = "Stop"
$repoRoot = Split-Path $PSScriptRoot -Parent
Set-Location $repoRoot

Write-Host "[1/3] 停止并移除 Neo4j 容器（含按名称强删，避免旧 compose 项目占用卷）..."
docker compose stop smartagri-neo4j 2>$null
docker compose rm -f smartagri-neo4j 2>$null
docker rm -f smartagri-neo4j 2>$null

Write-Host "[2/3] 删除名称包含 smartagri_neo4j_data 的卷..."
docker volume ls -q | ForEach-Object {
    if ($_ -match "smartagri_neo4j_data") {
        docker volume rm -f $_
        Write-Host "  已删除: $_"
    }
}

Write-Host "[3/3] 启动 Neo4j（密码以 .env 或 compose 默认为准，见 docker/stack.env.example）..."
docker compose up -d smartagri-neo4j
Write-Host "完成。请确认 smart-agriculture-server 中 neo4j 密码与 NEO4J_PASSWORD 一致后重启 Spring Boot。"
