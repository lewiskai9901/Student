package com.school.management.application.asset.command;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 完成维修保养命令
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteMaintenanceCommand {

    @NotNull(message = "维修记录ID不能为空")
    private Long maintenanceId;

    private String result;

    private BigDecimal cost;

    private String maintainer;

    // 操作人
    @NotNull(message = "操作人ID不能为空")
    private Long operatorId;

    private String operatorName;
}
