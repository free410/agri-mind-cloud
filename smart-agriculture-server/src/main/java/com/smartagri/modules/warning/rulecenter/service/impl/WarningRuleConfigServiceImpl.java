package com.smartagri.modules.warning.rulecenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartagri.common.exception.BusinessException;
import com.smartagri.common.result.ResultCode;
import com.smartagri.modules.crop.service.CropService;
import com.smartagri.modules.field.service.FieldService;
import com.smartagri.modules.warning.enums.WarningTypeEnum;
import com.smartagri.modules.warning.rulecenter.dto.WarningRuleCreateRequest;
import com.smartagri.modules.warning.rulecenter.dto.WarningRulePageQueryRequest;
import com.smartagri.modules.warning.rulecenter.dto.WarningRuleUpdateRequest;
import com.smartagri.modules.warning.rulecenter.entity.WarningRuleConfig;
import com.smartagri.modules.warning.rulecenter.enums.WarningRuleCompareOperatorEnum;
import com.smartagri.modules.warning.rulecenter.enums.WarningRuleMetricKeyEnum;
import com.smartagri.modules.warning.rulecenter.enums.WarningRuleScopeTypeEnum;
import com.smartagri.modules.warning.rulecenter.mapper.WarningRuleConfigMapper;
import com.smartagri.modules.warning.rulecenter.service.WarningRuleConfigService;
import com.smartagri.modules.warning.rulecenter.vo.WarningRuleListVO;
import com.smartagri.modules.warning.rulecenter.vo.WarningRuleSummaryVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class WarningRuleConfigServiceImpl extends ServiceImpl<WarningRuleConfigMapper, WarningRuleConfig>
        implements WarningRuleConfigService {

    private final FieldService fieldService;
    private final CropService cropService;

    public WarningRuleConfigServiceImpl(FieldService fieldService, CropService cropService) {
        this.fieldService = fieldService;
        this.cropService = cropService;
    }

    @Override
    public IPage<WarningRuleListVO> pageRules(WarningRulePageQueryRequest request) {
        Page<WarningRuleConfig> page = new Page<>(request.getCurrent(), request.getSize());
        LambdaQueryWrapper<WarningRuleConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StringUtils.isNotBlank(request.getRuleName()), WarningRuleConfig::getRuleName, request.getRuleName())
                .eq(StringUtils.isNotBlank(request.getWarningType()), WarningRuleConfig::getWarningType, request.getWarningType())
                .eq(request.getEnabled() != null, WarningRuleConfig::getEnabled, request.getEnabled())
                .orderByDesc(WarningRuleConfig::getUpdateTime)
                .orderByDesc(WarningRuleConfig::getId);

        Page<WarningRuleConfig> dataPage = page(page, queryWrapper);
        Page<WarningRuleListVO> result = new Page<>(dataPage.getCurrent(), dataPage.getSize(), dataPage.getTotal());
        result.setRecords(dataPage.getRecords().stream().map(this::toListVO).toList());
        return result;
    }

    @Override
    public WarningRuleSummaryVO getSummary() {
        long total = count();
        long enabled = count(new LambdaQueryWrapper<WarningRuleConfig>().eq(WarningRuleConfig::getEnabled, 1));
        long global = count(new LambdaQueryWrapper<WarningRuleConfig>()
                .eq(WarningRuleConfig::getTargetScopeType, WarningRuleScopeTypeEnum.GLOBAL.getCode()));

        return WarningRuleSummaryVO.builder()
                .totalCount(total)
                .enabledCount(enabled)
                .globalCount(global)
                .scopedCount(Math.max(0, total - global))
                .build();
    }

    @Override
    public Long createRule(WarningRuleCreateRequest request) {
        validateRuleInput(request.getRuleCode(), request.getWarningType(), request.getTargetScopeType(),
                request.getTargetScopeId(), request.getMetricKey(), request.getCompareOperator());
        checkRuleCodeUnique(request.getRuleCode(), null);

        WarningRuleConfig config = new WarningRuleConfig();
        config.setRuleName(StringUtils.trimToEmpty(request.getRuleName()));
        config.setRuleCode(normalizeRuleCode(request.getRuleCode()));
        config.setWarningType(request.getWarningType());
        config.setWarningLevel(request.getWarningLevel());
        config.setTargetScopeType(request.getTargetScopeType());
        config.setTargetScopeId(request.getTargetScopeId());
        config.setMetricKey(request.getMetricKey());
        config.setCompareOperator(request.getCompareOperator());
        config.setThresholdValue(request.getThresholdValue());
        config.setContinuousCount(request.getContinuousCount());
        config.setDeduplicateMinutes(request.getDeduplicateMinutes());
        config.setEnabled(request.getEnabled());
        config.setDescription(request.getDescription());
        save(config);
        return config.getId();
    }

    @Override
    public void updateRule(Long id, WarningRuleUpdateRequest request) {
        WarningRuleConfig config = getById(id);
        if (config == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "规则不存在");
        }

        validateRuleInput(config.getRuleCode(), request.getWarningType(), request.getTargetScopeType(),
                request.getTargetScopeId(), request.getMetricKey(), request.getCompareOperator());

        config.setRuleName(StringUtils.trimToEmpty(request.getRuleName()));
        config.setWarningType(request.getWarningType());
        config.setWarningLevel(request.getWarningLevel());
        config.setTargetScopeType(request.getTargetScopeType());
        config.setTargetScopeId(request.getTargetScopeId());
        config.setMetricKey(request.getMetricKey());
        config.setCompareOperator(request.getCompareOperator());
        config.setThresholdValue(request.getThresholdValue());
        config.setContinuousCount(request.getContinuousCount());
        config.setDeduplicateMinutes(request.getDeduplicateMinutes());
        config.setEnabled(request.getEnabled());
        config.setDescription(request.getDescription());
        updateById(config);
    }

    @Override
    public void updateEnabled(Long id, Integer enabled) {
        WarningRuleConfig config = getById(id);
        if (config == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "规则不存在");
        }
        if (!Objects.equals(enabled, 0) && !Objects.equals(enabled, 1)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "启用状态取值不合法");
        }
        config.setEnabled(enabled);
        updateById(config);
    }

    @Override
    public void deleteRule(Long id) {
        WarningRuleConfig config = getById(id);
        if (config == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "规则不存在");
        }
        removeById(id);
    }

    @Override
    public List<WarningRuleConfig> listMatchedEnabledRules(String metricKey, Long farmlandId, Long cropId) {
        LambdaQueryWrapper<WarningRuleConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WarningRuleConfig::getEnabled, 1)
                .eq(WarningRuleConfig::getMetricKey, metricKey)
                .eq(WarningRuleConfig::getTargetScopeType, WarningRuleScopeTypeEnum.GLOBAL.getCode());

        List<WarningRuleConfig> result = new ArrayList<>(list(queryWrapper));
        if (farmlandId != null) {
            result.addAll(list(new LambdaQueryWrapper<WarningRuleConfig>()
                    .eq(WarningRuleConfig::getEnabled, 1)
                    .eq(WarningRuleConfig::getMetricKey, metricKey)
                    .eq(WarningRuleConfig::getTargetScopeType, WarningRuleScopeTypeEnum.FARMLAND.getCode())
                    .eq(WarningRuleConfig::getTargetScopeId, farmlandId)));
        }
        if (cropId != null) {
            result.addAll(list(new LambdaQueryWrapper<WarningRuleConfig>()
                    .eq(WarningRuleConfig::getEnabled, 1)
                    .eq(WarningRuleConfig::getMetricKey, metricKey)
                    .eq(WarningRuleConfig::getTargetScopeType, WarningRuleScopeTypeEnum.CROP.getCode())
                    .eq(WarningRuleConfig::getTargetScopeId, cropId)));
        }
        result.sort((a, b) -> Long.compare(a.getId(), b.getId()));
        return result;
    }

    private void validateRuleInput(String ruleCode,
                                   String warningType,
                                   String targetScopeType,
                                   Long targetScopeId,
                                   String metricKey,
                                   String compareOperator) {
        if (!WarningTypeEnum.contains(warningType)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "预警类型不合法");
        }
        if (!WarningRuleScopeTypeEnum.contains(targetScopeType)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "作用范围类型不合法");
        }
        if (!WarningRuleMetricKeyEnum.contains(metricKey)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "监测指标不合法");
        }
        if (!WarningRuleCompareOperatorEnum.contains(compareOperator)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "比较符不合法");
        }

        String normalizedCode = normalizeRuleCode(ruleCode);
        if (StringUtils.isBlank(normalizedCode)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "规则编码不能为空");
        }

        if (!Objects.equals(targetScopeType, WarningRuleScopeTypeEnum.GLOBAL.getCode()) && targetScopeId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "定向规则必须传入作用范围ID");
        }

        if (Objects.equals(targetScopeType, WarningRuleScopeTypeEnum.FARMLAND.getCode())
                && targetScopeId != null && fieldService.getById(targetScopeId) == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "指定农田不存在");
        }

        if (Objects.equals(targetScopeType, WarningRuleScopeTypeEnum.CROP.getCode())
                && targetScopeId != null && cropService.getById(targetScopeId) == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "指定作物不存在");
        }
    }

    private void checkRuleCodeUnique(String ruleCode, Long excludeId) {
        String normalizedRuleCode = normalizeRuleCode(ruleCode);
        LambdaQueryWrapper<WarningRuleConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WarningRuleConfig::getRuleCode, normalizedRuleCode)
                .ne(excludeId != null, WarningRuleConfig::getId, excludeId);
        if (count(queryWrapper) > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "规则编码已存在");
        }
    }

    private String normalizeRuleCode(String ruleCode) {
        return StringUtils.upperCase(StringUtils.trimToEmpty(ruleCode));
    }

    private WarningRuleListVO toListVO(WarningRuleConfig item) {
        return WarningRuleListVO.builder()
                .id(item.getId())
                .ruleName(item.getRuleName())
                .ruleCode(item.getRuleCode())
                .warningType(item.getWarningType())
                .warningLevel(item.getWarningLevel())
                .targetScopeType(item.getTargetScopeType())
                .targetScopeTypeName(WarningRuleScopeTypeEnum.getDescriptionByCode(item.getTargetScopeType()))
                .targetScopeId(item.getTargetScopeId())
                .metricKey(item.getMetricKey())
                .metricName(WarningRuleMetricKeyEnum.getDescriptionByCode(item.getMetricKey()))
                .compareOperator(item.getCompareOperator())
                .thresholdValue(item.getThresholdValue())
                .continuousCount(item.getContinuousCount())
                .deduplicateMinutes(item.getDeduplicateMinutes())
                .enabled(item.getEnabled())
                .description(item.getDescription())
                .createTime(item.getCreateTime())
                .updateTime(item.getUpdateTime())
                .build();
    }
}

