package com.smartagri.modules.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@Schema(description = "新增设备请求参数")
public class DeviceCreateRequest {

    @NotBlank(message = "设备编号不能为空")
    @Size(max = 50, message = "设备编号长度不能超过50个字符")
    @Schema(description = "设备编号", example = "DEV-2026-001")
    private String deviceCode;

    @NotBlank(message = "设备名称不能为空")
    @Size(max = 100, message = "设备名称长度不能超过100个字符")
    @Schema(description = "设备名称", example = "温湿度传感器-01")
    private String deviceName;

    @NotBlank(message = "设备类型不能为空")
    @Size(max = 50, message = "设备类型长度不能超过50个字符")
    @Schema(description = "设备类型", example = "温湿度传感器")
    private String deviceType;

    @NotNull(message = "所属农田不能为空")
    @Schema(description = "所属农田ID", example = "1")
    private Long farmlandId;

    @NotNull(message = "安装时间不能为空")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "安装时间", example = "2026-03-31 10:00:00")
    private LocalDateTime installTime;

    @NotNull(message = "在线状态不能为空")
    @Min(value = 0, message = "在线状态取值不合法")
    @Max(value = 2, message = "在线状态取值不合法")
    @Schema(description = "在线状态：0离线，1在线，2故障", example = "1")
    private Integer onlineStatus;

    @NotNull(message = "运行状态不能为空")
    @Min(value = 0, message = "运行状态取值不合法")
    @Max(value = 3, message = "运行状态取值不合法")
    @Schema(description = "运行状态：0停用，1运行中，2待机中，3异常", example = "1")
    private Integer runStatus;

    @Size(max = 255, message = "备注长度不能超过255个字符")
    @Schema(description = "备注", example = "用于温室环境监测的模拟设备")
    private String remark;
}
