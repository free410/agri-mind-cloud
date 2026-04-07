package com.smartagri.modules.ai.controller;

import com.smartagri.common.result.Result;
import com.smartagri.modules.ai.dto.AnalyzeRequest;
import com.smartagri.modules.ai.dto.ChatRequest;
import com.smartagri.modules.ai.dto.WarningAiQueryRequest;
import com.smartagri.modules.ai.service.AiService;
import com.smartagri.modules.ai.vo.AnalyzeResponse;
import com.smartagri.modules.ai.vo.ChatResponse;
import com.smartagri.modules.ai.vo.WarningAiAnalysisVO;
import com.smartagri.modules.ai.vo.WarningDisposalSuggestionVO;
import com.smartagri.modules.ai.vo.WarningQuickSummaryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "AI 智能助手")
@RestController
@RequestMapping("/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @Operation(summary = "AI 对话问答")
    @PostMapping("/chat")
    public Result<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return Result.success(aiService.chat(request));
    }

    @Operation(summary = "AI 业务分析")
    @PostMapping("/analyze")
    public Result<AnalyzeResponse> analyze(@Valid @RequestBody AnalyzeRequest request) {
        return Result.success(aiService.analyze(request));
    }

    @Operation(summary = "获取预警AI分析结果")
    @GetMapping("/warning/analysis")
    public Result<WarningAiAnalysisVO> analyzeWarning(@Valid WarningAiQueryRequest request) {
        return Result.success(aiService.analyzeWarning(request.getWarningId()));
    }

    @Operation(summary = "生成预警处置建议")
    @GetMapping("/warning/disposal")
    public Result<WarningDisposalSuggestionVO> suggestWarningDisposal(@Valid WarningAiQueryRequest request) {
        return Result.success(aiService.suggestWarningDisposal(request.getWarningId()));
    }

    @Operation(summary = "预警快捷聚合分析（异常简报/闭环总结）")
    @GetMapping("/warning/quick-summary")
    public Result<WarningQuickSummaryVO> getWarningQuickSummary(
            @RequestParam(defaultValue = "abnormal") String mode) {
        return Result.success(aiService.getWarningQuickSummary(mode));
    }
}
