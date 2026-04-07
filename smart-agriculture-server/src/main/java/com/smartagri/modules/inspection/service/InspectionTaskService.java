package com.smartagri.modules.inspection.service;

import com.smartagri.modules.inspection.vo.InspectionTaskSummaryVO;

public interface InspectionTaskService {

    /**
     * 定时巡检入口（被调度器调用）
     */
    void executeScheduledInspection();

    /**
     * 手动触发一次巡检（便于本地验证和比赛演示）
     */
    void executeManualInspection(String operatorName);

    /**
     * 获取巡检摘要信息（用于前端展示）
     */
    InspectionTaskSummaryVO getInspectionSummary(int recentLimit);
}

