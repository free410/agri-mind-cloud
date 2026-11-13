package com.smartagri.modules.ai.rag.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartagri.common.properties.KnowledgeGraphProperties;
import com.smartagri.modules.ai.rag.vo.RagGraphCitationVO;
import com.smartagri.modules.knowledgegraph.service.KnowledgeGraphNeo4jService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 根据用户问题检索图谱实体，生成注入 RAG 的短文本与引用列表。
 */
@Slf4j
@Service
public class KnowledgeGraphRagContextProvider {

    private static final int SEARCH_LIMIT = 8;
    private static final int DETAIL_FOR_FIRST = 4;
    private static final int REL_OUT_PREVIEW = 5;
    private static final int SUBGRAPH_DEPTH = 1;  // 迷你图谱展开深度

    private final KnowledgeGraphProperties graphProperties;
    private final KnowledgeGraphNeo4jService neo4jService;
    private final ObjectMapper objectMapper;

    public KnowledgeGraphRagContextProvider(KnowledgeGraphProperties graphProperties,
                                            KnowledgeGraphNeo4jService neo4jService,
                                            ObjectMapper objectMapper) {
        this.graphProperties = graphProperties;
        this.neo4jService = neo4jService;
        this.objectMapper = objectMapper;
    }

    public KnowledgeGraphRagSnippet build(String userQuery) {
        if (!graphProperties.isEnabled() || !graphProperties.isRagContextEnabled()) {
            log.warn("图谱RAG上下文已禁用");
            return KnowledgeGraphRagSnippet.empty();
        }
        if (StringUtils.isBlank(graphProperties.getNeo4jPassword())) {
            log.warn("Neo4j密码未配置");
            return KnowledgeGraphRagSnippet.empty();
        }
        String q = StringUtils.trimToEmpty(userQuery);
        if (q.length() > 120) {
            q = q.substring(0, 120);
        }
        if (StringUtils.isBlank(q)) {
            log.warn("用户问题为空");
            return KnowledgeGraphRagSnippet.empty();
        }
        try {
            JsonNode hits = neo4jService.search(q, SEARCH_LIMIT);

            if (!hits.isArray() || hits.isEmpty()) {
                log.warn("搜索结果为空");
                return KnowledgeGraphRagSnippet.empty();
            }
            List<RagGraphCitationVO> citations = new ArrayList<>();
            StringBuilder sb = new StringBuilder();
            sb.append("【知识图谱检索】以下实体名称或 id 与用户问题相关，可作辅证（勿编造未列出关系）：\n");
            for (int i = 0; i < hits.size(); i++) {
                JsonNode h = hits.get(i);
                String id = h.path("id").asText("");
                String name = h.path("name").asText(id);
                String kind = h.path("kind").asText("concept");

                // 获取子图数据用于前端可视化
                Map<String, Object> subgraph = null;
                try {
                    JsonNode subgraphJson = neo4jService.subgraph(id, SUBGRAPH_DEPTH);

                    if (subgraphJson != null && subgraphJson.has("nodes") && subgraphJson.path("nodes").size() > 0) {
                        subgraph = objectMapper.convertValue(subgraphJson, Map.class);
                    }
                } catch (Exception ex) {
                    log.warn("获取实体 {} 的子图失败: {}", id, ex.getMessage());
                }

                citations.add(RagGraphCitationVO.builder()
                        .id(id)
                        .title(name)
                        .kind(kind)
                        .subgraph(subgraph)
                        .build());
                sb.append("- ").append(name).append("（").append(kind).append("，id=").append(id).append("）");
                if (i < DETAIL_FOR_FIRST) {
                    appendRelationPreview(sb, id);
                }
                sb.append("\n");
            }
            return new KnowledgeGraphRagSnippet(sb.toString().trim(), List.copyOf(citations));
        } catch (Exception ex) {
            log.error("构建知识图谱 RAG 上下文失败: {}", ex.getMessage(), ex);
            return KnowledgeGraphRagSnippet.empty();
        }
    }

    private void appendRelationPreview(StringBuilder sb, String entityId) {
        JsonNode det = neo4jService.entityDetail(entityId);
        if (!det.path("found").asBoolean(false)) {
            return;
        }
        JsonNode out = det.get("relations_out");
        if (out == null || !out.isArray() || out.isEmpty()) {
            return;
        }
        sb.append(" → 出边：");
        int n = Math.min(out.size(), REL_OUT_PREVIEW);
        for (int j = 0; j < n; j++) {
            JsonNode r = out.get(j);
            String pred = r.path("predicate").asText("关联");
            String tname = r.path("name").asText(r.path("id").asText(""));
            if (j > 0) {
                sb.append("；");
            }
            sb.append(pred).append("→").append(tname);
        }
        if (out.size() > n) {
            sb.append("…");
        }
    }

    public record KnowledgeGraphRagSnippet(String contextText, List<RagGraphCitationVO> citations) {
        static KnowledgeGraphRagSnippet empty() {
            return new KnowledgeGraphRagSnippet(null, List.of());
        }
    }
}
