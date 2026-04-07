package com.smartagri.modules.warning.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.smartagri.modules.environment.entity.EnvironmentData;
import com.smartagri.modules.warning.dto.WarningFalseAlarmRequest;
import com.smartagri.modules.warning.dto.WarningFlowActionRequest;
import com.smartagri.modules.warning.dto.WarningHandleSubmitRequest;
import com.smartagri.modules.warning.dto.WarningPageQueryRequest;
import com.smartagri.modules.warning.dto.WarningStatusUpdateRequest;
import com.smartagri.modules.warning.entity.WarningRecord;
import com.smartagri.modules.warning.vo.WarningDetailVO;
import com.smartagri.modules.warning.vo.WarningDisposalReportVO;
import com.smartagri.modules.warning.vo.WarningFlowVO;
import com.smartagri.modules.warning.vo.WarningListVO;

public interface WarningService extends IService<WarningRecord> {

    IPage<WarningListVO> pageWarnings(WarningPageQueryRequest request);

    WarningDetailVO getWarningDetail(Long id);

    void updateWarningStatus(WarningStatusUpdateRequest request);

    void confirmWarning(Long id, WarningFlowActionRequest request);

    void markProcessing(Long id, WarningFlowActionRequest request);

    void submitHandleResult(Long id, WarningHandleSubmitRequest request);

    void closeWarning(Long id, WarningFlowActionRequest request);

    void markFalseAlarm(Long id, WarningFalseAlarmRequest request);

    WarningFlowVO getWarningFlow(Long id);

    WarningDisposalReportVO generateDisposalReport(Long id);

    void generateWarningsForEnvironmentData(EnvironmentData environmentData);

    void generateDeviceOfflineWarning(Long deviceId);
}
