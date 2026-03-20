package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.inspection.model.v7.scoring.RatingDimension;
import com.school.management.domain.inspection.repository.v7.RatingDimensionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class RatingDimensionRepositoryImpl implements RatingDimensionRepository {

    private final RatingDimensionMapper mapper;

    public RatingDimensionRepositoryImpl(RatingDimensionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public RatingDimension save(RatingDimension dimension) {
        RatingDimensionPO po = toPO(dimension);
        if (dimension.getId() == null) {
            mapper.insert(po);
            dimension.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return dimension;
    }

    @Override
    public Optional<RatingDimension> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<RatingDimension> findByProjectId(Long projectId) {
        return mapper.findByProjectId(projectId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public void deleteByProjectId(Long projectId) {
        mapper.delete(new LambdaQueryWrapper<RatingDimensionPO>()
                .eq(RatingDimensionPO::getProjectId, projectId));
    }

    private RatingDimensionPO toPO(RatingDimension d) {
        RatingDimensionPO po = new RatingDimensionPO();
        po.setId(d.getId());
        po.setTenantId(d.getTenantId() != null ? d.getTenantId() : 0L);
        po.setProjectId(d.getProjectId());
        po.setDimensionName(d.getDimensionName());
        po.setSectionIds(d.getSectionIds());
        po.setAggregation(d.getAggregation());
        po.setGradeBands(d.getGradeBands());
        po.setAwardName(d.getAwardName());
        po.setRankingEnabled(d.getRankingEnabled());
        po.setSortOrder(d.getSortOrder());
        po.setCreatedBy(d.getCreatedBy());
        po.setCreatedAt(d.getCreatedAt());
        po.setUpdatedAt(d.getUpdatedAt());
        return po;
    }

    private RatingDimension toDomain(RatingDimensionPO po) {
        return RatingDimension.reconstruct(RatingDimension.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .projectId(po.getProjectId())
                .dimensionName(po.getDimensionName())
                .sectionIds(po.getSectionIds())
                .aggregation(po.getAggregation())
                .gradeBands(po.getGradeBands())
                .awardName(po.getAwardName())
                .rankingEnabled(po.getRankingEnabled())
                .sortOrder(po.getSortOrder())
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt()));
    }
}
