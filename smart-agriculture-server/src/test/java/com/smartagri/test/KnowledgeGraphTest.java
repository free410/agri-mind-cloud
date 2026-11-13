package com.smartagri.test;

import com.smartagri.modules.ai.rag.service.KnowledgeGraphRagContextProvider;
import com.smartagri.modules.ai.rag.service.KnowledgeGraphRagContextProvider.KnowledgeGraphRagSnippet;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@Slf4j
public class KnowledgeGraphTest {

    public static void main(String[] args) {
        SpringApplication.run(KnowledgeGraphTest.class, args);
    }

    @Bean
    CommandLineRunner testRunner(KnowledgeGraphRagContextProvider provider) {
        return args -> {
            log.info("===========================================");
            log.info("测试 KnowledgeGraphRagContextProvider");
            log.info("===========================================");

            String query = "草莓灰霉病";
            log.info("调用 provider.build(\"{}\")", query);

            KnowledgeGraphRagSnippet result = provider.build(query);

            log.info("结果:");
            log.info("  contextText: {}", result.contextText() != null ?
                result.contextText().substring(0, Math.min(200, result.contextText().length())) : "null");
            log.info("  citations数量: {}", result.citations().size());

            for (int i = 0; i < Math.min(3, result.citations().size()); i++) {
                var citation = result.citations().get(i);
                log.info("  [{}/{}] id={}, title={}, kind={}, subgraph={}",
                    i + 1, result.citations().size(),
                    citation.getId(),
                    citation.getTitle(),
                    citation.getKind(),
                    citation.getSubgraph() != null ? "有数据" : "空");
            }

            log.info("===========================================");
        };
    }
}
