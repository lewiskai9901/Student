package com.school.management.infrastructure.persistence.inspection.v7.analytics;

import com.school.management.domain.inspection.model.v7.analytics.ItemFrequencySummary;
import com.school.management.domain.inspection.model.v7.analytics.PeriodType;
import com.school.management.domain.inspection.repository.v7.ItemFrequencySummaryRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class ItemFrequencySummaryRepositoryImpl implements ItemFrequencySummaryRepository {

    private final ItemFrequencySummaryMapper mapper;

    public ItemFrequencySummaryRepositoryImpl(ItemFrequencySummaryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ItemFrequencySummary save(ItemFrequencySummary s) {
        ItemFrequencySummaryPO po = toPO(s);
        if (s.getId() == null) {
            mapper.insert(po);
            s.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return s;
    }

    @Override
    public List<ItemFrequencySummary> findByProjectAndPeriod(Long projectId, PeriodType periodType, LocalDate periodStart) {
        return mapper.findByProjectAndPeriod(projectId, periodType.name(), periodStart)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<ItemFrequencySummary> findTopNByDeduction(Long projectId, PeriodType periodType,
                                                           LocalDate periodStart, int limit) {
        return mapper.findTopNByDeduction(projectId, periodType.name(), periodStart, limit)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private ItemFrequencySummaryPO toPO(ItemFrequencySummary d) {
        ItemFrequencySummaryPO po = new ItemFrequencySummaryPO();
        po.setId(d.getId());
        po.setTenantId(d.getTenantId());
        po.setProjectId(d.getProjectId());
        po.setPeriodType(d.getPeriodType() != null ? d.getPeriodType().name() : null);
        po.setPeriodStart(d.getPeriodStart());
        po.setPeriodEnd(d.getPeriodEnd());
        po.setItemCode(d.getItemCode());
        po.setItemName(d.getItemName());
        po.setSectionId(d.getSectionId());
        po.setSectionName(d.getSectionName());
        po.setOccurrenceCount(d.getOccurrenceCount());
        po.setFlaggedCount(d.getFlaggedCount());
        po.setTotalDeduction(d.getTotalDeduction());
        po.setAvgDeduction(d.getAvgDeduction());
        po.setCumulativePercentage(d.getCumulativePercentage());
        po.setCreatedAt(d.getCreatedAt());
        po.setUpdatedAt(d.getUpdatedAt());
        return po;
    }

    private ItemFrequencySummary toDomain(ItemFrequencySummaryPO po) {
        ItemFrequencySummary d = new ItemFrequencySummary();
        d.setId(po.getId());
        d.setTenantId(po.getTenantId());
        d.setProjectId(po.getProjectId());
        d.setPeriodType(po.getPeriodType() != null ? PeriodType.valueOf(po.getPeriodType()) : null);
        d.setPeriodStart(po.getPeriodStart());
        d.setPeriodEnd(po.getPeriodEnd());
        d.setItemCode(po.getItemCode());
        d.setItemName(po.getItemName());
        d.setSectionId(po.getSectionId());
        d.setSectionName(po.getSectionName());
        d.setOccurrenceCount(po.getOccurrenceCount());
        d.setFlaggedCount(po.getFlaggedCount());
        d.setTotalDeduction(po.getTotalDeduction());
        d.setAvgDeduction(po.getAvgDeduction());
        d.setCumulativePercentage(po.getCumulativePercentage());
        d.setCreatedAt(po.getCreatedAt());
        d.setUpdatedAt(po.getUpdatedAt());
        return d;
    }
}
