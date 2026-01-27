package com.school.management.interfaces.rest.asset.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建维修记录请求
 */
@Data
public class CreateMaintenanceRequest {

    @NotNull(message = "维修类型不能为空")
    private Integer maintenanceType;  // 1-维修 2-保养

    private String faultDesc;

    private String maintainer;
}
