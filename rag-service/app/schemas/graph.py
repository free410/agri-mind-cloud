from typing import Any, Literal

from pydantic import BaseModel, Field

# 关系统一为 RELATES，predicate 表达语义（便于 Cypher 与前端展示，无需 APOC 动态类型）
ALLOWED_PREDICATES: frozenset[str] = frozenset(
    {
        "导致",
        "可能导致",
        "可能引发",
        "易感",
        "发生于",
        "缓解",
        "防治",
        "关联",
        "需要",
        "影响",
        "适用于",
        "属于",
    }
)

ALLOWED_ENTITY_KINDS: frozenset[str] = frozenset(
    {
        "crop",
        "disease",
        "pest",
        "stress",
        "practice",
        "environment",
        "input",
        "concept",
        "other",
    }
)


class AgriEntityPayload(BaseModel):
    id: str = Field(min_length=1, max_length=128, description="业务唯一键，如 crop_strawberry")
    name: str = Field(min_length=1, max_length=200)
    kind: Literal[
        "crop",
        "disease",
        "pest",
        "stress",
        "practice",
        "environment",
        "input",
        "concept",
        "other",
    ] = "concept"


class TriplePayload(BaseModel):
    head: AgriEntityPayload
    tail: AgriEntityPayload
    predicate: str = Field(min_length=1, max_length=64)
    source_ref: str | None = Field(default=None, max_length=500, description="文档/网页溯源说明")
    confidence: float | None = Field(default=None, ge=0.0, le=1.0)


class TripleBatchRequest(BaseModel):
    triples: list[TriplePayload] = Field(min_length=1, max_length=500)


class GraphNodeVO(BaseModel):
    id: str
    name: str
    kind: str
    extra: dict[str, Any] = Field(default_factory=dict)


class GraphEdgeVO(BaseModel):
    id: str
    source: str
    target: str
    predicate: str
    source_ref: str | None = None
    confidence: float | None = None


class GraphVisualization(BaseModel):
    center_id: str | None = None
    depth: int = 2
    nodes: list[GraphNodeVO]
    edges: list[GraphEdgeVO]


class GraphStats(BaseModel):
    entity_count: int
    relation_count: int
    neo4j_connected: bool
