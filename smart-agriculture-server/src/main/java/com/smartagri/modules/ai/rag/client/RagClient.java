package com.smartagri.modules.ai.rag.client;

import com.smartagri.modules.ai.rag.client.dto.RagAnswerClientRequest;
import com.smartagri.modules.ai.rag.client.dto.RagAnswerClientResponse;

import java.util.Map;

public interface RagClient {

    RagAnswerClientResponse answer(RagAnswerClientRequest request);

    /**
     * 上传知识文档，由 RAG 服务落盘并增量写入向量库。
     */
    Map<String, Object> uploadKnowledge(String filename, byte[] content, String category);

    /**
     * 查询知识库文档列表（含每个文档的向量条数）。
     */
    Map<String, Object> listKnowledgeDocuments();

    /**
     * 删除知识文档并同步删除其向量。
     */
    Map<String, Object> deleteKnowledgeDocument(String path);
}
