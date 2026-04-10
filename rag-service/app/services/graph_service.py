import logging
import uuid
from typing import Any

from neo4j.exceptions import Neo4jError, ServiceUnavailable

from app.core.config import Settings
from app.core.neo4j_client import get_neo4j_driver, neo4j_session_config
from app.schemas.graph import (
    ALLOWED_ENTITY_KINDS,
    ALLOWED_PREDICATES,
    AgriEntityPayload,
    GraphEdgeVO,
    GraphNodeVO,
    GraphStats,
    GraphVisualization,
    TripleBatchRequest,
    TriplePayload,
)

logger = logging.getLogger(__name__)

_CONSTRAINT_CYPHER = """
CREATE CONSTRAINT agri_entity_id_unique IF NOT EXISTS
FOR (e:AgriEntity) REQUIRE e.id IS UNIQUE
"""


class GraphService:
    _schema_ready: bool = False

    def _require_driver(self):
        driver = get_neo4j_driver()
        if driver is None:
            raise RuntimeError("Neo4j 未启用或未配置密码，请检查 NEO4J_* 环境变量")
        return driver

    def ensure_schema(self) -> None:
        if GraphService._schema_ready:
            return
        driver = self._require_driver()
        kw = neo4j_session_config()
        with driver.session(**kw) as session:
            session.run(_CONSTRAINT_CYPHER)
        GraphService._schema_ready = True
        logger.info("Neo4j 约束 AgriEntity.id 已就绪")

    def health(self) -> dict[str, Any]:
        driver = get_neo4j_driver()
        if driver is None:
            return {"ok": False, "detail": "neo4j disabled or missing password"}
        kw = neo4j_session_config()
        try:
            with driver.session(**kw) as session:
                session.run("RETURN 1 AS ok").single()
            return {"ok": True}
        except (ServiceUnavailable, Neo4jError) as exc:
            logger.warning("Neo4j health check failed: %s", exc)
            return {"ok": False, "detail": str(exc)}

    def stats(self) -> GraphStats:
        driver = get_neo4j_driver()
        if driver is None:
            return GraphStats(entity_count=0, relation_count=0, neo4j_connected=False)
        kw = neo4j_session_config()
        try:
            with driver.session(**kw) as session:
                ent = session.run(
                    "MATCH (e:AgriEntity) RETURN count(e) AS c"
                ).single()
                rel = session.run(
                    "MATCH ()-[r:RELATES]->() RETURN count(r) AS c"
                ).single()
            return GraphStats(
                entity_count=int(ent["c"]) if ent else 0,
                relation_count=int(rel["c"]) if rel else 0,
                neo4j_connected=True,
            )
        except (ServiceUnavailable, Neo4jError) as exc:
            logger.warning("Neo4j stats failed: %s", exc)
            return GraphStats(entity_count=0, relation_count=0, neo4j_connected=False)

    def upsert_triples(self, req: TripleBatchRequest) -> dict[str, int]:
        self.ensure_schema()
        for t in req.triples:
            if t.predicate not in ALLOWED_PREDICATES:
                raise ValueError(f"不支持的关系谓词: {t.predicate}，允许: {sorted(ALLOWED_PREDICATES)}")
            if t.head.kind not in ALLOWED_ENTITY_KINDS or t.tail.kind not in ALLOWED_ENTITY_KINDS:
                raise ValueError("实体 kind 不在允许列表内")

        driver = self._require_driver()
        kw = neo4j_session_config()
        merged = 0
        with driver.session(**kw) as session:
            for t in req.triples:
                session.run(
                    """
                    MERGE (h:AgriEntity {id: $hid})
                    SET h.name = $hname, h.kind = $hkind
                    MERGE (tail:AgriEntity {id: $tid})
                    SET tail.name = $tname, tail.kind = $tkind
                    MERGE (h)-[r:RELATES {predicate: $pred}]->(tail)
                    SET r.source_ref = $sref, r.confidence = $conf
                    """,
                    hid=t.head.id,
                    hname=t.head.name,
                    hkind=t.head.kind,
                    tid=t.tail.id,
                    tname=t.tail.name,
                    tkind=t.tail.kind,
                    pred=t.predicate,
                    sref=t.source_ref,
                    conf=t.confidence,
                )
                merged += 1
        return {"upserted": merged}

    def search_entities(self, q: str, limit: int = 20) -> list[GraphNodeVO]:
        if not q.strip():
            return []
        self.ensure_schema()
        driver = self._require_driver()
        kw = neo4j_session_config()
        pattern = q.strip()
        with driver.session(**kw) as session:
            rows = session.run(
                """
                MATCH (e:AgriEntity)
                WHERE toLower(e.name) CONTAINS toLower($q) OR e.id CONTAINS $q
                RETURN e.id AS id, e.name AS name, e.kind AS kind
                LIMIT $lim
                """,
                q=pattern,
                lim=limit,
            )
            return [
                GraphNodeVO(id=r["id"], name=r["name"], kind=r.get("kind") or "concept")
                for r in rows
            ]

    def subgraph(self, center_id: str, depth: int = 2, path_limit: int | None = None) -> GraphVisualization:
        self.ensure_schema()
        driver = self._require_driver()
        depth = min(max(depth, 1), 3)
        lim = path_limit or Settings().neo4j_max_subgraph_paths
        kw = neo4j_session_config()

        depth_literal = depth
        cypher_paths = f"""
        MATCH (center:AgriEntity {{id: $cid}})
        MATCH p = (center)-[:RELATES*1..{depth_literal}]-(n)
        WITH p LIMIT $plim
        UNWIND relationships(p) AS rel
        WITH collect(DISTINCT rel) AS rels
        UNWIND rels AS r
        WITH rels,
             collect(DISTINCT startNode(r)) + collect(DISTINCT endNode(r)) AS ends
        UNWIND ends AS node
        WITH rels, collect(DISTINCT node) AS nodes
        RETURN nodes, rels
        """

        with driver.session(**kw) as session:
            center_row = session.run(
                "MATCH (c:AgriEntity {id: $cid}) RETURN c LIMIT 1",
                cid=center_id,
            ).single()
            if not center_row:
                return GraphVisualization(
                    center_id=center_id, depth=depth, nodes=[], edges=[]
                )
            center_n = center_row["c"]
            record = session.run(cypher_paths, cid=center_id, plim=lim).single()

        if not record or not record["rels"]:
            return GraphVisualization(
                center_id=center_id,
                depth=depth,
                nodes=[
                    GraphNodeVO(
                        id=center_n["id"],
                        name=center_n.get("name") or center_n["id"],
                        kind=center_n.get("kind") or "concept",
                    )
                ],
                edges=[],
            )

        raw_nodes = record["nodes"]
        raw_rels = record["rels"]
        nodes: dict[str, GraphNodeVO] = {}
        for n in raw_nodes:
            if n is None:
                continue
            eid = n.get("id")
            if not eid:
                continue
            nodes[eid] = GraphNodeVO(
                id=eid,
                name=n.get("name") or eid,
                kind=n.get("kind") or "concept",
            )

        edges: list[GraphEdgeVO] = []
        seen_rel: set[str] = set()
        for r in raw_rels:
            if r is None:
                continue
            a = r.start_node.get("id")
            b = r.end_node.get("id")
            if not a or not b:
                continue
            pred = r.get("predicate") or "关联"
            key = f"{a}|{pred}|{b}"
            if key in seen_rel:
                continue
            seen_rel.add(key)
            rid = str(getattr(r, "element_id", None) or uuid.uuid4())
            edges.append(
                GraphEdgeVO(
                    id=rid,
                    source=a,
                    target=b,
                    predicate=pred,
                    source_ref=r.get("source_ref"),
                    confidence=r.get("confidence"),
                )
            )

        return GraphVisualization(
            center_id=center_id,
            depth=depth,
            nodes=list(nodes.values()),
            edges=edges,
        )

    def seed_demo(self) -> dict[str, int]:
        demo = TripleBatchRequest(
            triples=[
                TriplePayload(
                    head=AgriEntityPayload(
                        id="stress_soil_moisture_low",
                        name="土壤湿度偏低",
                        kind="stress",
                    ),
                    tail=AgriEntityPayload(
                        id="risk_water_stress",
                        name="作物水分胁迫",
                        kind="concept",
                    ),
                    predicate="可能导致",
                    source_ref="农业知识库-墒情与生理",
                ),
                TriplePayload(
                    head=AgriEntityPayload(
                        id="crop_strawberry",
                        name="草莓",
                        kind="crop",
                    ),
                    tail=AgriEntityPayload(
                        id="stress_soil_moisture_low",
                        name="土壤湿度偏低",
                        kind="stress",
                    ),
                    predicate="易感",
                    source_ref="设施草莓栽培要点",
                ),
                TriplePayload(
                    head=AgriEntityPayload(
                        id="practice_irrigation",
                        name="灌溉补水",
                        kind="practice",
                    ),
                    tail=AgriEntityPayload(
                        id="stress_soil_moisture_low",
                        name="土壤湿度偏低",
                        kind="stress",
                    ),
                    predicate="缓解",
                    source_ref="SOP-土壤湿度处置",
                ),
                TriplePayload(
                    head=AgriEntityPayload(
                        id="disease_gray_mold",
                        name="灰霉病",
                        kind="disease",
                    ),
                    tail=AgriEntityPayload(
                        id="crop_strawberry",
                        name="草莓",
                        kind="crop",
                    ),
                    predicate="发生于",
                    source_ref="常见病害图谱",
                ),
                TriplePayload(
                    head=AgriEntityPayload(
                        id="env_high_humidity",
                        name="高湿环境",
                        kind="environment",
                    ),
                    tail=AgriEntityPayload(
                        id="disease_gray_mold",
                        name="灰霉病",
                        kind="disease",
                    ),
                    predicate="可能导致",
                    source_ref="病害发生条件",
                ),
            ]
        )
        return self.upsert_triples(demo)


graph_service = GraphService()
