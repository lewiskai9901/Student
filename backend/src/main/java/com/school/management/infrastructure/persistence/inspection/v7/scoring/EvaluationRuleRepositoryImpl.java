package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.school.management.domain.inspection.model.v7.scoring.EvaluationRule;
import com.school.management.domain.inspection.repository.v7.EvaluationRuleRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class EvaluationRuleRepositoryImpl implements EvaluationRuleRepository {

    private final EvaluationRuleMapper mapper;

    public EvaluationRuleRepositoryImpl(EvaluationRuleMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public EvaluationRule save(EvaluationRule rule) {
        EvaluationRulePO po = toPO(rule);
        if (rule.getId() == null) {
            mapper.insert(po);
            rule.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return rule;
    }

    @Override
    public Optional<EvaluationRule> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<EvaluationRule> findByProjectId(Long projectId) {
        return mapper.findByProjectId(projectId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private EvaluationRulePO toPO(EvaluationRule d) {
        EvaluationRulePO po = new EvaluationRulePO();
        po.setId(d.getId());
        po.setTenantId(d.getTenantId() != null ? d.getTenantId() : 0L);
        po.setProjectId(d.getProjectId());
        po.setRuleName(d.getRuleName());
        po.setRuleDescription(d.getRuleDescription());
        po.setTargetType(d.getTargetType());
        po.setEvaluationPeriod(d.getEvaluationPeriod());
        po.setAwardName(d.getAwardName());
        po.setRankingEnabled(d.getRankingEnabled());
        po.setSortOrder(d.getSortOrder());
        po.setIsEnabled(d.getIsEnabled());
        po.setCreatedBy(d.getCreatedBy());
        po.setCreatedAt(d.getCreatedAt());
        po.setUpdatedBy(d.getUpdatedBy());
        po.setUpdatedAt(d.getUpdatedAt());
        return po;
    }

    private EvaluationRule toDomain(EvaluationRulePO po) {
        return EvaluationRule.reconstruct(EvaluationRule.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .projectId(po.getProjectId())
                .ruleName(po.getRuleName())
                .ruleDescription(po.getRuleDescription())
                .targetType(po.getTargetType())
                .evaluationPeriod(po.getEvaluationPeriod())
                .awardName(po.getAwardName())
                .rankingEnabled(po.getRankingEnabled())
                .sortOrder(po.getSortOrder())
                .isEnabled(po.getIsEnabled())
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedBy(po.getUpdatedBy())
                .updatedAt(po.getUpdatedAt()));
    }
}
