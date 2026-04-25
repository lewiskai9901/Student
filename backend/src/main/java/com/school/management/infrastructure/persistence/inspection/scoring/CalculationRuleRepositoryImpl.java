package com.school.management.infrastructure.persistence.inspection.scoring;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.inspection.model.scoring.CalculationRule;
import com.school.management.domain.inspection.model.scoring.RuleType;
import com.school.management.domain.inspection.repository.CalculationRuleRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class CalculationRuleRepositoryImpl implements CalculationRuleRepository {

    private final CalculationRuleMapper mapper;

    public CalculationRuleRepositoryImpl(CalculationRuleMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public CalculationRule save(CalculationRule rule) {
        CalculationRulePO po = toPO(rule);
        if (rule.getId() == null) {
            mapper.insert(po);
            rule.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return rule;
    }

    @Override
    public Optional<CalculationRule> findById(Long id) {
        CalculationRulePO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<CalculationRule> findByScoringProfileId(Long scoringProfileId) {
        return mapper.findByScoringProfileId(scoringProfileId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<CalculationRule> findByScoringProfileIdOrderByPriority(Long scoringProfileId) {
        return mapper.findByScoringProfileIdOrderByPriority(scoringProfileId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public void deleteByScoringProfileId(Long scoringProfileId) {
        mapper.delete(new LambdaQueryWrapper<CalculationRulePO>()
                .eq(CalculationRulePO::getScoringProfileId, scoringProfileId));
    }

    private CalculationRulePO toPO(CalculationRule domain) {
        CalculationRulePO po = new CalculationRulePO();
        po.setId(domain.getId());
        po.setTenantId(domain.getTenantId() != null ? domain.getTenantId() : 0L);
        po.setScoringProfileId(domain.getScoringProfileId());
        po.setRuleCode(domain.getRuleCode());
        po.setRuleName(domain.getRuleName());
        po.setPriority(domain.getPriority());
        po.setRuleType(domain.getRuleType() != null ? domain.getRuleType().name() : null);
        po.setConfig(domain.getConfig());
        po.setIsEnabled(domain.getIsEnabled());
        po.setScopeType(domain.getScopeType());
        po.setTargetDimensionIds(domain.getTargetDimensionIds());
        po.setActivationCondition(domain.getActivationCondition());
        po.setAppliesTo(domain.getAppliesTo());
        po.setEffectiveFrom(domain.getEffectiveFrom());
        po.setEffectiveUntil(domain.getEffectiveUntil());
        po.setExclusionGroup(domain.getExclusionGroup());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }

    private CalculationRule toDomain(CalculationRulePO po) {
        return CalculationRule.reconstruct(CalculationRule.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .scoringProfileId(po.getScoringProfileId())
                .ruleCode(po.getRuleCode())
                .ruleName(po.getRuleName())
                .priority(po.getPriority())
                .ruleType(po.getRuleType() != null ? RuleType.valueOf(po.getRuleType()) : null)
                .config(po.getConfig())
                .isEnabled(po.getIsEnabled())
                .scopeType(po.getScopeType())
                .targetDimensionIds(po.getTargetDimensionIds())
                .activationCondition(po.getActivationCondition())
                .appliesTo(po.getAppliesTo())
                .effectiveFrom(po.getEffectiveFrom())
                .effectiveUntil(po.getEffectiveUntil())
                .exclusionGroup(po.getExclusionGroup())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt()));
    }
}
