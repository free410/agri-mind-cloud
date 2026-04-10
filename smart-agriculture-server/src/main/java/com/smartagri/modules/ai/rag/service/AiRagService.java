package com.smartagri.modules.ai.rag.service;

import com.smartagri.modules.ai.rag.dto.RagChatRequest;
import com.smartagri.modules.ai.rag.vo.RagChatResponse;

public interface AiRagService {

    RagChatResponse chat(RagChatRequest request);
}
