package com.smartagri.modules.warning.rule;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Data
@Component
@ConfigurationProperties(prefix = "smart-agri.warning.rule")
public class WarningRuleProperties {

    /**
     * 土壤湿度过低阈值（%）
     */
    private BigDecimal soilMoistureLowThreshold = new BigDecimal("30.00");

    /**
     * 温度过高阈值（℃）
     */
    private BigDecimal temperatureHighThreshold = new BigDecimal("32.00");

    /**
     * 温度过低阈值（℃）
     */
    private BigDecimal temperatureLowThreshold = new BigDecimal("10.00");

    /**
     * 光照过低阈值（lux）
     */
    private BigDecimal lightLowThreshold = new BigDecimal("5000.00");

    /**
     * 光照过高阈值（lux）
     */
    private BigDecimal lightHighThreshold = new BigDecimal("50000.00");
}
