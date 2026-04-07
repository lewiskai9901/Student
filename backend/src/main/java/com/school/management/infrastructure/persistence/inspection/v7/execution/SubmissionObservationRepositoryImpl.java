package com.school.management.infrastructure.persistence.inspection.v7.execution;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.inspection.model.v7.execution.ScoringObservation;
import com.school.management.domain.inspection.repository.v7.SubmissionObservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class SubmissionObservationRepositoryImpl implements SubmissionObservationRepository {

    private final SubmissionObservationMapper mapper;

    @Override
    public void batchInsert(List<ScoringObservation> observations) {
        for (ScoringObservation obs : observations) {
            mapper.insert(toPO(obs));
        }
    }

    @Override
    public List<ScoringObservation> findBySubmissionId(Long submissionId) {
        return mapper.findBySubmissionId(submissionId).stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<ScoringObservation> findNegativeBySubmissionId(Long submissionId) {
        return mapper.findNegativeBySubmissionId(submissionId).stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteBySubmissionId(Long submissionId) {
        mapper.delete(new LambdaQueryWrapper<SubmissionObservationPO>()
                .eq(SubmissionObservationPO::getSubmissionId, submissionId));
    }

    private SubmissionObservationPO toPO(ScoringObservation obs) {
        SubmissionObservationPO po = new SubmissionObservationPO();
        po.setTenantId(1L);
        po.setSubmissionId(obs.getSubmissionId());
        po.setDetailId(obs.getDetailId());
        po.setProjectId(obs.getProjectId());
        po.setTaskId(obs.getTaskId());
        po.setItemCode(obs.getItemCode());
        po.setItemName(obs.getItemName());
        po.setItemType(obs.getItemType());
        po.setSectionName(obs.getSectionName());
        po.setSubjectType(obs.getSubjectType());
        po.setSubjectId(obs.getSubjectId());
        po.setSubjectName(obs.getSubjectName());
        po.setOrgUnitId(obs.getOrgUnitId());
        po.setClassName(obs.getClassName());
        po.setScore(obs.getScore() != null ? obs.getScore() : BigDecimal.ZERO);
        po.setIsNegative(obs.isNegative() ? 1 : 0);
        po.setSeverity(obs.getSeverity());
        po.setIsFlagged(obs.isFlagged() ? 1 : 0);
        po.setLinkedEventTypeCode(obs.getLinkedEventType());
        po.setResponseValue(obs.getResponseValue());
        po.setDescription(obs.getDescription());
        po.setObservedAt(obs.getObservedAt() != null ? obs.getObservedAt() : LocalDateTime.now());
        po.setCreatedAt(LocalDateTime.now());
        return po;
    }

    private ScoringObservation toDomain(SubmissionObservationPO po) {
        return ScoringObservation.builder()
                .submissionId(po.getSubmissionId())
                .detailId(po.getDetailId())
                .projectId(po.getProjectId())
                .taskId(po.getTaskId())
                .itemCode(po.getItemCode())
                .itemName(po.getItemName())
                .itemType(po.getItemType())
                .sectionName(po.getSectionName())
                .subjectType(po.getSubjectType())
                .subjectId(po.getSubjectId())
                .subjectName(po.getSubjectName())
                .orgUnitId(po.getOrgUnitId())
                .className(po.getClassName())
                .score(po.getScore())
                .negative(po.getIsNegative() != null && po.getIsNegative() == 1)
                .severity(po.getSeverity())
                .flagged(po.getIsFlagged() != null && po.getIsFlagged() == 1)
                .linkedEventType(po.getLinkedEventTypeCode())
                .responseValue(po.getResponseValue())
                .description(po.getDescription())
                .observedAt(po.getObservedAt())
                .build();
    }
}
