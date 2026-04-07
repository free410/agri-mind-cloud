package com.smartagri.modules.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartagri.modules.ai.dto.AnalyzeRequest;
import com.smartagri.modules.ai.dto.ChatRequest;
import com.smartagri.modules.ai.entity.AiChatLog;
import com.smartagri.modules.ai.vo.AnalyzeResponse;
import com.smartagri.modules.ai.vo.ChatResponse;
import com.smartagri.modules.ai.vo.WarningAiAnalysisVO;
import com.smartagri.modules.ai.vo.WarningDisposalSuggestionVO;
import com.smartagri.modules.ai.vo.WarningQuickSummaryVO;

public interface AiService extends IService<AiChatLog> {

    ChatResponse chat(ChatRequest request);

    AnalyzeResponse analyze(AnalyzeRequest request);

    WarningAiAnalysisVO analyzeWarning(Long warningId);

    WarningDisposalSuggestionVO suggestWarningDisposal(Long warningId);

    WarningQuickSummaryVO getWarningQuickSummary(String mode);
}
