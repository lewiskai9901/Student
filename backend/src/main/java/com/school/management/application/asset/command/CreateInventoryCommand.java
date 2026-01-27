package com.school.management.application.asset.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 创建资产盘点命令
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInventoryCommand {

    @NotBlank(message = "盘点名称不能为空")
    private String inventoryName;

    private String scopeType;  // all/category/location

    private String scopeValue;

    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    @NotNull(message = "截止日期不能为空")
    private LocalDate endDate;

    // 操作人
    @NotNull(message = "操作人ID不能为空")
    private Long operatorId;

    private String operatorName;
}
