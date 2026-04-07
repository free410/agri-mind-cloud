package com.smartagri.modules.device.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "设备列表返回对象")
public class DeviceListVO {

    @Schema(description = "设备ID")
    private Long id;

    @Schema(description = "设备编号")
    private String deviceCode;

    @Schema(description = "设备名称")
    private String deviceName;

    @Schema(description = "设备类型")
    private String deviceType;

    @Schema(description = "所属农田ID")
    private Long farmlandId;

    @Schema(description = "所属农田名称")
    private String farmlandName;

    @Schema(description = "安装时间")
    private LocalDateTime installTime;

    @Schema(description = "在线状态")
    private Integer onlineStatus;

    @Schema(description = "在线状态名称")
    private String onlineStatusName;

    @Schema(description = "运行状态")
    private Integer runStatus;

    @Schema(description = "运行状态名称")
    private String runStatusName;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
