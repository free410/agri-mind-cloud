import logging
from collections.abc import Generator
from typing import Annotated

from fastapi import APIRouter, Depends, Header, HTTPException, Query

from app.core.config import Settings
from app.core.neo4j_env import (
    reset_gateway_neo4j_password,
    resolve_neo4j_password,
    set_gateway_neo4j_password_token,
)
from app.core.response import success
from app.schemas.graph import TripleBatchRequest
from app.services.graph_service import graph_service

logger = logging.getLogger(__name__)


def _neo4j_gateway_ctx(
    x_rag_neo4j_password: Annotated[str | None, Header(alias="X-RAG-Neo4j-Password")] = None,
) -> Generator[None, None, None]:
    """Spring Boot 传入 Neo4j 密码，避免仅依赖 Python 进程 .env（与本机环境变量冲突时仍可用）。"""
    token = set_gateway_neo4j_password_token(x_rag_neo4j_password)
    try:
        yield
    finally:
        reset_gateway_neo4j_password(token)


router = APIRouter(prefix="/graph", dependencies=[Depends(_neo4j_gateway_ctx)])


def _check_enabled():
    cfg = Settings()
    if not cfg.neo4j_enabled:
        raise HTTPException(status_code=503, detail="Neo4j 功能已关闭（NEO4J_ENABLED=false）")
    pwd = resolve_neo4j_password() or (cfg.neo4j_password or "").strip()
    if not pwd:
        raise HTTPException(
            status_code=503,
            detail="未配置 NEO4J_PASSWORD：请在 rag-service/.env 中设置，或与 docker-compose NEO4J_AUTH 密码一致",
        )


@router.get("/health")
def graph_health():
    _check_enabled()
    return success(data=graph_service.health())


@router.get("/stats")
def graph_stats():
    _check_enabled()
    return success(data=graph_service.stats().model_dump())


@router.get("/search")
def graph_search(
    q: str = Query("", min_length=0, max_length=200),
    limit: int = Query(20, ge=1, le=100),
):
    _check_enabled()
    try:
        items = graph_service.search_entities(q, limit=limit)
        return success(data=[x.model_dump() for x in items])
    except RuntimeError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc


@router.get("/subgraph")
def graph_subgraph(
    center_id: str = Query(..., min_length=1, max_length=128),
    depth: int = Query(2, ge=1, le=3),
    path_limit: int | None = Query(default=None, description="限制展开路径条数，默认取环境变量 NEO4J_MAX_SUBGRAPH_PATHS"),
):
    _check_enabled()
    try:
        viz = graph_service.subgraph(center_id, depth=depth, path_limit=path_limit)
        return success(data=viz.model_dump())
    except RuntimeError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc


@router.post("/triples")
def graph_upsert_triples(body: TripleBatchRequest):
    _check_enabled()
    try:
        graph_service.ensure_schema()
        result = graph_service.upsert_triples(body)
        return success(data=result)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except RuntimeError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc


@router.post("/seed-demo")
def graph_seed_demo():
    _check_enabled()
    try:
        graph_service.ensure_schema()
        result = graph_service.seed_demo()
        return success(data=result, message="已写入演示三元组（可重复调用，边属性会更新）")
    except RuntimeError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc
