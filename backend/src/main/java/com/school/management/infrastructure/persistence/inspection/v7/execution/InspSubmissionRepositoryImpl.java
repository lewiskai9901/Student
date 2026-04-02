package com.school.management.infrastructure.persistence.inspection.v7.execution;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.inspection.model.v7.execution.*;
import com.school.management.domain.inspection.repository.v7.InspSubmissionRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class InspSubmissionRepositoryImpl implements InspSubmissionRepository {

    private final InspSubmissionMapper mapper;

    public InspSubmissionRepositoryImpl(InspSubmissionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public InspSubmission save(InspSubmission submission) {
        InspSubmissionPO po = toPO(submission);
        if (submission.getId() == null) {
            mapper.insert(po);
            submission.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return submission;
    }

    @Override
    public List<InspSubmission> saveAll(List<InspSubmission> submissions) {
        if (submissions == null || submissions.isEmpty()) {
            return submissions;
        }
        // Use individual inserts so that the DB-generated ID is populated back into each PO.
        // All execute within the same transaction/connection (no N+1 connection overhead).
        for (InspSubmission submission : submissions) {
            InspSubmissionPO po = toPO(submission);
            mapper.insert(po);
            submission.setId(po.getId());
        }
        return submissions;
    }

    @Override
    public Optional<InspSubmission> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<InspSubmission> findByTaskId(Long taskId) {
        return mapper.findByTaskId(taskId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<InspSubmission> findByTargetId(Long targetId) {
        return mapper.findByTargetId(targetId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<InspSubmission> findModifiedAfter(Long taskId, LocalDateTime since) {
        return mapper.findModifiedAfter(taskId, since).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public void deleteByTaskId(Long taskId) {
        mapper.delete(new LambdaQueryWrapper<InspSubmissionPO>().eq(InspSubmissionPO::getTaskId, taskId));
    }

    private InspSubmissionPO toPO(InspSubmission d) {
        InspSubmissionPO po = new InspSubmissionPO();
        po.setId(d.getId());
        po.setTenantId(d.getTenantId() != null ? d.getTenantId() : 0L);
        po.setTaskId(d.getTaskId());
        po.setSectionId(d.getSectionId());
        po.setTargetType(d.getTargetType() != null ? d.getTargetType().name() : null);
        po.setTargetId(d.getTargetId());
        po.setTargetName(d.getTargetName());
        po.setRootTargetId(d.getRootTargetId());
        po.setRootTargetName(d.getRootTargetName());
        po.setOrgUnitId(d.getOrgUnitId());
        po.setOrgUnitName(d.getOrgUnitName());
        po.setWeightRatio(d.getWeightRatio());
        po.setStatus(d.getStatus() != null ? d.getStatus().name() : null);
        po.setFormData(d.getFormData());
        po.setScoreBreakdown(d.getScoreBreakdown());
        po.setBaseScore(d.getBaseScore());
        po.setFinalScore(d.getFinalScore());
        po.setDeductionTotal(d.getDeductionTotal());
        po.setBonusTotal(d.getBonusTotal());
        po.setGrade(d.getGrade());
        po.setPassed(d.getPassed());
        po.setTotalTimeSeconds(d.getTotalTimeSeconds());
        po.setNfcTagUid(d.getNfcTagUid());
        po.setCheckinTime(d.getCheckinTime());
        po.setSyncVersion(d.getSyncVersion());
        po.setCompletedAt(d.getCompletedAt());
        po.setClosedAt(d.getClosedAt());
        po.setClosedReason(d.getClosedReason());
        po.setCreatedAt(d.getCreatedAt());
        po.setUpdatedAt(d.getUpdatedAt());
        return po;
    }

    private InspSubmission toDomain(InspSubmissionPO po) {
        return InspSubmission.reconstruct(InspSubmission.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .taskId(po.getTaskId())
                .sectionId(po.getSectionId())
                .targetType(po.getTargetType() != null ? TargetType.valueOf(po.getTargetType()) : null)
                .targetId(po.getTargetId())
                .targetName(po.getTargetName())
                .rootTargetId(po.getRootTargetId())
                .rootTargetName(po.getRootTargetName())
                .orgUnitId(po.getOrgUnitId())
                .orgUnitName(po.getOrgUnitName())
                .weightRatio(po.getWeightRatio())
                .status(po.getStatus() != null ? SubmissionStatus.valueOf(po.getStatus()) : null)
                .formData(po.getFormData())
                .scoreBreakdown(po.getScoreBreakdown())
                .baseScore(po.getBaseScore())
                .finalScore(po.getFinalScore())
                .deductionTotal(po.getDeductionTotal())
                .bonusTotal(po.getBonusTotal())
                .grade(po.getGrade())
                .passed(po.getPassed())
                .totalTimeSeconds(po.getTotalTimeSeconds())
                .nfcTagUid(po.getNfcTagUid())
                .checkinTime(po.getCheckinTime())
                .syncVersion(po.getSyncVersion())
                .completedAt(po.getCompletedAt())
                .closedAt(po.getClosedAt())
                .closedReason(po.getClosedReason())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt()));
    }
}
