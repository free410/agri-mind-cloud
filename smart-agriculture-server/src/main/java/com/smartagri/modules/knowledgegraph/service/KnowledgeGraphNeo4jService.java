package com.smartagri.modules.knowledgegraph.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.smartagri.common.exception.BusinessException;
import com.smartagri.common.properties.KnowledgeGraphProperties;
import com.smartagri.common.result.ResultCode;
import com.smartagri.modules.knowledgegraph.model.AgriGraphSeedTriple;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.TransactionContext;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Relationship;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 直连 Neo4j，不经过 FastAPI，避免本机 9001 上残留旧 Python 进程导致图谱不可用。
 */
@Slf4j
@Service
public class KnowledgeGraphNeo4jService {

    private static final int MAX_IMPORT_TRIPLES = 50_000;

    private static final String CONSTRAINT_CYPHER = """
            CREATE CONSTRAINT agri_entity_id_unique IF NOT EXISTS
            FOR (e:AgriEntity) REQUIRE e.id IS UNIQUE
            """;

    private static final String SEED_RESOURCE = "/knowledge-graph/agri-graph-seed.json";

    private static final String ENTITY_ENRICH_RESOURCE = "/knowledge-graph/agri-graph-entities.json";

    private static final List<String> STOP_WORDS = List.of(
            "的", "了", "是", "在", "和", "与", "或", "及", "等", "对", "有", "为",
            "我", "你", "他", "她", "它", "们", "这", "那", "个", "一", "上", "下",
            "中", "来", "去", "到", "从", "被", "把", "让", "用", "以", "但", "而",
            "吗", "呢", "吧", "啊", "哦", "呀", "哪", "什么", "怎么", "如何",
            "可以", "能够", "应该", "必须", "需要", "要", "不", "没", "请", "您",
            "做", "进行", "发生", "通过", "根据", "按照", "关于", "对于", "由于",
            "还是", "或者", "以及", "并", "且", "只", "仅", "都", "也", "已", "就",
            "会", "能", "将", "给", "向", "比", "如", "当", "于", "其"
    );

    private static final Pattern CN_WORD_PATTERN = Pattern.compile(
            "[\u4e00-\u9fff]{2,}"
                    + "|[a-zA-Z0-9]{3,}"
    );

    private static final List<AgriGraphSeedTriple> FALLBACK_SEED = List.of(
            new AgriGraphSeedTriple("stress_soil_moisture_low", "土壤湿度偏低", "stress",
                    "risk_water_stress", "作物水分胁迫", "concept",
                    "可能导致", "内嵌兜底-墒情与生理", null),
            new AgriGraphSeedTriple("crop_strawberry", "草莓", "crop",
                    "stress_soil_moisture_low", "土壤湿度偏低", "stress",
                    "易感", "内嵌兜底", null),
            new AgriGraphSeedTriple("prac_irrigation", "灌溉补水", "practice",
                    "stress_soil_moisture_low", "土壤湿度偏低", "stress",
                    "缓解", "内嵌兜底", null)
    );

    private final KnowledgeGraphProperties properties;
    private final ObjectMapper objectMapper;
    private volatile Driver driver;
    private volatile boolean schemaReady;
    private volatile List<AgriGraphSeedTriple> seedTriplesCache;

    public KnowledgeGraphNeo4jService(KnowledgeGraphProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @PreDestroy
    public void closeDriver() {
        if (driver != null) {
            driver.close();
            driver = null;
        }
    }

    private void requirePassword() {
        if (StringUtils.isBlank(properties.getNeo4jPassword())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR,
                    "未配置 Neo4j 密码：请在 application.yml 中设置 smart-agri.knowledge-graph.neo4j-password，"
                            + "或环境变量 SMART_AGRI_NEO4J_PASSWORD（须与 docker-compose NEO4J_AUTH 一致）");
        }
    }

    private Session openSession() {
        requirePassword();
        if (driver == null) {
            synchronized (this) {
                if (driver == null) {
                    driver = GraphDatabase.driver(
                            properties.getNeo4jUri(),
                            AuthTokens.basic(properties.getNeo4jUser(), properties.getNeo4jPassword())
                    );
                }
            }
        }
        SessionConfig.Builder builder = SessionConfig.builder();
        if (StringUtils.isNotBlank(properties.getNeo4jDatabase())) {
            builder.withDatabase(properties.getNeo4jDatabase());
        }
        return driver.session(builder.build());
    }

    private void ensureSchema(Session session) {
        if (!schemaReady) {
            synchronized (this) {
                if (!schemaReady) {
                    session.run(CONSTRAINT_CYPHER).consume();
                    schemaReady = true;
                }
            }
        }
    }

    public JsonNode health() {
        ObjectNode out = objectMapper.createObjectNode();
        if (StringUtils.isBlank(properties.getNeo4jPassword())) {
            out.put("ok", false);
            out.put("detail", "neo4j password not configured in Spring");
            return out;
        }
        try (Session session = openSession()) {
            session.run("RETURN 1 AS ok").consume();
            out.put("ok", true);
            return out;
        } catch (Exception ex) {
            out.put("ok", false);
            out.put("detail", ex.getMessage() == null ? "unknown" : ex.getMessage());
            return out;
        }
    }

    public JsonNode stats() {
        ObjectNode out = objectMapper.createObjectNode();
        if (StringUtils.isBlank(properties.getNeo4jPassword())) {
            out.put("entity_count", 0);
            out.put("relation_count", 0);
            out.put("neo4j_connected", false);
            out.set("kind_distribution", objectMapper.createArrayNode());
            return out;
        }
        try (Session session = openSession()) {
            ensureSchema(session);
            Record ent = session.run("MATCH (e:AgriEntity) RETURN count(e) AS c").single();
            Record rel = session.run("MATCH ()-[r:RELATES]->() RETURN count(r) AS c").single();
            out.put("entity_count", ent != null ? ent.get("c").asInt() : 0);
            out.put("relation_count", rel != null ? rel.get("c").asInt() : 0);
            out.put("neo4j_connected", true);
            ArrayNode dist = objectMapper.createArrayNode();
            var kd = session.run(
                    """
                            MATCH (e:AgriEntity)
                            RETURN coalesce(e.kind, 'other') AS k, count(*) AS c
                            ORDER BY c DESC
                            """
            );
            while (kd.hasNext()) {
                Record row = kd.next();
                ObjectNode item = objectMapper.createObjectNode();
                item.put("kind", row.get("k").asString());
                item.put("count", row.get("c").asInt());
                dist.add(item);
            }
            out.set("kind_distribution", dist);
            return out;
        } catch (Exception ex) {
            out.put("entity_count", 0);
            out.put("relation_count", 0);
            out.put("neo4j_connected", false);
            out.set("kind_distribution", objectMapper.createArrayNode());
            return out;
        }
    }

    public JsonNode search(String q, int limit) {
        requirePassword();
        ArrayNode arr = objectMapper.createArrayNode();
        if (StringUtils.isBlank(q)) {
            return arr;
        }
        int lim = Math.min(Math.max(limit, 1), 100);
        try (Session session = openSession()) {
            ensureSchema(session);

            // 中文分词 + 多关键词匹配：按命中数降序
            List<String> keywords = extractKeywords(q.trim());
            if (keywords.isEmpty()) {
                keywords = List.of(q.trim());
            }
            String keywordsParam = String.join(" ", keywords);

            var result = session.run(
                    """
                            MATCH (e:AgriEntity)
                            WHERE any(k IN split($keywords, ' ')
                                       WHERE toLower(e.name) CONTAINS toLower(k)
                                          OR e.id CONTAINS k
                            )
                            WITH e,
                                 size([k IN split($keywords, ' ')
                                       WHERE toLower(e.name) CONTAINS toLower(k)] +
                                      [k IN split($keywords, ' ')
                                       WHERE e.id CONTAINS k]) AS hitCount
                            RETURN e.id AS id, e.name AS name, e.kind AS kind
                            ORDER BY hitCount DESC
                            LIMIT $lim
                            """,
                    Values.parameters("keywords", keywordsParam, "lim", lim)
            );
            while (result.hasNext()) {
                Record row = result.next();
                ObjectNode n = objectMapper.createObjectNode();
                n.put("id", row.get("id").asString());
                n.put("name", row.get("name").isNull() ? row.get("id").asString() : row.get("name").asString());
                var k = row.get("kind");
                n.put("kind", k.isNull() ? "concept" : k.asString());
                n.set("extra", objectMapper.createObjectNode());
                arr.add(n);
            }
            return arr;
        }
    }

    private List<String> extractKeywords(String text) {
        List<String> keywords = new ArrayList<>();
        int n = text.length();
        for (int i = 0; i < n; i++) {
            // 跳过非汉字
            if (!isChinese(text.charAt(i))) continue;
            // 找连续汉字片段
            int j = i;
            StringBuilder segment = new StringBuilder();
            while (j < n && isChinese(text.charAt(j))) {
                segment.append(text.charAt(j));
                j++;
            }
            String seg = segment.toString();
            // 片段内：滑动窗口提取 2~4 字词元
            if (seg.length() <= 8) {
                for (int start = 0; start < seg.length(); start++) {
                    for (int len = 2; len <= 4; len++) {
                        if (start + len <= seg.length()) {
                            String kw = seg.substring(start, start + len);
                            if (!STOP_WORDS.contains(kw)) {
                                keywords.add(kw);
                            }
                        }
                    }
                }
            } else {
                // 片段过长时用滑动窗口避免组合爆炸
                for (int start = 0; start + 2 <= seg.length(); start++) {
                    for (int len = 2; len <= 4 && start + len <= seg.length(); len++) {
                        String kw = seg.substring(start, start + len);
                        if (!STOP_WORDS.contains(kw)) {
                            keywords.add(kw);
                        }
                    }
                }
            }
            i = j - 1;
        }
        return keywords;
    }

    private static boolean isChinese(char c) {
        return c >= 0x4e00 && c <= 0x9fff;
    }

    public JsonNode subgraph(String centerId, int depth) {
        requirePassword();
        int d = Math.min(Math.max(depth, 1), 3);
        int plim = properties.getNeo4jMaxSubgraphPaths() == null ? 400 : properties.getNeo4jMaxSubgraphPaths();
        try (Session session = openSession()) {
            ensureSchema(session);
            var centerResult = session.run(
                    "MATCH (c:AgriEntity {id: $cid}) RETURN c LIMIT 1",
                    Values.parameters("cid", centerId)
            );
            if (!centerResult.hasNext()) {
                return emptySubgraph(centerId, d);
            }
            Node centerNode = centerResult.single().get("c").asNode();
            String pathCypher = String.format(Locale.ROOT, """
                    MATCH (center:AgriEntity {id: $cid})
                    MATCH p = (center)-[:RELATES*1..%d]-(n)
                    WITH p LIMIT $plim
                    UNWIND relationships(p) AS r
                    RETURN DISTINCT elementId(r) AS id, startNode(r).id AS source, endNode(r).id AS target,
                    coalesce(r.predicate,'关联') AS predicate, r.source_ref AS source_ref, r.confidence AS confidence
                    """, d);
            var edgeResult = session.run(
                    pathCypher,
                    Values.parameters("cid", centerId, "plim", plim)
            );
            ArrayNode edges = objectMapper.createArrayNode();
            LinkedHashSet<String> nodeIds = new LinkedHashSet<>();
            nodeIds.add(centerNode.get("id").asString());
            while (edgeResult.hasNext()) {
                Record row = edgeResult.next();
                ObjectNode e = objectMapper.createObjectNode();
                e.put("id", row.get("id").asString());
                e.put("source", row.get("source").asString());
                e.put("target", row.get("target").asString());
                e.put("predicate", row.get("predicate").asString());
                var sr = row.get("source_ref");
                if (sr.isNull()) {
                    e.putNull("source_ref");
                } else {
                    e.put("source_ref", sr.asString());
                }
                var cf = row.get("confidence");
                if (cf.isNull()) {
                    e.putNull("confidence");
                } else {
                    e.put("confidence", cf.asDouble());
                }
                edges.add(e);
                nodeIds.add(row.get("source").asString());
                nodeIds.add(row.get("target").asString());
            }
            if (edges.isEmpty()) {
                ArrayNode nodes = objectMapper.createArrayNode();
                nodes.add(nodeToJsonFull(centerNode));
                ObjectNode root = objectMapper.createObjectNode();
                root.put("center_id", centerId);
                root.put("depth", d);
                root.set("nodes", nodes);
                root.set("edges", objectMapper.createArrayNode());
                return root;
            }
            var nodeResult = session.run(
                    """
                            MATCH (n:AgriEntity) WHERE n.id IN $ids
                            RETURN n.id AS id, n.name AS name, n.kind AS kind, n.description AS description, n.aliases AS aliases
                            """,
                    Values.parameters("ids", new ArrayList<>(nodeIds))
            );
            ArrayNode nodes = objectMapper.createArrayNode();
            while (nodeResult.hasNext()) {
                nodes.add(subgraphNodeFromRecord(nodeResult.next()));
            }
            ObjectNode root = objectMapper.createObjectNode();
            root.put("center_id", centerId);
            root.put("depth", d);
            root.set("nodes", nodes);
            root.set("edges", edges);
            return root;
        }
    }

    private JsonNode emptySubgraph(String centerId, int depth) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("center_id", centerId);
        root.put("depth", depth);
        root.set("nodes", objectMapper.createArrayNode());
        root.set("edges", objectMapper.createArrayNode());
        return root;
    }

    private ObjectNode subgraphNodeFromRecord(Record row) {
        ObjectNode n = objectMapper.createObjectNode();
        n.put("id", row.get("id").asString());
        n.put("name", row.get("name").isNull() ? row.get("id").asString() : row.get("name").asString());
        var k = row.get("kind");
        n.put("kind", k.isNull() ? "concept" : k.asString());
        var desc = row.get("description");
        if (desc == null || desc.isNull()) {
            n.putNull("description");
        } else {
            n.put("description", desc.asString());
        }
        n.set("aliases", aliasesToJsonArray(row.get("aliases")));
        n.set("extra", objectMapper.createObjectNode());
        return n;
    }

    private ArrayNode aliasesToJsonArray(Value aliasesVal) {
        ArrayNode arr = objectMapper.createArrayNode();
        if (aliasesVal == null || aliasesVal.isNull()) {
            return arr;
        }
        for (Value v : aliasesVal.values()) {
            if (!v.isNull()) {
                arr.add(v.asString());
            }
        }
        return arr;
    }

    private ObjectNode nodeToJsonFull(Node n) {
        ObjectNode o = objectMapper.createObjectNode();
        o.put("id", n.get("id").asString());
        o.put("name", n.get("name").isNull() ? n.get("id").asString() : n.get("name").asString());
        var k = n.get("kind");
        o.put("kind", k.isNull() ? "concept" : k.asString());
        Value desc = n.get("description");
        if (desc == null || desc.isNull()) {
            o.putNull("description");
        } else {
            o.put("description", desc.asString());
        }
        o.set("aliases", aliasesToJsonArray(n.get("aliases")));
        o.set("extra", objectMapper.createObjectNode());
        return o;
    }

    /**
     * 实体详情：一度邻居与度，用于业务侧抽屉与关联分析。
     */
    public JsonNode entityDetail(String entityId) {
        requirePassword();
        ObjectNode notFound = objectMapper.createObjectNode();
        notFound.put("found", false);
        if (StringUtils.isBlank(entityId)) {
            return notFound;
        }
        String id = entityId.trim();
        try (Session session = openSession()) {
            ensureSchema(session);
            var r0 = session.run(
                    "MATCH (n:AgriEntity {id: $id}) RETURN n LIMIT 1",
                    Values.parameters("id", id)
            );
            if (!r0.hasNext()) {
                return notFound;
            }
            Node n = r0.single().get("n").asNode();
            ObjectNode root = nodeToJsonFull(n);
            root.put("found", true);
            var rout = session.run(
                    """
                            MATCH (n:AgriEntity {id:$id})-[r:RELATES]->(o:AgriEntity)
                            RETURN o.id AS id, o.name AS name, o.kind AS kind, coalesce(r.predicate,'关联') AS predicate
                            ORDER BY predicate LIMIT 48
                            """,
                    Values.parameters("id", id)
            );
            ArrayNode outArr = objectMapper.createArrayNode();
            while (rout.hasNext()) {
                outArr.add(neighborRowToJson(rout.next()));
            }
            root.set("relations_out", outArr);
            var rin = session.run(
                    """
                            MATCH (i:AgriEntity)-[r:RELATES]->(n:AgriEntity {id:$id})
                            RETURN i.id AS id, i.name AS name, i.kind AS kind, coalesce(r.predicate,'关联') AS predicate
                            ORDER BY predicate LIMIT 48
                            """,
                    Values.parameters("id", id)
            );
            ArrayNode inArr = objectMapper.createArrayNode();
            while (rin.hasNext()) {
                inArr.add(neighborRowToJson(rin.next()));
            }
            root.set("relations_in", inArr);
            Record deg = session.run(
                    """
                            MATCH (n:AgriEntity {id:$id})
                            RETURN COUNT {(n)-[:RELATES]->()} AS dout, COUNT {(n)<-[:RELATES]-()} AS din
                            """,
                    Values.parameters("id", id)
            ).single();
            root.put("degree_out", deg.get("dout").asInt());
            root.put("degree_in", deg.get("din").asInt());
            return root;
        }
    }

    private ObjectNode neighborRowToJson(Record row) {
        ObjectNode x = objectMapper.createObjectNode();
        x.put("id", row.get("id").asString());
        x.put("name", row.get("name").isNull() ? row.get("id").asString() : row.get("name").asString());
        var kk = row.get("kind");
        x.put("kind", kk.isNull() ? "concept" : kk.asString());
        x.put("predicate", row.get("predicate").asString());
        return x;
    }

    /**
     * 两实体间无向最短路径，用于链路解读与溯源展示。
     */
    public JsonNode shortestPath(String fromId, String toId, int maxHops) {
        requirePassword();
        ObjectNode empty = objectMapper.createObjectNode();
        empty.put("found", false);
        empty.set("nodes", objectMapper.createArrayNode());
        empty.set("edges", objectMapper.createArrayNode());
        if (StringUtils.isBlank(fromId) || StringUtils.isBlank(toId)) {
            return empty;
        }
        String a = fromId.trim();
        String b = toId.trim();
        int mh = Math.min(Math.max(maxHops, 1), 12);
        try (Session session = openSession()) {
            ensureSchema(session);
            if (a.equals(b)) {
                var one = session.run(
                        "MATCH (n:AgriEntity {id:$id}) RETURN n LIMIT 1",
                        Values.parameters("id", a)
                );
                if (!one.hasNext()) {
                    empty.put("from_id", a);
                    empty.put("to_id", b);
                    return empty;
                }
                ObjectNode ok = objectMapper.createObjectNode();
                ok.put("found", true);
                ok.put("from_id", a);
                ok.put("to_id", b);
                ok.put("hop_count", 0);
                ArrayNode na = objectMapper.createArrayNode();
                na.add(nodeToJsonFull(one.single().get("n").asNode()));
                ok.set("nodes", na);
                ok.set("edges", objectMapper.createArrayNode());
                return ok;
            }
            String cypher = String.format(Locale.ROOT, """
                    MATCH (x:AgriEntity {id:$from}), (y:AgriEntity {id:$to})
                    MATCH p = shortestPath((x)-[:RELATES*..%d]-(y))
                    RETURN p
                    LIMIT 1
                    """, mh);
            var res = session.run(cypher, Values.parameters("from", a, "to", b));
            if (!res.hasNext()) {
                empty.put("from_id", a);
                empty.put("to_id", b);
                return empty;
            }
            Path path = res.single().get("p").asPath();
            ArrayNode nodes = objectMapper.createArrayNode();
            ArrayNode edges = objectMapper.createArrayNode();
            LinkedHashSet<String> seen = new LinkedHashSet<>();
            for (Path.Segment seg : path) {
                Node sn = seg.start();
                Node en = seg.end();
                String sid = sn.get("id").asString();
                String eid = en.get("id").asString();
                if (seen.add(sid)) {
                    nodes.add(nodeToJsonFull(sn));
                }
                if (seen.add(eid)) {
                    nodes.add(nodeToJsonFull(en));
                }
                Relationship rel = seg.relationship();
                ObjectNode e = objectMapper.createObjectNode();
                e.put("id", rel.elementId());
                e.put("source", sid);
                e.put("target", eid);
                var pred = rel.get("predicate");
                e.put("predicate", pred.isNull() ? "关联" : pred.asString());
                var sr = rel.get("source_ref");
                if (sr.isNull()) {
                    e.putNull("source_ref");
                } else {
                    e.put("source_ref", sr.asString());
                }
                var cf = rel.get("confidence");
                if (cf.isNull()) {
                    e.putNull("confidence");
                } else {
                    e.put("confidence", cf.asDouble());
                }
                edges.add(e);
            }
            ObjectNode out = objectMapper.createObjectNode();
            out.put("found", true);
            out.put("from_id", a);
            out.put("to_id", b);
            out.put("hop_count", path.length());
            out.set("nodes", nodes);
            out.set("edges", edges);
            return out;
        }
    }

    /**
     * 批量导入：请求体为对象，必填 {@code triples} 数组，可选 {@code entities} 对象（id → description/aliases）。
     */
    public JsonNode importFromBundle(JsonNode bundle) {
        requirePassword();
        if (bundle == null || !bundle.isObject()) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "请求体须为 JSON 对象，且包含 triples 数组");
        }
        JsonNode triplesNode = bundle.get("triples");
        if (triplesNode == null || !triplesNode.isArray()) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "缺少 triples 数组");
        }
        if (triplesNode.size() > MAX_IMPORT_TRIPLES) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR,
                    "单次导入三元组超过上限 " + MAX_IMPORT_TRIPLES + " 条");
        }
        List<AgriGraphSeedTriple> triples = parseTriplesArray(triplesNode);
        try (Session session = openSession()) {
            int n = upsertTriples(session, triples);
            int entUpdated = 0;
            JsonNode entities = bundle.get("entities");
            if (entities != null && entities.isObject()) {
                entUpdated = applyEntityEnrichmentsFromObject(session, entities);
            }
            ObjectNode out = objectMapper.createObjectNode();
            out.put("triples_upserted", n);
            out.put("entities_updated", entUpdated);
            return out;
        }
    }

    /**
     * 从本地文件导入：根为 triples bundle，或仅为三元组数组（与种子 JSON 相同）。
     */
    public JsonNode importFromJsonFile(java.nio.file.Path path) throws IOException {
        requirePassword();
        if (path == null || !Files.isRegularFile(path)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "导入文件不存在或不是普通文件");
        }
        JsonNode root = objectMapper.readTree(path.toFile());
        if (root.isArray()) {
            ObjectNode wrap = objectMapper.createObjectNode();
            wrap.set("triples", root);
            return importFromBundle(wrap);
        }
        return importFromBundle(root);
    }

    public JsonNode seedDemo() {
        requirePassword();
        List<AgriGraphSeedTriple> triples = loadSeedTriples();
        try (Session session = openSession()) {
            int count = upsertTriples(session, triples);
            applyEntityEnrichmentsFromClasspath(session);
            ObjectNode out = objectMapper.createObjectNode();
            out.put("upserted", count);
            out.put("source", "classpath:" + SEED_RESOURCE);
            return out;
        }
    }

    private List<AgriGraphSeedTriple> parseTriplesArray(JsonNode triplesNode) {
        List<AgriGraphSeedTriple> list = new ArrayList<>(triplesNode.size());
        for (JsonNode n : triplesNode) {
            list.add(objectMapper.convertValue(n, AgriGraphSeedTriple.class));
        }
        return list;
    }

    private int upsertTriples(Session session, List<AgriGraphSeedTriple> triples) {
        ensureSchema(session);
        AtomicInteger count = new AtomicInteger();
        session.executeWrite(tx -> {
            for (AgriGraphSeedTriple t : triples) {
                mergeTriple(tx, t);
                count.incrementAndGet();
            }
            return null;
        });
        return count.get();
    }

    private List<AgriGraphSeedTriple> loadSeedTriples() {
        if (seedTriplesCache != null) {
            return seedTriplesCache;
        }
        synchronized (this) {
            if (seedTriplesCache != null) {
                return seedTriplesCache;
            }
            try (InputStream in = KnowledgeGraphNeo4jService.class.getResourceAsStream(SEED_RESOURCE)) {
                if (in == null) {
                    log.warn("未找到 {}，使用内嵌少量兜底三元组", SEED_RESOURCE);
                    seedTriplesCache = FALLBACK_SEED;
                    return seedTriplesCache;
                }
                List<AgriGraphSeedTriple> list = objectMapper.readValue(in, new TypeReference<>() { });
                if (list == null || list.isEmpty()) {
                    log.warn("{} 为空，使用兜底数据", SEED_RESOURCE);
                    seedTriplesCache = FALLBACK_SEED;
                } else {
                    seedTriplesCache = List.copyOf(list);
                    log.info("已加载农业知识图谱种子三元组 {} 条（{}）", seedTriplesCache.size(), SEED_RESOURCE);
                }
            } catch (Exception ex) {
                log.error("读取 {} 失败，使用兜底数据: {}", SEED_RESOURCE, ex.getMessage());
                seedTriplesCache = FALLBACK_SEED;
            }
            return seedTriplesCache;
        }
    }

    private void applyEntityEnrichmentsFromClasspath(Session session) {
        try (InputStream in = KnowledgeGraphNeo4jService.class.getResourceAsStream(ENTITY_ENRICH_RESOURCE)) {
            if (in == null) {
                log.warn("未找到 {}，跳过实体说明写入", ENTITY_ENRICH_RESOURCE);
                return;
            }
            JsonNode root = objectMapper.readTree(in);
            if (!root.isObject()) {
                return;
            }
            int n = applyEntityEnrichmentsFromObject(session, root);
            log.info("已写入实体说明（{}），共 {} 条", ENTITY_ENRICH_RESOURCE, n);
        } catch (Exception ex) {
            log.warn("写入实体说明失败: {}", ex.getMessage());
        }
    }

    /**
     * @return 处理的实体条目数（按 key 计数）
     */
    private int applyEntityEnrichmentsFromObject(Session session, JsonNode root) {
        if (root == null || !root.isObject()) {
            return 0;
        }
        AtomicInteger updated = new AtomicInteger();
        root.fields().forEachRemaining(entry -> {
            String id = entry.getKey();
            JsonNode v = entry.getValue();
            String desc = v.path("description").asText("");
            List<String> aliases = new ArrayList<>();
            if (v.has("aliases") && v.get("aliases").isArray()) {
                for (JsonNode a : v.get("aliases")) {
                    if (a.isTextual()) {
                        aliases.add(a.asText());
                    }
                }
            }
            session.run(
                    "MATCH (n:AgriEntity {id:$id}) SET n.description = $d, n.aliases = $a",
                    Values.parameters("id", id, "d", desc, "a", aliases)
            ).consume();
            updated.incrementAndGet();
        });
        return updated.get();
    }

    private void mergeTriple(TransactionContext tx, AgriGraphSeedTriple t) {
        String sref = t.sourceRef() == null ? "" : t.sourceRef();
        tx.run(
                """
                        MERGE (h:AgriEntity {id: $hid})
                        SET h.name = $hname, h.kind = $hkind
                        MERGE (tail:AgriEntity {id: $tid})
                        SET tail.name = $tname, tail.kind = $tkind
                        MERGE (h)-[r:RELATES {predicate: $pred}]->(tail)
                        SET r.source_ref = $sref, r.confidence = $conf
                        """,
                Values.parameters(
                        "hid", t.headId(),
                        "hname", t.headName(),
                        "hkind", t.headKind(),
                        "tid", t.tailId(),
                        "tname", t.tailName(),
                        "tkind", t.tailKind(),
                        "pred", t.predicate(),
                        "sref", sref,
                        "conf", t.confidence() == null ? Values.NULL : Values.value(t.confidence())
                )
        );
    }
}
