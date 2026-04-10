package com.smartagri.modules.knowledgegraph.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 与 {@code knowledge-graph/agri-graph-seed.json} 数组元素一致，由脚本 {@code scripts/generate_agri_graph_seed.py} 生成。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AgriGraphSeedTriple(
        @JsonAlias("head_id") String headId,
        @JsonAlias("head_name") String headName,
        @JsonAlias("head_kind") String headKind,
        @JsonAlias("tail_id") String tailId,
        @JsonAlias("tail_name") String tailName,
        @JsonAlias("tail_kind") String tailKind,
        String predicate,
        @JsonAlias("source_ref") String sourceRef,
        Double confidence
) {
}
