package com.smartagri.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "smart-agri.inspection")
public class InspectionTaskProperties {

    /**
     * 是否启用定时巡检
     */
    private boolean enabled = true;

    /**
     * 首次延迟执行毫秒
     */
    private long initialDelayMs = 15000L;

    /**
     * 固定间隔毫秒
     */
    private long fixedDelayMs = 60000L;

    /**
     * 环境数据回看分钟数（拉取该时间窗内最新数据）
     */
    private int envLookbackMinutes = 10;

    /**
     * 单次巡检环境数据上限
     */
    private int envBatchSize = 200;

    /**
     * 单次巡检设备数量上限
     */
    private int deviceBatchSize = 200;
}

