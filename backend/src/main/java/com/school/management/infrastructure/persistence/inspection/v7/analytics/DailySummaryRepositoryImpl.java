package com.school.management.infrastructure.persistence.inspection.v7.analytics;

import com.school.management.domain.inspection.model.v7.analytics.DailySummary;
import com.school.management.domain.inspection.repository.v7.DailySummaryRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class DailySummaryRepositoryImpl implements DailySummaryRepository {

    private final DailySummaryMapper mapper;

    public DailySummaryRepositoryImpl(DailySummaryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public DailySummary save(DailySummary s) {
        DailySummaryPO po = toPO(s);
        if (s.getId() == null) {
            mapper.insert(po);
            s.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return s;
    }

    @Override
    public Optional<DailySummary> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public Optional<DailySummary> findByProjectDateTarget(Long projectId, LocalDate summaryDate,
                                                            String targetType, Long targetId) {
        return Optional.ofNullable(mapper.findByProjectDateTarget(projectId, summaryDate, targetType, targetId))
                .map(this::toDomain);
    }

    @Override
    public List<DailySummary> findByProjectAndDate(Long projectId, LocalDate summaryDate) {
        return mapper.findByProjectAndDate(projectId, summaryDate).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<DailySummary> findByProjectAndDateRange(Long projectId, LocalDate startDate, LocalDate endDate) {
        return mapper.findByProjectAndDateRange(projectId, startDate, endDate).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<DailySummary> findByTarget(String targetType, Long targetId, LocalDate startDate, LocalDate endDate) {
        return mapper.findByTarget(targetType, targetId, startDate, endDate).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<DailySummary> findRanking(Long projectId, LocalDate summaryDate) {
        return mapper.findByProjectAndDate(projectId, summaryDate).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteByProjectAndDate(Long projectId, LocalDate summaryDate) {
        mapper.deleteByProjectAndDate(projectId, summaryDate);
    }

    private DailySummaryPO toPO(DailySummary d) {
        DailySummaryPO po = new DailySummaryPO();
        po.setId(d.getId());
        po.setTenantId(d.getTenantId() != null ? d.getTenantId() : 0L);
        po.setProjectId(d.getProjectId());
        po.setSummaryDate(d.getSummaryDate());
        po.setTargetType(d.getTargetType());
        po.setTargetId(d.getTargetId());
        po.setTargetName(d.getTargetName());
        po.setOrgUnitId(d.getOrgUnitId());
        po.setOrgUnitName(d.getOrgUnitName());
        po.setInspectionCount(d.getInspectionCount());
        po.setAvgScore(d.getAvgScore());
        po.setMinScore(d.getMinScore());
        po.setMaxScore(d.getMaxScore());
        po.setTotalDeductions(d.getTotalDeductions());
        po.setTotalBonuses(d.getTotalBonuses());
        po.setPassCount(d.getPassCount());
        po.setFailCount(d.getFailCount());
        po.setRanking(d.getRanking());
        po.setDimensionScores(d.getDimensionScores());
        po.setGrade(d.getGrade());
        po.setCreatedAt(d.getCreatedAt());
        po.setUpdatedAt(d.getUpdatedAt());
        return po;
    }

    private DailySummary toDomain(DailySummaryPO po) {
        DailySummary d = new DailySummary();
        d.setId(po.getId());
        d.setTenantId(po.getTenantId());
        d.setProjectId(po.getProjectId());
        d.setSummaryDate(po.getSummaryDate());
        d.setTargetType(po.getTargetType());
        d.setTargetId(po.getTargetId());
        d.setTargetName(po.getTargetName());
        d.setOrgUnitId(po.getOrgUnitId());
        d.setOrgUnitName(po.getOrgUnitName());
        d.setInspectionCount(po.getInspectionCount());
        d.setAvgScore(po.getAvgScore());
        d.setMinScore(po.getMinScore());
        d.setMaxScore(po.getMaxScore());
        d.setTotalDeductions(po.getTotalDeductions());
        d.setTotalBonuses(po.getTotalBonuses());
        d.setPassCount(po.getPassCount());
        d.setFailCount(po.getFailCount());
        d.setRanking(po.getRanking());
        d.setDimensionScores(po.getDimensionScores());
        d.setGrade(po.getGrade());
        d.setCreatedAt(po.getCreatedAt());
        d.setUpdatedAt(po.getUpdatedAt());
        return d;
    }
}
