package com.smartagri.modules.field.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartagri.common.exception.BusinessException;
import com.smartagri.common.result.ResultCode;
import com.smartagri.modules.field.dto.FieldCreateRequest;
import com.smartagri.modules.field.dto.FieldPageQueryRequest;
import com.smartagri.modules.field.dto.FieldUpdateRequest;
import com.smartagri.modules.field.entity.Field;
import com.smartagri.modules.field.mapper.FieldMapper;
import com.smartagri.modules.field.service.FieldService;
import com.smartagri.modules.field.vo.FieldDetailVO;
import com.smartagri.modules.field.vo.FieldListVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class FieldServiceImpl extends ServiceImpl<FieldMapper, Field> implements FieldService {

    @Override
    public Long createField(FieldCreateRequest request) {
        checkFieldNameUnique(request.getFieldName(), null);

        Field field = new Field();
        field.setFieldName(request.getFieldName().trim());
        field.setArea(request.getArea());
        field.setLocation(request.getLocation().trim());
        field.setSoilType(request.getSoilType().trim());
        field.setStatus(request.getStatus());
        field.setRemark(StringUtils.trimToNull(request.getRemark()));
        save(field);
        return field.getId();
    }

    @Override
    public void updateField(FieldUpdateRequest request) {
        Field field = getFieldOrThrow(request.getId());
        checkFieldNameUnique(request.getFieldName(), request.getId());

        field.setFieldName(request.getFieldName().trim());
        field.setArea(request.getArea());
        field.setLocation(request.getLocation().trim());
        field.setSoilType(request.getSoilType().trim());
        field.setStatus(request.getStatus());
        field.setRemark(StringUtils.trimToNull(request.getRemark()));
        updateById(field);
    }

    @Override
    public void deleteField(Long id) {
        Field field = getFieldOrThrow(id);
        removeById(field.getId());
    }

    @Override
    public FieldDetailVO getFieldDetail(Long id) {
        Field field = getFieldOrThrow(id);
        return buildDetailVO(field);
    }

    @Override
    public IPage<FieldListVO> pageFields(FieldPageQueryRequest request) {
        String fieldName = StringUtils.trimToNull(request.getFieldName());
        String soilType = StringUtils.trimToNull(request.getSoilType());

        Page<Field> page = new Page<>(request.getCurrent(), request.getSize());
        LambdaQueryWrapper<Field> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StringUtils.isNotBlank(fieldName), Field::getFieldName, fieldName)
                .eq(StringUtils.isNotBlank(soilType), Field::getSoilType, soilType)
                .eq(request.getStatus() != null, Field::getStatus, request.getStatus())
                .orderByDesc(Field::getCreateTime);

        Page<Field> fieldPage = page(page, queryWrapper);
        Page<FieldListVO> resultPage = new Page<>(fieldPage.getCurrent(), fieldPage.getSize(), fieldPage.getTotal());
        resultPage.setRecords(fieldPage.getRecords().stream().map(this::buildListVO).toList());
        return resultPage;
    }

    private Field getFieldOrThrow(Long id) {
        Field field = getById(id);
        if (field == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "农田不存在");
        }
        return field;
    }

    private void checkFieldNameUnique(String fieldName, Long excludeId) {
        LambdaQueryWrapper<Field> queryWrapper = new LambdaQueryWrapper<Field>()
                .eq(Field::getFieldName, fieldName.trim());
        if (excludeId != null) {
            queryWrapper.ne(Field::getId, excludeId);
        }

        long count = count(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "农田名称已存在");
        }
    }

    private FieldListVO buildListVO(Field field) {
        return FieldListVO.builder()
                .id(field.getId())
                .fieldName(field.getFieldName())
                .area(field.getArea())
                .location(field.getLocation())
                .soilType(field.getSoilType())
                .status(field.getStatus())
                .statusName(resolveStatusName(field.getStatus()))
                .remark(field.getRemark())
                .createTime(field.getCreateTime())
                .build();
    }

    private FieldDetailVO buildDetailVO(Field field) {
        return FieldDetailVO.builder()
                .id(field.getId())
                .fieldName(field.getFieldName())
                .area(field.getArea())
                .location(field.getLocation())
                .soilType(field.getSoilType())
                .status(field.getStatus())
                .statusName(resolveStatusName(field.getStatus()))
                .remark(field.getRemark())
                .createTime(field.getCreateTime())
                .updateTime(field.getUpdateTime())
                .build();
    }

    private String resolveStatusName(Integer status) {
        return switch (status) {
            case 0 -> "闲置";
            case 1 -> "种植中";
            case 2 -> "维护中";
            default -> "未知";
        };
    }
}
