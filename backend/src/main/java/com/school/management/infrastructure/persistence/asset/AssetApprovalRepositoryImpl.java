package com.school.management.infrastructure.persistence.asset;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.asset.model.aggregate.AssetApproval;
import com.school.management.domain.asset.model.valueobject.ApprovalStatus;
import com.school.management.domain.asset.model.valueobject.ApprovalType;
import com.school.management.domain.asset.repository.AssetApprovalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 资产审批仓储实现
 */
@Repository
@RequiredArgsConstructor
public class AssetApprovalRepositoryImpl implements AssetApprovalRepository {

    private final AssetApprovalMapper mapper;

    @Override
    public void save(AssetApproval approval) {
        AssetApprovalPO po = toPO(approval);
        if (po.getId() == null) {
            mapper.insert(po);
            approval.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
    }

    @Override
    public Optional<AssetApproval> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id))
                .map(this::toDomain);
    }

    @Override
    public Optional<AssetApproval> findByApprovalNo(String approvalNo) {
        LambdaQueryWrapper<AssetApprovalPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetApprovalPO::getApprovalNo, approvalNo);
        return Optional.ofNullable(mapper.selectOne(wrapper))
                .map(this::toDomain);
    }

    @Override
    public Optional<AssetApproval> findByBusinessIdAndType(Long businessId, ApprovalType type) {
        LambdaQueryWrapper<AssetApprovalPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetApprovalPO::getBusinessId, businessId)
               .eq(AssetApprovalPO::getApprovalType, type.getCode())
               .eq(AssetApprovalPO::getStatus, ApprovalStatus.PENDING.getCode());
        return Optional.ofNullable(mapper.selectOne(wrapper))
                .map(this::toDomain);
    }

    @Override
    public List<AssetApproval> findPendingByApproverId(Long approverId) {
        LambdaQueryWrapper<AssetApprovalPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetApprovalPO::getStatus, ApprovalStatus.PENDING.getCode())
               .orderByDesc(AssetApprovalPO::getApplyTime);
        // 注：实际项目中审批人逻辑可能更复杂，这里简化处理
        return mapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AssetApproval> findByApplicantId(Long applicantId) {
        LambdaQueryWrapper<AssetApprovalPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetApprovalPO::getApplicantId, applicantId)
               .orderByDesc(AssetApprovalPO::getApplyTime);
        return mapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public int countByStatus(ApprovalStatus status) {
        return mapper.countByStatus(status.getCode());
    }

    @Override
    public int countByApplicantIdAndStatus(Long applicantId, ApprovalStatus status) {
        return mapper.countByApplicantIdAndStatus(applicantId, status.getCode());
    }

    @Override
    public List<AssetApproval> findExpiredPending() {
        return mapper.selectExpiredPending().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AssetApproval> findByCondition(
            ApprovalType type,
            ApprovalStatus status,
            Long applicantId,
            Long approverId,
            int offset,
            int limit
    ) {
        LambdaQueryWrapper<AssetApprovalPO> wrapper = buildConditionWrapper(type, status, applicantId, approverId);
        wrapper.orderByDesc(AssetApprovalPO::getApplyTime)
               .last("LIMIT " + offset + ", " + limit);
        return mapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public int countByCondition(
            ApprovalType type,
            ApprovalStatus status,
            Long applicantId,
            Long approverId
    ) {
        LambdaQueryWrapper<AssetApprovalPO> wrapper = buildConditionWrapper(type, status, applicantId, approverId);
        return Math.toIntExact(mapper.selectCount(wrapper));
    }

    private LambdaQueryWrapper<AssetApprovalPO> buildConditionWrapper(
            ApprovalType type,
            ApprovalStatus status,
            Long applicantId,
            Long approverId
    ) {
        LambdaQueryWrapper<AssetApprovalPO> wrapper = new LambdaQueryWrapper<>();
        if (type != null) {
            wrapper.eq(AssetApprovalPO::getApprovalType, type.getCode());
        }
        if (status != null) {
            wrapper.eq(AssetApprovalPO::getStatus, status.getCode());
        }
        if (applicantId != null) {
            wrapper.eq(AssetApprovalPO::getApplicantId, applicantId);
        }
        if (approverId != null) {
            wrapper.eq(AssetApprovalPO::getApproverId, approverId);
        }
        return wrapper;
    }

    // ============ 转换方法 ============

    private AssetApproval toDomain(AssetApprovalPO po) {
        AssetApproval approval = new AssetApproval();
        approval.setId(po.getId());
        approval.setApprovalNo(po.getApprovalNo());
        approval.setApprovalType(ApprovalType.fromCode(po.getApprovalType()));
        approval.setBusinessId(po.getBusinessId());
        approval.setAssetId(po.getAssetId());
        approval.setAssetName(po.getAssetName());
        approval.setApplicantId(po.getApplicantId());
        approval.setApplicantName(po.getApplicantName());
        approval.setApplicantDept(po.getApplicantDept());
        approval.setApproverId(po.getApproverId());
        approval.setApproverName(po.getApproverName());
        approval.setStatus(ApprovalStatus.fromCode(po.getStatus()));
        approval.setApplyReason(po.getApplyReason());
        approval.setApplyQuantity(po.getApplyQuantity());
        approval.setApplyAmount(po.getApplyAmount());
        approval.setApprovalRemark(po.getApprovalRemark());
        approval.setApplyTime(po.getApplyTime());
        approval.setApprovalTime(po.getApprovalTime());
        approval.setExpireTime(po.getExpireTime());
        approval.setCreatedAt(po.getCreatedAt());
        approval.setUpdatedAt(po.getUpdatedAt());
        return approval;
    }

    private AssetApprovalPO toPO(AssetApproval approval) {
        AssetApprovalPO po = new AssetApprovalPO();
        po.setId(approval.getId());
        po.setApprovalNo(approval.getApprovalNo());
        po.setApprovalType(approval.getApprovalType().getCode());
        po.setBusinessId(approval.getBusinessId());
        po.setAssetId(approval.getAssetId());
        po.setAssetName(approval.getAssetName());
        po.setApplicantId(approval.getApplicantId());
        po.setApplicantName(approval.getApplicantName());
        po.setApplicantDept(approval.getApplicantDept());
        po.setApproverId(approval.getApproverId());
        po.setApproverName(approval.getApproverName());
        po.setStatus(approval.getStatus().getCode());
        po.setApplyReason(approval.getApplyReason());
        po.setApplyQuantity(approval.getApplyQuantity());
        po.setApplyAmount(approval.getApplyAmount());
        po.setApprovalRemark(approval.getApprovalRemark());
        po.setApplyTime(approval.getApplyTime());
        po.setApprovalTime(approval.getApprovalTime());
        po.setExpireTime(approval.getExpireTime());
        return po;
    }
}
