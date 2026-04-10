# Git 与敏感信息（勿提交清单）

## 已被 .gitignore 忽略（勿强行 `git add -f`）

| 类型 | 说明 |
|------|------|
| `**/.env`、`rag-service/.env` | 含 `OPENAI_API_KEY`、Qdrant 等 |
| `.env.docker.neo4j` | Neo4j Docker 口令 |
| `application-local.yml` | 本地 MySQL 密码、JWT 密钥、Neo4j 密码等 |
| `node_modules/`、`dist/`、`target/` | 构建产物与依赖 |
| Python `__pycache__`、`.venv/` | 本地解释器环境 |

## 仓库中应只保留示例文件

| 文件 | 用途 |
|------|------|
| `rag-service/.env.example` | 复制为 `.env` 后填写密钥 |
| `.env.docker.neo4j.example` | 复制为 `.env.docker.neo4j` 后填写 `NEO4J_AUTH` |
| `application-local.example.yml` | 复制为 `application-local.yml` 并启用 profile `local` |

## 本地启动 Spring 前建议设置

- `MYSQL_PASSWORD`：MySQL 密码（与 `application.yml` 中库连接一致）  
- `JWT_SECRET_KEY`：长度建议 ≥ 32 字符；若留空，当前代码会走内置开发用回退逻辑，**生产环境务必配置强密钥**  
- `SMART_AGRI_NEO4J_PASSWORD`：与 Docker `NEO4J_AUTH` 中密码部分一致（若使用图谱）  
- `SMART_AGRI_AI_API_KEY`：百炼等兼容接口 Key（若启用真实模型）  

## 代码中的开发用回退

`JwtUtils` 在 `JWT_SECRET_KEY` 未配置或长度不足时，会使用源码内的 Base64 回退密钥，**仅便于本地启动**；生产环境必须配置足够长的 `JWT_SECRET_KEY`，并在上线前审计该行为是否符合安全策略。

## 若误提交密钥

应立即在对应平台**轮换密钥**，并用 `git filter-repo` 或 BFG 清理历史（必要时配合 GitHub 支持）。
