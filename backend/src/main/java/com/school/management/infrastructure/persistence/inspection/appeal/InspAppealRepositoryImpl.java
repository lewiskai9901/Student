package com.school.management.infrastructure.persistence.inspection.appeal;

import com.school.management.domain.inspection.model.appeal.AppealStatus;
import com.school.management.domain.inspection.model.appeal.InspAppeal;
import com.school.management.domain.inspection.repository.InspAppealRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class InspAppealRepositoryImpl implements InspAppealRepository {

    private final InspAppealMapper mapper;

    public InspAppealRepositoryImpl(InspAppealMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public InspAppeal save(InspAppeal appeal) {
        InspAppealPO po = toPO(appeal);
        if (po.getId() == null) {
            mapper.insert(po);
            appeal.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return appeal;
    }

    @Override
    public Optional<InspAppeal> findById(Long id) {
        InspAppealPO po = mapper.selectById(id);
        return po != null && (po.getDeleted() == null || po.getDeleted() == 0)
                ? Optional.of(toDomain(po)) : Optional.empty();
    }

    @Override
    public Optional<InspAppeal> findByAppealCode(String appealCode) {
        InspAppealPO po = mapper.findByAppealCode(appealCode);
        return po != null ? Optional.of(toDomain(po)) : Optional.empty();
    }

    @Override
    public List<InspAppeal> findBySubmitterUserId(Long submitterUserId) {
        return mapper.findBySubmitterUserId(submitterUserId).stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<InspAppeal> findByStatus(AppealStatus status) {
        return mapper.findByStatus(status.name()).stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<InspAppeal> findBySubmissionDetailId(Long submissionDetailId) {
        return mapper.findBySubmissionDetailId(submissionDetailId).stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<InspAppeal> findByProjectId(Long projectId) {
        return mapper.findByProjectId(projectId).stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    private InspAppealPO toPO(InspAppeal d) {
        InspAppealPO po = new InspAppealPO();
        po.setId(d.getId());
        po.setTenantId(d.getTenantId() != null ? d.getTenantId() : 0L);
        po.setOrgUnitId(d.getOrgUnitId());
        po.setAppealCode(d.getAppealCode());
        po.setSubmissionDetailId(d.getSubmissionDetailId());
        po.setSubmissionId(d.getSubmissionId());
        po.setTaskId(d.getTaskId());
        po.setProjectId(d.getProjectId());
        po.setSubjectType(d.getSubjectType());
        po.setSubjectId(d.getSubjectId());
        po.setSubmitterUserId(d.getSubmitterUserId());
        po.setSubmitterName(d.getSubmitterName());
        po.setReason(d.getReason());
        po.setAttachments(d.getAttachments());
        po.setExpectedAdjustment(d.getExpectedAdjustment());
        po.setFinalAdjustment(d.getFinalAdjustment());
        po.setStatus(d.getStatus() != null ? d.getStatus().name() : null);
        po.setReviewerId(d.getReviewerId());
        po.setReviewerName(d.getReviewerName());
        po.setReviewerComment(d.getReviewerComment());
        po.setReviewedAt(d.getReviewedAt());
        po.setCreatedAt(d.getCreatedAt());
        po.setUpdatedAt(d.getUpdatedAt());
        return po;
    }

    private InspAppeal toDomain(InspAppealPO po) {
        return InspAppeal.reconstruct(InspAppeal.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .orgUnitId(po.getOrgUnitId())
                .appealCode(po.getAppealCode())
                .submissionDetailId(po.getSubmissionDetailId())
                .submissionId(po.getSubmissionId())
                .taskId(po.getTaskId())
                .projectId(po.getProjectId())
                .subjectType(po.getSubjectType())
                .subjectId(po.getSubjectId())
                .submitterUserId(po.getSubmitterUserId())
                .submitterName(po.getSubmitterName())
                .reason(po.getReason())
                .attachments(po.getAttachments())
                .expectedAdjustment(po.getExpectedAdjustment())
                .finalAdjustment(po.getFinalAdjustment())
                .status(po.getStatus() != null ? AppealStatus.valueOf(po.getStatus()) : null)
                .reviewerId(po.getReviewerId())
                .reviewerName(po.getReviewerName())
                .reviewerComment(po.getReviewerComment())
                .reviewedAt(po.getReviewedAt())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt()));
    }
}
