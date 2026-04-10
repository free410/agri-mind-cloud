"""
Neo4j 连接参数解析：不依赖 pydantic-settings 对 .env 的合并顺序。

Windows 上常见故障：环境变量里存在空的 NEO4J_PASSWORD，导致始终读不到 rag-service/.env；
或 .env 带 BOM、工作目录不一致等。此处对 rag-service/.env 做显式解析并写回 os.environ。
"""

from __future__ import annotations

import os
from contextvars import ContextVar, Token
from pathlib import Path

_RAG_SERVICE_ROOT = Path(__file__).resolve().parents[2]
_DOTENV_PATH = _RAG_SERVICE_ROOT / ".env"

# Spring Boot 网关通过请求头 X-RAG-Neo4j-Password 传入密码（仅服务端互通，勿对公网暴露）
_gateway_neo4j_password: ContextVar[str | None] = ContextVar("gateway_neo4j_password", default=None)


def set_gateway_neo4j_password_token(value: str | None) -> Token | None:
    """返回 reset 用 token；未设置则返回 None。"""
    if value is None or not str(value).strip():
        return None
    return _gateway_neo4j_password.set(str(value).strip())


def reset_gateway_neo4j_password(token: Token | None) -> None:
    if token is not None:
        _gateway_neo4j_password.reset(token)


def _strip_quotes(raw: str) -> str:
    s = raw.strip()
    if len(s) >= 2 and ((s[0] == s[-1] == '"') or (s[0] == s[-1] == "'")):
        return s[1:-1].strip()
    return s


def resolve_neo4j_password() -> str:
    """
    返回非空密码，或 ""。
    优先级：网关请求头（ContextVar）> 环境变量 > rag-service/.env 文件解析。
    """
    gw = _gateway_neo4j_password.get()
    if gw is not None and str(gw).strip():
        return str(gw).strip()

    env_val = os.environ.get("NEO4J_PASSWORD")
    if env_val is not None and str(env_val).strip():
        return str(env_val).strip()

    if not _DOTENV_PATH.is_file():
        return ""

    try:
        text = _DOTENV_PATH.read_text(encoding="utf-8-sig")
    except OSError:
        return ""

    for raw in text.splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        if "=" not in line:
            continue
        key, _, rest = line.partition("=")
        if key.strip().upper() != "NEO4J_PASSWORD":
            continue
        val = rest.split("#", 1)[0].strip()
        val = _strip_quotes(val)
        if val:
            os.environ["NEO4J_PASSWORD"] = val
            return val
    return ""
