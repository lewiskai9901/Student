package com.school.management.infrastructure.persistence.asset;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 资产审批持久化对象
 */
@Data
@TableName("asset_approval")
public class AssetApprovalPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String approvalNo;

    private Integer approvalType;

    private Long businessId;

    private Long assetId;

    private String assetName;

    private Long applicantId;

    private String applicantName;

    private String applicantDept;

    private Long approverId;

    private String approverName;

    private Integer status;

    private String applyReason;

    private Integer applyQuantity;

    private BigDecimal applyAmount;

    private String approvalRemark;

    private LocalDateTime applyTime;

    private LocalDateTime approvalTime;

    private LocalDateTime expireTime;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
