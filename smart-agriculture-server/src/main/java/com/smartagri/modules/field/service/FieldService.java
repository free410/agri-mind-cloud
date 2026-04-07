package com.smartagri.modules.field.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.smartagri.modules.field.dto.FieldCreateRequest;
import com.smartagri.modules.field.dto.FieldPageQueryRequest;
import com.smartagri.modules.field.dto.FieldUpdateRequest;
import com.smartagri.modules.field.entity.Field;
import com.smartagri.modules.field.vo.FieldDetailVO;
import com.smartagri.modules.field.vo.FieldListVO;

public interface FieldService extends IService<Field> {

    Long createField(FieldCreateRequest request);

    void updateField(FieldUpdateRequest request);

    void deleteField(Long id);

    FieldDetailVO getFieldDetail(Long id);

    IPage<FieldListVO> pageFields(FieldPageQueryRequest request);
}
