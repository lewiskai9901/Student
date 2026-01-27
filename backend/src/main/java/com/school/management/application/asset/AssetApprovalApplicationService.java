package com.school.management.application.asset;

import com.school.management.application.asset.command.ApproveCommand;
import com.school.management.application.asset.command.CreateApprovalCommand;
import com.school.management.application.asset.query.AssetApprovalDTO;
import com.school.management.domain.asset.model.aggregate.AssetApproval;
import com.school.management.domain.asset.model.valueobject.ApprovalStatus;
import com.school.management.domain.asset.model.valueobject.ApprovalType;
import com.school.management.domain.asset.repository.AssetApprovalRepository;
import com.school.management.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 资产审批应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetApprovalApplicationService {

    private final AssetApprovalRepository approvalRepository;

    // 简单的序列号生成器（生产环境应使用分布式ID生成器）
    private static final AtomicLong SEQUENCE = new AtomicLong(System.currentTimeMillis() % 10000);

    /**
     * 创建审批申请
     */
    @Transactional
    public Long createApproval(CreateApprovalCommand command) {
        // 生成审批单号
        String approvalNo = generateApprovalNo(command.getApprovalType());

        ApprovalType approvalType = ApprovalType.fromCode(command.getApprovalType());

        AssetApproval approval = AssetApproval.create(
                approvalNo,
                approvalType,
                command.getBusinessId(),
                command.getAssetId(),
                command.getAssetName(),
                command.getApplicantId(),
                command.getApplicantName(),
                command.getApplyReason()
        );

        approval.setApplicantDept(command.getApplicantDept());
        approval.setApplyQuantity(command.getApplyQuantity());
        approval.setApplyAmount(command.getApplyAmount());

        // 设置过期时间（7天后）
        approval.setExpireTime(LocalDateTime.now().plusDays(7));

        approvalRepository.save(approval);

        log.info("Created approval: {} - {}", approvalNo, approvalType.getDescription());
        return approval.getId();
    }

    /**
     * 审批通过
     */
    @Transactional
    public void approve(ApproveCommand command) {
        AssetApproval approval = approvalRepository.findById(command.getApprovalId())
                .orElseThrow(() -> new BusinessException("审批记录不存在"));

        if (command.isApproved()) {
            approval.approve(command.getApproverId(), command.getApproverName(), command.getRemark());
            log.info("Approval {} approved by {}", approval.getApprovalNo(), command.getApproverName());
        } else {
            approval.reject(command.getApproverId(), command.getApproverName(), command.getRemark());
            log.info("Approval {} rejected by {}", approval.getApprovalNo(), command.getApproverName());
        }

        approvalRepository.save(approval);

        // TODO: 发布审批结果事件，触发后续业务处理
    }

    /**
     * 取消申请
     */
    @Transactional
    public void cancel(Long approvalId, Long operatorId) {
        AssetApproval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new BusinessException("审批记录不存在"));

        approval.cancel(operatorId);
        approvalRepository.save(approval);

        log.info("Approval {} cancelled", approval.getApprovalNo());
    }

    /**
     * 获取审批详情
     */
    public AssetApprovalDTO getApproval(Long id) {
        return approvalRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new BusinessException("审批记录不存在"));
    }

    /**
     * 获取我的申请列表
     */
    public List<AssetApprovalDTO> getMyApprovals(Long applicantId) {
        return approvalRepository.findByApplicantId(applicantId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取待审批列表
     */
    public List<AssetApprovalDTO> getPendingApprovals(Long approverId) {
        return approvalRepository.findPendingByApproverId(approverId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 分页查询
     */
    public List<AssetApprovalDTO> queryApprovals(
            Integer approvalType,
            Integer status,
            Long applicantId,
            Long approverId,
            int pageNum,
            int pageSize
    ) {
        ApprovalType type = approvalType != null ? ApprovalType.fromCode(approvalType) : null;
        ApprovalStatus approvalStatus = status != null ? ApprovalStatus.fromCode(status) : null;

        int offset = (pageNum - 1) * pageSize;
        return approvalRepository.findByCondition(type, approvalStatus, applicantId, approverId, offset, pageSize)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 统计数量
     */
    public int countApprovals(
            Integer approvalType,
            Integer status,
            Long applicantId,
            Long approverId
    ) {
        ApprovalType type = approvalType != null ? ApprovalType.fromCode(approvalType) : null;
        ApprovalStatus approvalStatus = status != null ? ApprovalStatus.fromCode(status) : null;

        return approvalRepository.countByCondition(type, approvalStatus, applicantId, approverId);
    }

    /**
     * 获取待审批数量
     */
    public int countPending() {
        return approvalRepository.countByStatus(ApprovalStatus.PENDING);
    }

    /**
     * 处理过期审批（定时任务调用）
     */
    @Transactional
    public void processExpiredApprovals() {
        List<AssetApproval> expiredList = approvalRepository.findExpiredPending();
        for (AssetApproval approval : expiredList) {
            approval.autoExpire();
            approvalRepository.save(approval);
            log.info("Approval {} auto expired", approval.getApprovalNo());
        }
    }

    // ============ 私有方法 ============

    private String generateApprovalNo(Integer approvalType) {
        String prefix;
        switch (approvalType) {
            case 1: prefix = "JY"; break;  // 借用
            case 2: prefix = "CG"; break;  // 采购
            case 3: prefix = "BF"; break;  // 报废
            case 4: prefix = "DB"; break;  // 调拨
            default: prefix = "SP";
        }
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long seq = SEQUENCE.incrementAndGet() % 10000;
        return String.format("%s%s%04d", prefix, date, seq);
    }

    private AssetApprovalDTO toDTO(AssetApproval approval) {
        return AssetApprovalDTO.builder()
                .id(approval.getId())
                .approvalNo(approval.getApprovalNo())
                .approvalType(approval.getApprovalType().getCode())
                .approvalTypeDesc(approval.getApprovalType().getDescription())
                .businessId(approval.getBusinessId())
                .assetId(approval.getAssetId())
                .assetName(approval.getAssetName())
                .applicantId(approval.getApplicantId())
                .applicantName(approval.getApplicantName())
                .applicantDept(approval.getApplicantDept())
                .approverId(approval.getApproverId())
                .approverName(approval.getApproverName())
                .status(approval.getStatus().getCode())
                .statusDesc(approval.getStatus().getDescription())
                .applyReason(approval.getApplyReason())
                .applyQuantity(approval.getApplyQuantity())
                .applyAmount(approval.getApplyAmount())
                .approvalRemark(approval.getApprovalRemark())
                .applyTime(approval.getApplyTime())
                .approvalTime(approval.getApprovalTime())
                .expireTime(approval.getExpireTime())
                .createdAt(approval.getCreatedAt())
                .build();
    }
}
