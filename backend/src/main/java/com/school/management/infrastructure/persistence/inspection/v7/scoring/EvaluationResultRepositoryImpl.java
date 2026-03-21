package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.school.management.domain.inspection.model.v7.scoring.EvaluationResult;
import com.school.management.domain.inspection.repository.v7.EvaluationResultRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class EvaluationResultRepositoryImpl implements EvaluationResultRepository {

    private final EvaluationResultMapper mapper;

    public EvaluationResultRepositoryImpl(EvaluationResultMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public EvaluationResult save(EvaluationResult result) {
        EvaluationResultPO po = toPO(result);
        if (result.getId() == null) {
            mapper.insert(po);
            result.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return result;
    }

    @Override
    public List<EvaluationResult> findByRuleIdAndCycleDate(Long ruleId, LocalDate cycleDate) {
        return mapper.findByRuleIdAndCycleDate(ruleId, cycleDate).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByRuleIdAndCycleDate(Long ruleId, LocalDate cycleDate) {
        mapper.deleteByRuleIdAndCycleDate(ruleId, cycleDate);
    }

    private EvaluationResultPO toPO(EvaluationResult d) {
        EvaluationResultPO po = new EvaluationResultPO();
        po.setId(d.getId());
        po.setTenantId(d.getTenantId() != null ? d.getTenantId() : 0L);
        po.setRuleId(d.getRuleId());
        po.setTargetType(d.getTargetType());
        po.setTargetId(d.getTargetId());
        po.setTargetName(d.getTargetName());
        po.setCycleDate(d.getCycleDate());
        po.setLevelNum(d.getLevelNum());
        po.setLevelName(d.getLevelName());
        po.setScore(d.getScore());
        po.setRankNo(d.getRankNo());
        po.setDetails(d.getDetails());
        po.setCreatedAt(d.getCreatedAt());
        return po;
    }

    private EvaluationResult toDomain(EvaluationResultPO po) {
        return EvaluationResult.reconstruct(EvaluationResult.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .ruleId(po.getRuleId())
                .targetType(po.getTargetType())
                .targetId(po.getTargetId())
                .targetName(po.getTargetName())
                .cycleDate(po.getCycleDate())
                .levelNum(po.getLevelNum())
                .levelName(po.getLevelName())
                .score(po.getScore())
                .rankNo(po.getRankNo())
                .details(po.getDetails())
                .createdAt(po.getCreatedAt()));
    }
}
