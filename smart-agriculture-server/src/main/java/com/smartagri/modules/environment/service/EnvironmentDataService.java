package com.smartagri.modules.environment.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.smartagri.modules.environment.dto.EnvironmentDataCreateRequest;
import com.smartagri.modules.environment.dto.EnvironmentDataPageQueryRequest;
import com.smartagri.modules.environment.dto.EnvironmentTrendQueryRequest;
import com.smartagri.modules.environment.entity.EnvironmentData;
import com.smartagri.modules.environment.vo.EnvironmentDataListVO;
import com.smartagri.modules.environment.vo.EnvironmentTrendVO;

public interface EnvironmentDataService extends IService<EnvironmentData> {

    Long createEnvironmentData(EnvironmentDataCreateRequest request);

    IPage<EnvironmentDataListVO> pageEnvironmentData(EnvironmentDataPageQueryRequest request);

    EnvironmentTrendVO getTrendData(EnvironmentTrendQueryRequest request);
}
