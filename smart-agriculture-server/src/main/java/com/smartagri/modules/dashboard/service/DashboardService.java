package com.smartagri.modules.dashboard.service;

import com.smartagri.modules.dashboard.vo.DashboardChartVO;
import com.smartagri.modules.dashboard.vo.DashboardHomeVO;
import com.smartagri.modules.dashboard.vo.DashboardNameValueVO;
import com.smartagri.modules.dashboard.vo.DashboardSummaryVO;
import com.smartagri.modules.dashboard.vo.DashboardWarningQueueVO;
import com.smartagri.modules.inspection.vo.InspectionTaskSummaryVO;

import java.util.List;

public interface DashboardService {

    DashboardHomeVO getHomeDashboard();

    DashboardSummaryVO getSummary();

    List<DashboardNameValueVO> getFarmlandStatusStats();

    List<DashboardNameValueVO> getCropGrowthStageStats();

    DashboardChartVO getEnvironmentTrend();

    List<DashboardNameValueVO> getWarningLevelStats();

    List<DashboardNameValueVO> getWarningStatusStats();

    List<DashboardWarningQueueVO> getHighRiskWarningQueue();

    List<String> getTodayAiSuggestionList();

    InspectionTaskSummaryVO getInspectionSummary();

    DashboardChartVO getClosureTrend();
}
