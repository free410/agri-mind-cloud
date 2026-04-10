package com.smartagri.modules.ai.rag.client;

import com.smartagri.modules.ai.rag.client.dto.RagAnswerClientRequest;
import com.smartagri.modules.ai.rag.client.dto.RagAnswerClientResponse;

public interface RagClient {

    RagAnswerClientResponse answer(RagAnswerClientRequest request);
}
