package com.smartagri.modules.ai.rag.controller;

import com.smartagri.common.result.Result;
import com.smartagri.modules.ai.rag.dto.RagChatRequest;
import com.smartagri.modules.ai.rag.service.AiRagService;
import com.smartagri.modules.ai.rag.vo.RagChatResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/ai/rag")
@Tag(name = "AI RAG 问答")
public class AiRagController {

    private final AiRagService aiRagService;

    public AiRagController(AiRagService aiRagService) {
        this.aiRagService = aiRagService;
    }

    @PostMapping("/chat")
    @Operation(summary = "RAG 结构化问答")
    public Result<RagChatResponse> chat(@Valid @RequestBody RagChatRequest request) {
        return Result.success(aiRagService.chat(request));
    }
}
