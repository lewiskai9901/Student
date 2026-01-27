package com.school.management.domain.asset.repository;

import com.school.management.domain.asset.model.aggregate.AssetApproval;
import com.school.management.domain.asset.model.valueobject.ApprovalStatus;
import com.school.management.domain.asset.model.valueobject.ApprovalType;

import java.util.List;
import java.util.Optional;

/**
 * 资产审批仓储接口
 */
public interface AssetApprovalRepository {

    /**
     * 保存审批
     */
    void save(AssetApproval approval);

    /**
     * 根据ID查找
     */
    Optional<AssetApproval> findById(Long id);

    /**
     * 根据审批单号查找
     */
    Optional<AssetApproval> findByApprovalNo(String approvalNo);

    /**
     * 根据业务ID和类型查找
     */
    Optional<AssetApproval> findByBusinessIdAndType(Long businessId, ApprovalType type);

    /**
     * 查找待审批列表（审批人视角）
     */
    List<AssetApproval> findPendingByApproverId(Long approverId);

    /**
     * 查找我的申请列表
     */
    List<AssetApproval> findByApplicantId(Long applicantId);

    /**
     * 按状态统计
     */
    int countByStatus(ApprovalStatus status);

    /**
     * 按申请人和状态统计
     */
    int countByApplicantIdAndStatus(Long applicantId, ApprovalStatus status);

    /**
     * 查找已过期的待审批记录
     */
    List<AssetApproval> findExpiredPending();

    /**
     * 分页查询
     */
    List<AssetApproval> findByCondition(
            ApprovalType type,
            ApprovalStatus status,
            Long applicantId,
            Long approverId,
            int offset,
            int limit
    );

    /**
     * 条件统计
     */
    int countByCondition(
            ApprovalType type,
            ApprovalStatus status,
            Long applicantId,
            Long approverId
    );
}
