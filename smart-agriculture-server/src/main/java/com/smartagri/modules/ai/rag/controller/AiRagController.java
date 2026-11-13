package com.smartagri.modules.ai.rag.controller;

import com.smartagri.common.exception.BusinessException;
import com.smartagri.common.result.Result;
import com.smartagri.common.result.ResultCode;
import com.smartagri.modules.ai.rag.client.RagClient;
import com.smartagri.modules.ai.rag.dto.RagChatRequest;
import com.smartagri.modules.ai.rag.service.AiRagService;
import com.smartagri.modules.ai.rag.vo.RagChatResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/ai/rag")
@Tag(name = "AI RAG 问答")
public class AiRagController {

    private final AiRagService aiRagService;
    private final RagClient ragClient;

    public AiRagController(AiRagService aiRagService, RagClient ragClient) {
        this.aiRagService = aiRagService;
        this.ragClient = ragClient;
    }

    @PostMapping("/chat")
    @Operation(summary = "RAG 结构化问答")
    public Result<RagChatResponse> chat(@Valid @RequestBody RagChatRequest request) {
        return Result.success(aiRagService.chat(request));
    }

    @PostMapping("/knowledge/upload")
    @Operation(summary = "上传知识文档并写入向量库")
    public Result<Map<String, Object>> uploadKnowledge(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", defaultValue = "crop") String category) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "请选择要上传的文件");
        }
        return Result.success(ragClient.uploadKnowledge(file.getOriginalFilename(), file.getBytes(), category));
    }

    @GetMapping("/knowledge/documents")
    @Operation(summary = "查询知识库文档列表")
    public Result<Map<String, Object>> listKnowledgeDocuments() {
        return Result.success(ragClient.listKnowledgeDocuments());
    }

    @DeleteMapping("/knowledge/documents")
    @Operation(summary = "删除知识文档并同步删除向量")
    public Result<Map<String, Object>> deleteKnowledgeDocument(@RequestParam("path") String path) {
        if (StringUtils.isBlank(path)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "缺少文档路径");
        }
        return Result.success(ragClient.deleteKnowledgeDocument(path));
    }
}
