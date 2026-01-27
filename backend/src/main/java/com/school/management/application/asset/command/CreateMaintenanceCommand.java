package com.school.management.application.asset.command;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 创建维修保养记录命令
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMaintenanceCommand {

    @NotNull(message = "资产ID不能为空")
    private Long assetId;

    @NotNull(message = "维修类型不能为空")
    private Integer maintenanceType;  // 1-维修 2-保养

    private String faultDesc;

    private String maintainer;

    private BigDecimal estimatedCost;

    // 操作人
    @NotNull(message = "操作人ID不能为空")
    private Long operatorId;

    private String operatorName;
}
