package com.school.management.infrastructure.persistence.inspection.v7.analytics;

import com.school.management.domain.inspection.model.v7.analytics.*;
import com.school.management.domain.inspection.repository.v7.PeriodSummaryRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class PeriodSummaryRepositoryImpl implements PeriodSummaryRepository {

    private final PeriodSummaryMapper mapper;

    public PeriodSummaryRepositoryImpl(PeriodSummaryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public PeriodSummary save(PeriodSummary s) {
        PeriodSummaryPO po = toPO(s);
        if (s.getId() == null) {
            mapper.insert(po);
            s.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return s;
    }

    @Override
    public Optional<PeriodSummary> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public Optional<PeriodSummary> findByProjectPeriodTarget(Long projectId, PeriodType periodType,
                                                               LocalDate periodStart, String targetType, Long targetId) {
        return Optional.ofNullable(mapper.findByProjectPeriodTarget(
                projectId, periodType.name(), periodStart, targetType, targetId)).map(this::toDomain);
    }

    @Override
    public List<PeriodSummary> findByProjectAndPeriod(Long projectId, PeriodType periodType, LocalDate periodStart) {
        return mapper.findByProjectAndPeriod(projectId, periodType.name(), periodStart)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<PeriodSummary> findByTarget(String targetType, Long targetId, PeriodType periodType) {
        return mapper.findByTarget(targetType, targetId, periodType.name())
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<PeriodSummary> findRanking(Long projectId, PeriodType periodType, LocalDate periodStart) {
        return mapper.findByProjectAndPeriod(projectId, periodType.name(), periodStart)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteByProjectAndPeriod(Long projectId, PeriodType periodType, LocalDate periodStart) {
        mapper.deleteByProjectAndPeriod(projectId, periodType.name(), periodStart);
    }

    private PeriodSummaryPO toPO(PeriodSummary d) {
        PeriodSummaryPO po = new PeriodSummaryPO();
        po.setId(d.getId());
        po.setTenantId(d.getTenantId() != null ? d.getTenantId() : 0L);
        po.setProjectId(d.getProjectId());
        po.setPeriodType(d.getPeriodType() != null ? d.getPeriodType().name() : null);
        po.setPeriodStart(d.getPeriodStart());
        po.setPeriodEnd(d.getPeriodEnd());
        po.setTargetType(d.getTargetType());
        po.setTargetId(d.getTargetId());
        po.setTargetName(d.getTargetName());
        po.setOrgUnitId(d.getOrgUnitId());
        po.setOrgUnitName(d.getOrgUnitName());
        po.setInspectionDays(d.getInspectionDays());
        po.setAvgScore(d.getAvgScore());
        po.setMinScore(d.getMinScore());
        po.setMaxScore(d.getMaxScore());
        po.setScoreStdDev(d.getScoreStdDev());
        po.setTrendDirection(d.getTrendDirection() != null ? d.getTrendDirection().name() : null);
        po.setTrendPercent(d.getTrendPercent());
        po.setRanking(d.getRanking());
        po.setDimensionScores(d.getDimensionScores());
        po.setGrade(d.getGrade());
        po.setCorrectiveCount(d.getCorrectiveCount());
        po.setCorrectiveClosedCount(d.getCorrectiveClosedCount());
        po.setPrevPeriodScore(d.getPrevPeriodScore());
        po.setMomChange(d.getMomChange());
        po.setYoyScore(d.getYoyScore());
        po.setYoyChange(d.getYoyChange());
        po.setCreatedAt(d.getCreatedAt());
        po.setUpdatedAt(d.getUpdatedAt());
        return po;
    }

    private PeriodSummary toDomain(PeriodSummaryPO po) {
        PeriodSummary d = new PeriodSummary();
        d.setId(po.getId());
        d.setTenantId(po.getTenantId());
        d.setProjectId(po.getProjectId());
        d.setPeriodType(po.getPeriodType() != null ? PeriodType.valueOf(po.getPeriodType()) : null);
        d.setPeriodStart(po.getPeriodStart());
        d.setPeriodEnd(po.getPeriodEnd());
        d.setTargetType(po.getTargetType());
        d.setTargetId(po.getTargetId());
        d.setTargetName(po.getTargetName());
        d.setOrgUnitId(po.getOrgUnitId());
        d.setOrgUnitName(po.getOrgUnitName());
        d.setInspectionDays(po.getInspectionDays());
        d.setAvgScore(po.getAvgScore());
        d.setMinScore(po.getMinScore());
        d.setMaxScore(po.getMaxScore());
        d.setScoreStdDev(po.getScoreStdDev());
        d.setTrendDirection(po.getTrendDirection() != null ? TrendDirection.valueOf(po.getTrendDirection()) : null);
        d.setTrendPercent(po.getTrendPercent());
        d.setRanking(po.getRanking());
        d.setDimensionScores(po.getDimensionScores());
        d.setGrade(po.getGrade());
        d.setCorrectiveCount(po.getCorrectiveCount());
        d.setCorrectiveClosedCount(po.getCorrectiveClosedCount());
        d.setPrevPeriodScore(po.getPrevPeriodScore());
        d.setMomChange(po.getMomChange());
        d.setYoyScore(po.getYoyScore());
        d.setYoyChange(po.getYoyChange());
        d.setCreatedAt(po.getCreatedAt());
        d.setUpdatedAt(po.getUpdatedAt());
        return d;
    }
}
