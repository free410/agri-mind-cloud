package com.smartagri.modules.knowledgegraph.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.smartagri.common.exception.BusinessException;
import com.smartagri.common.properties.KnowledgeGraphProperties;
import com.smartagri.common.result.Result;
import com.smartagri.common.result.ResultCode;
import com.smartagri.modules.knowledgegraph.service.KnowledgeGraphNeo4jService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/knowledge-graph")
@Tag(name = "农业知识图谱")
public class KnowledgeGraphController {

    private final KnowledgeGraphProperties properties;
    private final KnowledgeGraphNeo4jService graphService;

    public KnowledgeGraphController(KnowledgeGraphProperties properties,
                                    KnowledgeGraphNeo4jService graphService) {
        this.properties = properties;
        this.graphService = graphService;
    }

    @GetMapping("/health")
    @Operation(summary = "Neo4j 连通性（Spring 直连）")
    public Result<JsonNode> health() {
        requireEnabled();
        return Result.success(graphService.health());
    }

    @GetMapping("/stats")
    @Operation(summary = "图谱规模统计")
    public Result<JsonNode> stats() {
        requireEnabled();
        return Result.success(graphService.stats());
    }

    @GetMapping("/search")
    @Operation(summary = "按名称或 id 搜索实体")
    public Result<JsonNode> search(
            @RequestParam(value = "q", defaultValue = "") String q,
            @RequestParam(value = "limit", defaultValue = "20") int limit
    ) {
        requireEnabled();
        return Result.success(graphService.search(q, limit));
    }

    @GetMapping("/subgraph")
    @Operation(summary = "以某实体为中心展开子图")
    public Result<JsonNode> subgraph(
            @RequestParam("centerId") String centerId,
            @RequestParam(value = "depth", defaultValue = "2") int depth
    ) {
        requireEnabled();
        return Result.success(graphService.subgraph(centerId, depth));
    }

    @GetMapping("/entity/{id}")
    @Operation(summary = "实体详情（业务说明、出入度、一度关系）")
    public Result<JsonNode> entityDetail(@PathVariable("id") String id) {
        requireEnabled();
        return Result.success(graphService.entityDetail(id));
    }

    @GetMapping("/path")
    @Operation(summary = "两实体间最短路径（无向）")
    public Result<JsonNode> shortestPath(
            @RequestParam("fromId") String fromId,
            @RequestParam("toId") String toId,
            @RequestParam(value = "maxHops", defaultValue = "10") int maxHops
    ) {
        requireEnabled();
        return Result.success(graphService.shortestPath(fromId, toId, maxHops));
    }

    @PostMapping("/seed-demo")
    @Operation(summary = "写入演示三元组（开发/体验）")
    public Result<JsonNode> seedDemo() {
        requireEnabled();
        return Result.success(graphService.seedDemo());
    }

    @PostMapping("/import")
    @Operation(summary = "批量导入三元组与实体说明（需管理员登录）")
    public Result<JsonNode> importBundle(@RequestBody JsonNode body) {
        requireEnabled();
        return Result.success(graphService.importFromBundle(body));
    }

    @PostMapping("/pipeline/ingest")
    @Operation(summary = "流水线入库（请求头 X-Pipeline-Ingest-Key，需在 jwt.ignore-urls 放行本路径）")
    public Result<JsonNode> pipelineIngest(
            @RequestHeader(value = "X-Pipeline-Ingest-Key", required = false) String pipelineKey,
            @RequestBody JsonNode body
    ) {
        requireEnabled();
        String expected = StringUtils.trimToEmpty(properties.getPipelineIngestKey());
        if (StringUtils.isBlank(expected)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR,
                    "未启用流水线入库：请在配置中设置 smart-agri.knowledge-graph.pipeline-ingest-key");
        }
        if (!StringUtils.equals(expected, StringUtils.trimToEmpty(pipelineKey))) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "Pipeline 密钥无效");
        }
        return Result.success(graphService.importFromBundle(body));
    }

    private void requireEnabled() {
        if (!properties.isEnabled()) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "知识图谱功能已关闭");
        }
    }
}
