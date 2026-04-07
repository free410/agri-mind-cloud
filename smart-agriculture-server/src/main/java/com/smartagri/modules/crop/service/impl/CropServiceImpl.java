package com.smartagri.modules.crop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartagri.common.exception.BusinessException;
import com.smartagri.common.result.ResultCode;
import com.smartagri.modules.crop.dto.CropCreateRequest;
import com.smartagri.modules.crop.dto.CropPageQueryRequest;
import com.smartagri.modules.crop.dto.CropUpdateRequest;
import com.smartagri.modules.crop.entity.Crop;
import com.smartagri.modules.crop.enums.CropGrowthStageEnum;
import com.smartagri.modules.crop.enums.CropHealthStatusEnum;
import com.smartagri.modules.crop.mapper.CropMapper;
import com.smartagri.modules.crop.service.CropService;
import com.smartagri.modules.crop.vo.CropDetailVO;
import com.smartagri.modules.crop.vo.CropListVO;
import com.smartagri.modules.field.entity.Field;
import com.smartagri.modules.field.service.FieldService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CropServiceImpl extends ServiceImpl<CropMapper, Crop> implements CropService {

    private final FieldService fieldService;

    public CropServiceImpl(FieldService fieldService) {
        this.fieldService = fieldService;
    }

    @Override
    public Long createCrop(CropCreateRequest request) {
        validateFarmlandExists(request.getFarmlandId());
        validateDates(request.getSowingDate(), request.getMaturityDate());
        validateEnums(request.getGrowthStage(), request.getHealthStatus());
        checkCropUniqueInFarmland(request.getCropName(), request.getFarmlandId(), null);

        Crop crop = new Crop();
        crop.setCropName(request.getCropName().trim());
        crop.setCropType(request.getCropType().trim());
        crop.setFarmlandId(request.getFarmlandId());
        crop.setSowingDate(request.getSowingDate());
        crop.setMaturityDate(request.getMaturityDate());
        crop.setGrowthStage(request.getGrowthStage().trim());
        crop.setHealthStatus(request.getHealthStatus().trim());
        crop.setRemark(StringUtils.trimToNull(request.getRemark()));
        save(crop);
        return crop.getId();
    }

    @Override
    public void updateCrop(CropUpdateRequest request) {
        Crop crop = getCropOrThrow(request.getId());
        validateFarmlandExists(request.getFarmlandId());
        validateDates(request.getSowingDate(), request.getMaturityDate());
        validateEnums(request.getGrowthStage(), request.getHealthStatus());
        checkCropUniqueInFarmland(request.getCropName(), request.getFarmlandId(), request.getId());

        crop.setCropName(request.getCropName().trim());
        crop.setCropType(request.getCropType().trim());
        crop.setFarmlandId(request.getFarmlandId());
        crop.setSowingDate(request.getSowingDate());
        crop.setMaturityDate(request.getMaturityDate());
        crop.setGrowthStage(request.getGrowthStage().trim());
        crop.setHealthStatus(request.getHealthStatus().trim());
        crop.setRemark(StringUtils.trimToNull(request.getRemark()));
        updateById(crop);
    }

    @Override
    public void deleteCrop(Long id) {
        Crop crop = getCropOrThrow(id);
        removeById(crop.getId());
    }

    @Override
    public CropDetailVO getCropDetail(Long id) {
        Crop crop = getCropOrThrow(id);
        Field field = fieldService.getById(crop.getFarmlandId());
        return buildDetailVO(crop, field);
    }

    @Override
    public IPage<CropListVO> pageCrops(CropPageQueryRequest request) {
        String cropName = StringUtils.trimToNull(request.getCropName());
        String cropType = StringUtils.trimToNull(request.getCropType());

        Page<Crop> page = new Page<>(request.getCurrent(), request.getSize());
        LambdaQueryWrapper<Crop> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StringUtils.isNotBlank(cropName), Crop::getCropName, cropName)
                .eq(StringUtils.isNotBlank(cropType), Crop::getCropType, cropType)
                .eq(request.getFarmlandId() != null, Crop::getFarmlandId, request.getFarmlandId())
                .orderByDesc(Crop::getCreateTime);

        Page<Crop> cropPage = page(page, queryWrapper);
        Map<Long, Field> fieldMap = loadFieldMap(cropPage.getRecords());

        Page<CropListVO> resultPage = new Page<>(cropPage.getCurrent(), cropPage.getSize(), cropPage.getTotal());
        resultPage.setRecords(cropPage.getRecords().stream()
                .map(crop -> buildListVO(crop, fieldMap.get(crop.getFarmlandId())))
                .toList());
        return resultPage;
    }

    private Crop getCropOrThrow(Long id) {
        Crop crop = getById(id);
        if (crop == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "作物不存在");
        }
        return crop;
    }

    private void validateFarmlandExists(Long farmlandId) {
        if (fieldService.getById(farmlandId) == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "所属农田不存在");
        }
    }

    private void validateDates(java.time.LocalDate sowingDate, java.time.LocalDate maturityDate) {
        if (maturityDate.isBefore(sowingDate)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "预计成熟日期不能早于播种日期");
        }
    }

    private void validateEnums(String growthStage, String healthStatus) {
        if (!CropGrowthStageEnum.contains(growthStage.trim())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "作物生长阶段取值不合法");
        }
        if (!CropHealthStatusEnum.contains(healthStatus.trim())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "作物健康状态取值不合法");
        }
    }

    private void checkCropUniqueInFarmland(String cropName, Long farmlandId, Long excludeId) {
        LambdaQueryWrapper<Crop> queryWrapper = new LambdaQueryWrapper<Crop>()
                .eq(Crop::getCropName, cropName.trim())
                .eq(Crop::getFarmlandId, farmlandId);
        if (excludeId != null) {
            queryWrapper.ne(Crop::getId, excludeId);
        }

        if (count(queryWrapper) > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "同一农田下作物名称已存在");
        }
    }

    private Map<Long, Field> loadFieldMap(List<Crop> crops) {
        if (crops == null || crops.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<Long> fieldIds = crops.stream()
                .map(Crop::getFarmlandId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return fieldService.listByIds(fieldIds).stream()
                .collect(Collectors.toMap(Field::getId, Function.identity()));
    }

    private CropListVO buildListVO(Crop crop, Field field) {
        return CropListVO.builder()
                .id(crop.getId())
                .cropName(crop.getCropName())
                .cropType(crop.getCropType())
                .farmlandId(crop.getFarmlandId())
                .farmlandName(field == null ? null : field.getFieldName())
                .sowingDate(crop.getSowingDate())
                .maturityDate(crop.getMaturityDate())
                .growthStage(crop.getGrowthStage())
                .growthStageName(CropGrowthStageEnum.getDescriptionByCode(crop.getGrowthStage()))
                .healthStatus(crop.getHealthStatus())
                .healthStatusName(CropHealthStatusEnum.getDescriptionByCode(crop.getHealthStatus()))
                .remark(crop.getRemark())
                .createTime(crop.getCreateTime())
                .build();
    }

    private CropDetailVO buildDetailVO(Crop crop, Field field) {
        return CropDetailVO.builder()
                .id(crop.getId())
                .cropName(crop.getCropName())
                .cropType(crop.getCropType())
                .farmlandId(crop.getFarmlandId())
                .farmlandName(field == null ? null : field.getFieldName())
                .sowingDate(crop.getSowingDate())
                .maturityDate(crop.getMaturityDate())
                .growthStage(crop.getGrowthStage())
                .growthStageName(CropGrowthStageEnum.getDescriptionByCode(crop.getGrowthStage()))
                .healthStatus(crop.getHealthStatus())
                .healthStatusName(CropHealthStatusEnum.getDescriptionByCode(crop.getHealthStatus()))
                .remark(crop.getRemark())
                .createTime(crop.getCreateTime())
                .updateTime(crop.getUpdateTime())
                .build();
    }
}
