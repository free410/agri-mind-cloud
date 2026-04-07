package com.smartagri.modules.crop.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.smartagri.modules.crop.dto.CropCreateRequest;
import com.smartagri.modules.crop.dto.CropPageQueryRequest;
import com.smartagri.modules.crop.dto.CropUpdateRequest;
import com.smartagri.modules.crop.entity.Crop;
import com.smartagri.modules.crop.vo.CropDetailVO;
import com.smartagri.modules.crop.vo.CropListVO;

public interface CropService extends IService<Crop> {

    Long createCrop(CropCreateRequest request);

    void updateCrop(CropUpdateRequest request);

    void deleteCrop(Long id);

    CropDetailVO getCropDetail(Long id);

    IPage<CropListVO> pageCrops(CropPageQueryRequest request);
}
