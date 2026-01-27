package com.school.management.interfaces.rest.asset;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建审批请求
 */
@Data
public class CreateApprovalRequest {

    @NotNull(message = "审批类型不能为空")
    private Integer approvalType;

    private Long businessId;

    private Long assetId;

    private String assetName;

    private String applyReason;

    private Integer applyQuantity;

    private BigDecimal applyAmount;
}
