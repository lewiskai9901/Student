package com.school.management.application.asset.command;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 创建审批命令
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateApprovalCommand {

    @NotNull(message = "审批类型不能为空")
    private Integer approvalType;

    private Long businessId;

    private Long assetId;

    private String assetName;

    @NotNull(message = "申请人ID不能为空")
    private Long applicantId;

    private String applicantName;

    private String applicantDept;

    private String applyReason;

    private Integer applyQuantity;

    private BigDecimal applyAmount;
}
