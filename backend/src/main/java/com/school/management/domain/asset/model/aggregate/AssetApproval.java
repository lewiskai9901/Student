package com.school.management.domain.asset.model.aggregate;

import com.school.management.domain.asset.model.valueobject.ApprovalStatus;
import com.school.management.domain.asset.model.valueobject.ApprovalType;
import com.school.management.domain.shared.AggregateRoot;
import com.school.management.exception.BusinessException;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 资产审批聚合根
 */
@Getter
@Setter
public class AssetApproval extends AggregateRoot<Long> {

    /** 审批单号 */
    private String approvalNo;

    /** 审批类型 */
    private ApprovalType approvalType;

    /** 关联业务ID（如借用记录ID、资产ID等） */
    private Long businessId;

    /** 关联资产ID */
    private Long assetId;

    /** 资产名称（冗余） */
    private String assetName;

    /** 申请人ID */
    private Long applicantId;

    /** 申请人姓名 */
    private String applicantName;

    /** 申请人部门 */
    private String applicantDept;

    /** 审批人ID */
    private Long approverId;

    /** 审批人姓名 */
    private String approverName;

    /** 审批状态 */
    private ApprovalStatus status;

    /** 申请原因 */
    private String applyReason;

    /** 申请数量（批量资产适用） */
    private Integer applyQuantity;

    /** 申请金额（采购适用） */
    private java.math.BigDecimal applyAmount;

    /** 审批意见 */
    private String approvalRemark;

    /** 申请时间 */
    private LocalDateTime applyTime;

    /** 审批时间 */
    private LocalDateTime approvalTime;

    /** 过期时间（可选，超时自动取消） */
    private LocalDateTime expireTime;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    public AssetApproval() {
    }

    /**
     * 创建审批申请
     */
    public static AssetApproval create(
            String approvalNo,
            ApprovalType approvalType,
            Long businessId,
            Long assetId,
            String assetName,
            Long applicantId,
            String applicantName,
            String applyReason
    ) {
        AssetApproval approval = new AssetApproval();
        approval.approvalNo = approvalNo;
        approval.approvalType = approvalType;
        approval.businessId = businessId;
        approval.assetId = assetId;
        approval.assetName = assetName;
        approval.applicantId = applicantId;
        approval.applicantName = applicantName;
        approval.applyReason = applyReason;
        approval.status = ApprovalStatus.PENDING;
        approval.applyTime = LocalDateTime.now();
        return approval;
    }

    /**
     * 审批通过
     */
    public void approve(Long approverId, String approverName, String remark) {
        if (!status.isPending()) {
            throw new BusinessException("只能审批待处理的申请");
        }
        this.approverId = approverId;
        this.approverName = approverName;
        this.approvalRemark = remark;
        this.status = ApprovalStatus.APPROVED;
        this.approvalTime = LocalDateTime.now();
    }

    /**
     * 审批拒绝
     */
    public void reject(Long approverId, String approverName, String remark) {
        if (!status.isPending()) {
            throw new BusinessException("只能审批待处理的申请");
        }
        this.approverId = approverId;
        this.approverName = approverName;
        this.approvalRemark = remark;
        this.status = ApprovalStatus.REJECTED;
        this.approvalTime = LocalDateTime.now();
    }

    /**
     * 取消申请
     */
    public void cancel(Long operatorId) {
        if (!status.isPending()) {
            throw new BusinessException("只能取消待处理的申请");
        }
        if (!applicantId.equals(operatorId)) {
            throw new BusinessException("只能取消自己的申请");
        }
        this.status = ApprovalStatus.CANCELLED;
        this.approvalTime = LocalDateTime.now();
    }

    /**
     * 检查是否已过期
     */
    public boolean isExpired() {
        return expireTime != null && LocalDateTime.now().isAfter(expireTime);
    }

    /**
     * 自动过期处理
     */
    public void autoExpire() {
        if (status.isPending() && isExpired()) {
            this.status = ApprovalStatus.CANCELLED;
            this.approvalRemark = "审批超时自动取消";
            this.approvalTime = LocalDateTime.now();
        }
    }
}
