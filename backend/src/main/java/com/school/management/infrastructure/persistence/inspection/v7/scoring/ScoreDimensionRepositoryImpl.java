package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.inspection.model.v7.scoring.ScoreDimension;
import com.school.management.domain.inspection.repository.v7.ScoreDimensionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ScoreDimensionRepositoryImpl implements ScoreDimensionRepository {

    private final ScoreDimensionMapper mapper;

    public ScoreDimensionRepositoryImpl(ScoreDimensionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ScoreDimension save(ScoreDimension dimension) {
        ScoreDimensionPO po = toPO(dimension);
        if (dimension.getId() == null) {
            mapper.insert(po);
            dimension.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return dimension;
    }

    @Override
    public Optional<ScoreDimension> findById(Long id) {
        ScoreDimensionPO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<ScoreDimension> findByScoringProfileId(Long scoringProfileId) {
        return mapper.findByScoringProfileId(scoringProfileId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public void deleteByScoringProfileId(Long scoringProfileId) {
        mapper.delete(new LambdaQueryWrapper<ScoreDimensionPO>()
                .eq(ScoreDimensionPO::getScoringProfileId, scoringProfileId));
    }

    private ScoreDimensionPO toPO(ScoreDimension domain) {
        ScoreDimensionPO po = new ScoreDimensionPO();
        po.setId(domain.getId());
        po.setTenantId(domain.getTenantId() != null ? domain.getTenantId() : 0L);
        po.setScoringProfileId(domain.getScoringProfileId());
        po.setDimensionCode(domain.getDimensionCode());
        po.setDimensionName(domain.getDimensionName());
        po.setWeight(domain.getWeight());
        po.setBaseScore(domain.getBaseScore());
        po.setPassThreshold(domain.getPassThreshold());
        po.setSourceType(domain.getSourceType());
        po.setModuleTemplateId(domain.getModuleTemplateId());
        po.setSortOrder(domain.getSortOrder());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }

    private ScoreDimension toDomain(ScoreDimensionPO po) {
        return ScoreDimension.reconstruct(ScoreDimension.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .scoringProfileId(po.getScoringProfileId())
                .dimensionCode(po.getDimensionCode())
                .dimensionName(po.getDimensionName())
                .weight(po.getWeight())
                .baseScore(po.getBaseScore())
                .passThreshold(po.getPassThreshold())
                .sourceType(po.getSourceType())
                .moduleTemplateId(po.getModuleTemplateId())
                .sortOrder(po.getSortOrder())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt()));
    }
}
