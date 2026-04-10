package com.smartagri.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "smart-agri.knowledge-graph")
public class KnowledgeGraphProperties {

    /**
     * 是否启用知识图谱 REST 接口（Spring 直连 Neo4j）。
     */
    private boolean enabled = true;

    /**
     * 历史字段：曾用于代理 FastAPI；图谱已直连 Neo4j，保留以免旧配置报错。
     */
    private String baseUrl = "http://localhost:9001";

    private Integer connectTimeoutSeconds = 5;

    private Integer readTimeoutSeconds = 60;

    /**
     * Bolt 地址，与 Neo4j Browser 中 neo4j://localhost:7687 对应，Java 使用 bolt://
     */
    private String neo4jUri = "bolt://localhost:7687";

    private String neo4jUser = "neo4j";

    /**
     * 与 docker-compose.neo4j.yml 中 NEO4J_AUTH 密码一致；生产用 SMART_AGRI_NEO4J_PASSWORD。
     */
    private String neo4jPassword = "";

    /**
     * 多库时填写库名；单库默认留空
     */
    private String neo4jDatabase = "";

    private Integer neo4jMaxSubgraphPaths = 400;

    /**
     * 非空时开放 {@code POST /knowledge-graph/pipeline/ingest}（需在 jwt.ignore-urls 中放行该路径），
     * 请求头 {@code X-Pipeline-Ingest-Key} 须与本值一致。建议通过环境变量 SMART_AGRI_KG_PIPELINE_KEY 注入。
     */
    private String pipelineIngestKey = "";

    /**
     * 为 true 时，启动后若 {@link #autoIngestBundlePath} 指向的 JSON 文件存在则自动执行导入。
     */
    private boolean autoIngestOnStartup = false;

    /**
     * 自动导入的 bundle 文件路径：JSON 对象含 {@code triples} 数组，可选 {@code entities} 对象（与 agri-graph-entities.json 同结构）。
     */
    private String autoIngestBundlePath = "";

    /**
     * 智能问答调用 RAG 前是否注入图谱检索摘要（需 Neo4j 可用）。
     */
    private boolean ragContextEnabled = true;
}
