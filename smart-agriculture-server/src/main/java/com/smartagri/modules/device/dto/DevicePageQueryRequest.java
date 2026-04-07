package com.smartagri.modules.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "设备分页查询参数")
public class DevicePageQueryRequest {

    @Min(value = 1, message = "页码不能小于1")
    @Schema(description = "当前页码", example = "1")
    private long current = 1;

    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 100, message = "每页条数不能超过100")
    @Schema(description = "每页条数", example = "10")
    private long size = 10;

    @Size(max = 50, message = "设备类型长度不能超过50个字符")
    @Schema(description = "设备类型", example = "温湿度传感器")
    private String deviceType;

    @Schema(description = "所属农田ID", example = "1")
    private Long farmlandId;

    @Min(value = 0, message = "在线状态取值不合法")
    @Max(value = 2, message = "在线状态取值不合法")
    @Schema(description = "在线状态：0离线，1在线，2故障", example = "1")
    private Integer onlineStatus;
}
