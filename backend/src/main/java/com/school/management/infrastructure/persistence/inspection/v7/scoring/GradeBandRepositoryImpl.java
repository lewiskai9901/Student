package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.inspection.model.v7.scoring.GradeBand;
import com.school.management.domain.inspection.repository.v7.GradeBandRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class GradeBandRepositoryImpl implements GradeBandRepository {

    private final GradeBandMapper mapper;

    public GradeBandRepositoryImpl(GradeBandMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public GradeBand save(GradeBand gradeBand) {
        GradeBandPO po = toPO(gradeBand);
        if (gradeBand.getId() == null) {
            mapper.insert(po);
            gradeBand.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return gradeBand;
    }

    @Override
    public Optional<GradeBand> findById(Long id) {
        GradeBandPO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<GradeBand> findByScoringProfileId(Long scoringProfileId) {
        return mapper.findByScoringProfileId(scoringProfileId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<GradeBand> findByDimensionId(Long dimensionId) {
        return mapper.findByDimensionId(dimensionId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public void deleteByDimensionId(Long dimensionId) {
        mapper.delete(new LambdaQueryWrapper<GradeBandPO>()
                .eq(GradeBandPO::getDimensionId, dimensionId));
    }

    @Override
    public void deleteByScoringProfileId(Long scoringProfileId) {
        mapper.delete(new LambdaQueryWrapper<GradeBandPO>()
                .eq(GradeBandPO::getScoringProfileId, scoringProfileId));
    }

    private GradeBandPO toPO(GradeBand domain) {
        GradeBandPO po = new GradeBandPO();
        po.setId(domain.getId());
        po.setTenantId(domain.getTenantId() != null ? domain.getTenantId() : 0L);
        po.setScoringProfileId(domain.getScoringProfileId());
        po.setDimensionId(domain.getDimensionId());
        po.setGradeCode(domain.getGradeCode());
        po.setGradeName(domain.getGradeName());
        po.setMinScore(domain.getMinScore());
        po.setMaxScore(domain.getMaxScore());
        po.setColor(domain.getColor());
        po.setIcon(domain.getIcon());
        po.setSortOrder(domain.getSortOrder());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }

    private GradeBand toDomain(GradeBandPO po) {
        return GradeBand.reconstruct(GradeBand.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .scoringProfileId(po.getScoringProfileId())
                .dimensionId(po.getDimensionId())
                .gradeCode(po.getGradeCode())
                .gradeName(po.getGradeName())
                .minScore(po.getMinScore())
                .maxScore(po.getMaxScore())
                .color(po.getColor())
                .icon(po.getIcon())
                .sortOrder(po.getSortOrder())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt()));
    }
}
