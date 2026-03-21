package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.school.management.domain.inspection.model.v7.scoring.EvaluationLevel;
import com.school.management.domain.inspection.repository.v7.EvaluationLevelRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class EvaluationLevelRepositoryImpl implements EvaluationLevelRepository {

    private final EvaluationLevelMapper mapper;

    public EvaluationLevelRepositoryImpl(EvaluationLevelMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public EvaluationLevel save(EvaluationLevel level) {
        EvaluationLevelPO po = toPO(level);
        if (level.getId() == null) {
            mapper.insert(po);
            level.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return level;
    }

    @Override
    public List<EvaluationLevel> findByRuleId(Long ruleId) {
        return mapper.findByRuleId(ruleId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByRuleId(Long ruleId) {
        mapper.deleteByRuleId(ruleId);
    }

    private EvaluationLevelPO toPO(EvaluationLevel d) {
        EvaluationLevelPO po = new EvaluationLevelPO();
        po.setId(d.getId());
        po.setRuleId(d.getRuleId());
        po.setLevelNum(d.getLevelNum());
        po.setLevelName(d.getLevelName());
        po.setLevelIcon(d.getLevelIcon());
        po.setLevelColor(d.getLevelColor());
        po.setConditionLogic(d.getConditionLogic());
        po.setConditions(d.getConditions());
        po.setSortOrder(d.getSortOrder());
        po.setCreatedAt(d.getCreatedAt());
        po.setUpdatedAt(d.getUpdatedAt());
        return po;
    }

    private EvaluationLevel toDomain(EvaluationLevelPO po) {
        return EvaluationLevel.reconstruct(EvaluationLevel.builder()
                .id(po.getId())
                .ruleId(po.getRuleId())
                .levelNum(po.getLevelNum())
                .levelName(po.getLevelName())
                .levelIcon(po.getLevelIcon())
                .levelColor(po.getLevelColor())
                .conditionLogic(po.getConditionLogic())
                .conditions(po.getConditions())
                .sortOrder(po.getSortOrder())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt()));
    }
}
