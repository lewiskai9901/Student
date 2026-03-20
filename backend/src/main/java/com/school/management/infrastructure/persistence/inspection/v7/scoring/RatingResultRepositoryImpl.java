package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.inspection.model.v7.scoring.RatingResult;
import com.school.management.domain.inspection.repository.v7.RatingResultRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Repository("inspRatingResultRepositoryImpl")
public class RatingResultRepositoryImpl implements RatingResultRepository {

    private final RatingResultMapper mapper;

    public RatingResultRepositoryImpl(RatingResultMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public RatingResult save(RatingResult result) {
        RatingResultPO po = toPO(result);
        if (result.getId() == null) {
            mapper.insert(po);
            result.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return result;
    }

    @Override
    public List<RatingResult> findByDimensionId(Long dimensionId) {
        return mapper.findByDimensionId(dimensionId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<RatingResult> findByDimensionIdAndCycleDate(Long dimensionId, LocalDate cycleDate) {
        return mapper.findByDimensionIdAndCycleDate(dimensionId, cycleDate).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByDimensionId(Long dimensionId) {
        mapper.delete(new LambdaQueryWrapper<RatingResultPO>()
                .eq(RatingResultPO::getDimensionId, dimensionId));
    }

    @Override
    public void deleteByDimensionIdAndCycleDate(Long dimensionId, LocalDate cycleDate) {
        mapper.delete(new LambdaQueryWrapper<RatingResultPO>()
                .eq(RatingResultPO::getDimensionId, dimensionId)
                .eq(RatingResultPO::getCycleDate, cycleDate));
    }

    private RatingResultPO toPO(RatingResult d) {
        RatingResultPO po = new RatingResultPO();
        po.setId(d.getId());
        po.setTenantId(d.getTenantId() != null ? d.getTenantId() : 0L);
        po.setDimensionId(d.getDimensionId());
        po.setTargetId(d.getTargetId());
        po.setTargetName(d.getTargetName());
        po.setTargetType(d.getTargetType());
        po.setCycleDate(d.getCycleDate());
        po.setScore(d.getScore());
        po.setGrade(d.getGrade());
        po.setRankNo(d.getRankNo());
        po.setCreatedAt(d.getCreatedAt());
        return po;
    }

    private RatingResult toDomain(RatingResultPO po) {
        return RatingResult.reconstruct(RatingResult.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .dimensionId(po.getDimensionId())
                .targetId(po.getTargetId())
                .targetName(po.getTargetName())
                .targetType(po.getTargetType())
                .cycleDate(po.getCycleDate())
                .score(po.getScore())
                .grade(po.getGrade())
                .rankNo(po.getRankNo())
                .createdAt(po.getCreatedAt()));
    }
}
