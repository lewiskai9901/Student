package com.school.management.infrastructure.persistence.inspection.v7.analytics;

import com.school.management.domain.inspection.model.v7.analytics.CorrectiveSummary;
import com.school.management.domain.inspection.model.v7.analytics.PeriodType;
import com.school.management.domain.inspection.repository.v7.CorrectiveSummaryRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class CorrectiveSummaryRepositoryImpl implements CorrectiveSummaryRepository {

    private final CorrectiveSummaryMapper mapper;

    public CorrectiveSummaryRepositoryImpl(CorrectiveSummaryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public CorrectiveSummary save(CorrectiveSummary s) {
        CorrectiveSummaryPO po = toPO(s);
        if (s.getId() == null) {
            mapper.insert(po);
            s.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return s;
    }

    @Override
    public Optional<CorrectiveSummary> findByProjectAndPeriod(Long projectId, PeriodType periodType, LocalDate periodStart) {
        return Optional.ofNullable(mapper.findByProjectAndPeriod(projectId, periodType.name(), periodStart))
                .map(this::toDomain);
    }

    @Override
    public List<CorrectiveSummary> findByProject(Long projectId) {
        return mapper.findByProject(projectId).stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private CorrectiveSummaryPO toPO(CorrectiveSummary d) {
        CorrectiveSummaryPO po = new CorrectiveSummaryPO();
        po.setId(d.getId());
        po.setTenantId(d.getTenantId());
        po.setProjectId(d.getProjectId());
        po.setPeriodType(d.getPeriodType() != null ? d.getPeriodType().name() : null);
        po.setPeriodStart(d.getPeriodStart());
        po.setPeriodEnd(d.getPeriodEnd());
        po.setTotalCases(d.getTotalCases());
        po.setOpenCases(d.getOpenCases());
        po.setInProgressCases(d.getInProgressCases());
        po.setClosedCases(d.getClosedCases());
        po.setOverdueCases(d.getOverdueCases());
        po.setEscalatedCases(d.getEscalatedCases());
        po.setAvgResolutionDays(d.getAvgResolutionDays());
        po.setOnTimeRate(d.getOnTimeRate());
        po.setEffectivenessConfirmed(d.getEffectivenessConfirmed());
        po.setEffectivenessFailed(d.getEffectivenessFailed());
        po.setCreatedAt(d.getCreatedAt());
        po.setUpdatedAt(d.getUpdatedAt());
        return po;
    }

    private CorrectiveSummary toDomain(CorrectiveSummaryPO po) {
        CorrectiveSummary d = new CorrectiveSummary();
        d.setId(po.getId());
        d.setTenantId(po.getTenantId());
        d.setProjectId(po.getProjectId());
        d.setPeriodType(po.getPeriodType() != null ? PeriodType.valueOf(po.getPeriodType()) : null);
        d.setPeriodStart(po.getPeriodStart());
        d.setPeriodEnd(po.getPeriodEnd());
        d.setTotalCases(po.getTotalCases());
        d.setOpenCases(po.getOpenCases());
        d.setInProgressCases(po.getInProgressCases());
        d.setClosedCases(po.getClosedCases());
        d.setOverdueCases(po.getOverdueCases());
        d.setEscalatedCases(po.getEscalatedCases());
        d.setAvgResolutionDays(po.getAvgResolutionDays());
        d.setOnTimeRate(po.getOnTimeRate());
        d.setEffectivenessConfirmed(po.getEffectivenessConfirmed());
        d.setEffectivenessFailed(po.getEffectivenessFailed());
        d.setCreatedAt(po.getCreatedAt());
        d.setUpdatedAt(po.getUpdatedAt());
        return d;
    }
}
