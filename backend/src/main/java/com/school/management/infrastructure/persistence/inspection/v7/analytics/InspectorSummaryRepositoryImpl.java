package com.school.management.infrastructure.persistence.inspection.v7.analytics;

import com.school.management.domain.inspection.model.v7.analytics.InspectorSummary;
import com.school.management.domain.inspection.model.v7.analytics.PeriodType;
import com.school.management.domain.inspection.repository.v7.InspectorSummaryRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class InspectorSummaryRepositoryImpl implements InspectorSummaryRepository {

    private final InspectorSummaryMapper mapper;

    public InspectorSummaryRepositoryImpl(InspectorSummaryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public InspectorSummary save(InspectorSummary s) {
        InspectorSummaryPO po = toPO(s);
        if (s.getId() == null) {
            mapper.insert(po);
            s.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return s;
    }

    @Override
    public Optional<InspectorSummary> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<InspectorSummary> findByProjectAndPeriod(Long projectId, PeriodType periodType, LocalDate periodStart) {
        return mapper.findByProjectAndPeriod(projectId, periodType.name(), periodStart)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<InspectorSummary> findByInspectorAndPeriod(Long projectId, Long inspectorId,
                                                                PeriodType periodType, LocalDate periodStart) {
        return Optional.ofNullable(mapper.findByInspectorAndPeriod(projectId, inspectorId, periodType.name(), periodStart))
                .map(this::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private InspectorSummaryPO toPO(InspectorSummary d) {
        InspectorSummaryPO po = new InspectorSummaryPO();
        po.setId(d.getId());
        po.setTenantId(d.getTenantId());
        po.setProjectId(d.getProjectId());
        po.setInspectorId(d.getInspectorId());
        po.setInspectorName(d.getInspectorName());
        po.setPeriodType(d.getPeriodType() != null ? d.getPeriodType().name() : null);
        po.setPeriodStart(d.getPeriodStart());
        po.setPeriodEnd(d.getPeriodEnd());
        po.setTotalTasks(d.getTotalTasks());
        po.setCompletedTasks(d.getCompletedTasks());
        po.setCancelledTasks(d.getCancelledTasks());
        po.setExpiredTasks(d.getExpiredTasks());
        po.setAvgCompletionTimeMinutes(d.getAvgCompletionTimeMinutes());
        po.setAvgScore(d.getAvgScore());
        po.setTotalSubmissions(d.getTotalSubmissions());
        po.setFlaggedSubmissions(d.getFlaggedSubmissions());
        po.setComplianceRate(d.getComplianceRate());
        po.setCreatedAt(d.getCreatedAt());
        po.setUpdatedAt(d.getUpdatedAt());
        return po;
    }

    private InspectorSummary toDomain(InspectorSummaryPO po) {
        InspectorSummary d = new InspectorSummary();
        d.setId(po.getId());
        d.setTenantId(po.getTenantId());
        d.setProjectId(po.getProjectId());
        d.setInspectorId(po.getInspectorId());
        d.setInspectorName(po.getInspectorName());
        d.setPeriodType(po.getPeriodType() != null ? PeriodType.valueOf(po.getPeriodType()) : null);
        d.setPeriodStart(po.getPeriodStart());
        d.setPeriodEnd(po.getPeriodEnd());
        d.setTotalTasks(po.getTotalTasks());
        d.setCompletedTasks(po.getCompletedTasks());
        d.setCancelledTasks(po.getCancelledTasks());
        d.setExpiredTasks(po.getExpiredTasks());
        d.setAvgCompletionTimeMinutes(po.getAvgCompletionTimeMinutes());
        d.setAvgScore(po.getAvgScore());
        d.setTotalSubmissions(po.getTotalSubmissions());
        d.setFlaggedSubmissions(po.getFlaggedSubmissions());
        d.setComplianceRate(po.getComplianceRate());
        d.setCreatedAt(po.getCreatedAt());
        d.setUpdatedAt(po.getUpdatedAt());
        return d;
    }
}
