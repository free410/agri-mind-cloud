package com.smartagri.modules.knowledgegraph.bootstrap;

import com.smartagri.common.properties.KnowledgeGraphProperties;
import com.smartagri.modules.knowledgegraph.service.KnowledgeGraphNeo4jService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 启动时可选自动导入本地 bundle（用于容器/流水线环境无需手工点「种子导入」）。
 */
@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@ConditionalOnProperty(prefix = "smart-agri.knowledge-graph", name = "auto-ingest-on-startup", havingValue = "true")
public class KgAutoIngestRunner implements ApplicationRunner {

    private final KnowledgeGraphProperties properties;
    private final KnowledgeGraphNeo4jService graphService;

    public KgAutoIngestRunner(KnowledgeGraphProperties properties, KnowledgeGraphNeo4jService graphService) {
        this.properties = properties;
        this.graphService = graphService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }
        String raw = StringUtils.trimToEmpty(properties.getAutoIngestBundlePath());
        if (StringUtils.isBlank(raw)) {
            log.warn("已开启 auto-ingest-on-startup，但未配置 auto-ingest-bundle-path，跳过自动入库");
            return;
        }
        Path path = Paths.get(raw);
        if (!Files.isRegularFile(path)) {
            log.warn("自动入库文件不存在，跳过：{}", path.toAbsolutePath());
            return;
        }
        try {
            var result = graphService.importFromJsonFile(path);
            log.info("知识图谱自动入库完成：{}", result);
        } catch (Exception ex) {
            log.error("知识图谱自动入库失败: {}", ex.getMessage());
        }
    }
}
