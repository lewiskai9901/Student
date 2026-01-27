package com.school.management.application.asset.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 资产审批DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetApprovalDTO {

    private Long id;

    private String approvalNo;

    private Integer approvalType;

    private String approvalTypeDesc;

    private Long businessId;

    private Long assetId;

    private String assetName;

    private Long applicantId;

    private String applicantName;

    private String applicantDept;

    private Long approverId;

    private String approverName;

    private Integer status;

    private String statusDesc;

    private String applyReason;

    private Integer applyQuantity;

    private BigDecimal applyAmount;

    private String approvalRemark;

    private LocalDateTime applyTime;

    private LocalDateTime approvalTime;

    private LocalDateTime expireTime;

    private LocalDateTime createdAt;
}
