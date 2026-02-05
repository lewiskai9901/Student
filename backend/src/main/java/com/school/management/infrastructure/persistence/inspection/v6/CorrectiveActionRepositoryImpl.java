package com.school.management.infrastructure.persistence.inspection.v6;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.inspection.model.v6.CorrectiveAction;
import com.school.management.domain.inspection.model.v6.CorrectiveActionStatus;
import com.school.management.domain.inspection.repository.v6.CorrectiveActionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * V6整改记录仓储实现
 */
@Repository
@RequiredArgsConstructor
public class CorrectiveActionRepositoryImpl implements CorrectiveActionRepository {

    private final CorrectiveActionMapper correctiveActionMapper;

    @Override
    public CorrectiveAction save(CorrectiveAction action) {
        CorrectiveActionPO po = toPO(action);
        if (po.getId() == null) {
            correctiveActionMapper.insert(po);
        } else {
            correctiveActionMapper.updateById(po);
        }
        action.setId(po.getId());
        return action;
    }

    @Override
    public Optional<CorrectiveAction> findById(Long id) {
        return Optional.ofNullable(correctiveActionMapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public Optional<CorrectiveAction> findByActionCode(String actionCode) {
        LambdaQueryWrapper<CorrectiveActionPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CorrectiveActionPO::getActionCode, actionCode);
        return Optional.ofNullable(correctiveActionMapper.selectOne(wrapper)).map(this::toDomain);
    }

    @Override
    public List<CorrectiveAction> findByDetailId(Long detailId) {
        LambdaQueryWrapper<CorrectiveActionPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CorrectiveActionPO::getDetailId, detailId)
               .orderByDesc(CorrectiveActionPO::getCreatedAt);
        return correctiveActionMapper.selectList(wrapper).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<CorrectiveAction> findByTargetId(Long targetId) {
        LambdaQueryWrapper<CorrectiveActionPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CorrectiveActionPO::getTargetId, targetId)
               .orderByDesc(CorrectiveActionPO::getCreatedAt);
        return correctiveActionMapper.selectList(wrapper).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<CorrectiveAction> findByTaskId(Long taskId) {
        LambdaQueryWrapper<CorrectiveActionPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CorrectiveActionPO::getTaskId, taskId)
               .orderByDesc(CorrectiveActionPO::getCreatedAt);
        return correctiveActionMapper.selectList(wrapper).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<CorrectiveAction> findByProjectId(Long projectId) {
        LambdaQueryWrapper<CorrectiveActionPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CorrectiveActionPO::getProjectId, projectId)
               .orderByDesc(CorrectiveActionPO::getCreatedAt);
        return correctiveActionMapper.selectList(wrapper).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<CorrectiveAction> findByAssigneeId(Long assigneeId) {
        LambdaQueryWrapper<CorrectiveActionPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CorrectiveActionPO::getAssigneeId, assigneeId)
               .orderByDesc(CorrectiveActionPO::getCreatedAt);
        return correctiveActionMapper.selectList(wrapper).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<CorrectiveAction> findByStatus(CorrectiveActionStatus status) {
        LambdaQueryWrapper<CorrectiveActionPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CorrectiveActionPO::getStatus, status.name())
               .orderByDesc(CorrectiveActionPO::getCreatedAt);
        return correctiveActionMapper.selectList(wrapper).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<CorrectiveAction> findOverdue() {
        LambdaQueryWrapper<CorrectiveActionPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(CorrectiveActionPO::getStatus, CorrectiveActionStatus.PENDING.name(), CorrectiveActionStatus.REJECTED.name())
               .lt(CorrectiveActionPO::getDeadline, LocalDate.now())
               .orderByAsc(CorrectiveActionPO::getDeadline);
        return correctiveActionMapper.selectList(wrapper).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<CorrectiveAction> findByProjectIdAndStatus(Long projectId, CorrectiveActionStatus status) {
        LambdaQueryWrapper<CorrectiveActionPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CorrectiveActionPO::getProjectId, projectId)
               .eq(CorrectiveActionPO::getStatus, status.name())
               .orderByDesc(CorrectiveActionPO::getCreatedAt);
        return correctiveActionMapper.selectList(wrapper).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        correctiveActionMapper.deleteById(id);
    }

    @Override
    public long countByProjectIdAndStatus(Long projectId, CorrectiveActionStatus status) {
        LambdaQueryWrapper<CorrectiveActionPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CorrectiveActionPO::getProjectId, projectId)
               .eq(CorrectiveActionPO::getStatus, status.name());
        return correctiveActionMapper.selectCount(wrapper);
    }

    private CorrectiveActionPO toPO(CorrectiveAction entity) {
        CorrectiveActionPO po = new CorrectiveActionPO();
        po.setId(entity.getId());
        po.setDetailId(entity.getDetailId());
        po.setTargetId(entity.getTargetId());
        po.setTaskId(entity.getTaskId());
        po.setProjectId(entity.getProjectId());
        po.setActionCode(entity.getActionCode());
        po.setIssueDescription(entity.getIssueDescription());
        po.setRequiredAction(entity.getRequiredAction());
        po.setDeadline(entity.getDeadline());
        po.setAssigneeId(entity.getAssigneeId());
        po.setAssigneeName(entity.getAssigneeName());
        po.setStatus(entity.getStatus() != null ? entity.getStatus().name() : null);
        po.setCorrectionNote(entity.getCorrectionNote());
        po.setEvidenceIds(entity.getEvidenceIds());
        po.setCorrectedAt(entity.getCorrectedAt());
        po.setVerifierId(entity.getVerifierId());
        po.setVerifierName(entity.getVerifierName());
        po.setVerifiedAt(entity.getVerifiedAt());
        po.setVerificationNote(entity.getVerificationNote());
        po.setCreatedBy(entity.getCreatedBy());
        po.setCreatedAt(entity.getCreatedAt());
        po.setUpdatedAt(entity.getUpdatedAt());
        return po;
    }

    private CorrectiveAction toDomain(CorrectiveActionPO po) {
        return CorrectiveAction.builder()
                .id(po.getId())
                .detailId(po.getDetailId())
                .targetId(po.getTargetId())
                .taskId(po.getTaskId())
                .projectId(po.getProjectId())
                .actionCode(po.getActionCode())
                .issueDescription(po.getIssueDescription())
                .requiredAction(po.getRequiredAction())
                .deadline(po.getDeadline())
                .assigneeId(po.getAssigneeId())
                .assigneeName(po.getAssigneeName())
                .status(po.getStatus() != null ? CorrectiveActionStatus.valueOf(po.getStatus()) : null)
                .correctionNote(po.getCorrectionNote())
                .evidenceIds(po.getEvidenceIds())
                .correctedAt(po.getCorrectedAt())
                .verifierId(po.getVerifierId())
                .verifierName(po.getVerifierName())
                .verifiedAt(po.getVerifiedAt())
                .verificationNote(po.getVerificationNote())
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }
}
