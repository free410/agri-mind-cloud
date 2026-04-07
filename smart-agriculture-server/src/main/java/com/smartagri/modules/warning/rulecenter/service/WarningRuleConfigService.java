package com.smartagri.modules.warning.rulecenter.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.smartagri.modules.warning.rulecenter.dto.WarningRuleCreateRequest;
import com.smartagri.modules.warning.rulecenter.dto.WarningRulePageQueryRequest;
import com.smartagri.modules.warning.rulecenter.dto.WarningRuleUpdateRequest;
import com.smartagri.modules.warning.rulecenter.entity.WarningRuleConfig;
import com.smartagri.modules.warning.rulecenter.vo.WarningRuleListVO;
import com.smartagri.modules.warning.rulecenter.vo.WarningRuleSummaryVO;

import java.util.List;

public interface WarningRuleConfigService extends IService<WarningRuleConfig> {

    IPage<WarningRuleListVO> pageRules(WarningRulePageQueryRequest request);

    WarningRuleSummaryVO getSummary();

    Long createRule(WarningRuleCreateRequest request);

    void updateRule(Long id, WarningRuleUpdateRequest request);

    void updateEnabled(Long id, Integer enabled);

    void deleteRule(Long id);

    List<WarningRuleConfig> listMatchedEnabledRules(String metricKey, Long farmlandId, Long cropId);
}

