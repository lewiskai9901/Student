package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.school.management.domain.inspection.model.v7.scoring.IndicatorScore;
import com.school.management.domain.inspection.repository.v7.IndicatorScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class IndicatorScoreRepositoryImpl implements IndicatorScoreRepository {

    private final IndicatorScoreMapper mapper;

    @Override
    public IndicatorScore save(IndicatorScore score) {
        IndicatorScorePO po = toPO(score);
        if (score.getId() == null) {
            mapper.insert(po);
            score.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return score;
    }

    @Override
    public Optional<IndicatorScore> findByIndicatorAndTargetAndPeriod(Long indicatorId, Long targetId, LocalDate periodStart) {
        IndicatorScorePO po = mapper.findByIndicatorAndTargetAndPeriod(indicatorId, targetId, periodStart);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<IndicatorScore> findByIndicatorId(Long indicatorId) {
        return mapper.findByIndicatorId(indicatorId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<IndicatorScore> findByIndicatorIdAndPeriod(Long indicatorId, LocalDate periodStart, LocalDate periodEnd) {
        return mapper.findByIndicatorIdAndPeriod(indicatorId, periodStart, periodEnd).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByIndicatorId(Long indicatorId) {
        mapper.softDeleteByIndicatorId(indicatorId);
    }

    private IndicatorScorePO toPO(IndicatorScore domain) {
        IndicatorScorePO po = new IndicatorScorePO();
        po.setId(domain.getId());
        po.setTenantId(domain.getTenantId());
        po.setIndicatorId(domain.getIndicatorId());
        po.setTargetId(domain.getTargetId());
        po.setTargetName(domain.getTargetName());
        po.setTargetType(domain.getTargetType());
        po.setPeriodStart(domain.getPeriodStart());
        po.setPeriodEnd(domain.getPeriodEnd());
        po.setScore(domain.getScore());
        po.setGradeCode(domain.getGradeCode());
        po.setGradeName(domain.getGradeName());
        po.setGradeColor(domain.getGradeColor());
        po.setSourceCount(domain.getSourceCount());
        po.setDetail(domain.getDetail());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }

    private IndicatorScore toDomain(IndicatorScorePO po) {
        return IndicatorScore.reconstruct(IndicatorScore.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .indicatorId(po.getIndicatorId())
                .targetId(po.getTargetId())
                .targetName(po.getTargetName())
                .targetType(po.getTargetType())
                .periodStart(po.getPeriodStart())
                .periodEnd(po.getPeriodEnd())
                .score(po.getScore())
                .gradeCode(po.getGradeCode())
                .gradeName(po.getGradeName())
                .gradeColor(po.getGradeColor())
                .sourceCount(po.getSourceCount())
                .detail(po.getDetail())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt()));
    }
}
