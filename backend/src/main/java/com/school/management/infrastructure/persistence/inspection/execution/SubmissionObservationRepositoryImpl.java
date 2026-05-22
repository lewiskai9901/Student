package com.school.management.infrastructure.persistence.inspection.execution;

import com.baomidou.mybatisplus.core.batch.MybatisBatch;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.inspection.model.execution.ScoringObservation;
import com.school.management.domain.inspection.repository.SubmissionObservationRepository;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class SubmissionObservationRepositoryImpl implements SubmissionObservationRepository {

    private final SubmissionObservationMapper mapper;
    private final SqlSessionFactory sqlSessionFactory;

    public SubmissionObservationRepositoryImpl(SubmissionObservationMapper mapper,
                                               SqlSessionFactory sqlSessionFactory) {
        this.mapper = mapper;
        this.sqlSessionFactory = sqlSessionFactory;
    }

    @Override
    public void batchInsert(List<ScoringObservation> observations) {
        if (observations == null || observations.isEmpty()) {
            return;
        }
        List<SubmissionObservationPO> poList =
                observations.stream().map(this::toPO).collect(Collectors.toList());
        // Observations don't need generated IDs returned — use MybatisBatch for true JDBC batching.
        MybatisBatch<SubmissionObservationPO> mybatisBatch = new MybatisBatch<>(sqlSessionFactory, poList);
        MybatisBatch.Method<SubmissionObservationPO> method =
                new MybatisBatch.Method<>(SubmissionObservationMapper.class);
        mybatisBatch.execute(method.insert());
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

    @Override
    public long countNegativeForSubjectInPeriod(String subjectType, Long subjectId,
                                                 String itemCode, int sinceUtcDays) {
        if (subjectType == null || subjectId == null || itemCode == null) return 0L;
        LocalDateTime since = LocalDateTime.now().minusDays(sinceUtcDays);
        Long count = mapper.selectCount(new LambdaQueryWrapper<SubmissionObservationPO>()
                .eq(SubmissionObservationPO::getSubjectType, subjectType)
                .eq(SubmissionObservationPO::getSubjectId, subjectId)
                .eq(SubmissionObservationPO::getItemCode, itemCode)
                .eq(SubmissionObservationPO::getIsNegative, 1)
                .ge(SubmissionObservationPO::getObservedAt, since));
        return count == null ? 0L : count;
    }

    private SubmissionObservationPO toPO(ScoringObservation obs) {
        SubmissionObservationPO po = new SubmissionObservationPO();
        // tenant 默认 0L (单租户), 与其余 RepositoryImpl 一致 —
        // ScoringObservation 领域对象无 tenantId 字段, 由 tenant 拦截器休眠期兜底
        po.setTenantId(0L);
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
        po.setOrgUnitName(obs.getOrgUnitName());
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
                .orgUnitName(po.getOrgUnitName())
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
