package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.inspection.model.v7.scoring.CalculationRuleV7;
import com.school.management.domain.inspection.model.v7.scoring.RuleType;
import com.school.management.domain.inspection.repository.v7.CalculationRuleV7Repository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class CalculationRuleV7RepositoryImpl implements CalculationRuleV7Repository {

    private final CalculationRuleV7Mapper mapper;

    public CalculationRuleV7RepositoryImpl(CalculationRuleV7Mapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public CalculationRuleV7 save(CalculationRuleV7 rule) {
        CalculationRuleV7PO po = toPO(rule);
        if (rule.getId() == null) {
            mapper.insert(po);
            rule.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return rule;
    }

    @Override
    public Optional<CalculationRuleV7> findById(Long id) {
        CalculationRuleV7PO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<CalculationRuleV7> findByScoringProfileId(Long scoringProfileId) {
        return mapper.findByScoringProfileId(scoringProfileId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<CalculationRuleV7> findByScoringProfileIdOrderByPriority(Long scoringProfileId) {
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
        mapper.delete(new LambdaQueryWrapper<CalculationRuleV7PO>()
                .eq(CalculationRuleV7PO::getScoringProfileId, scoringProfileId));
    }

    private CalculationRuleV7PO toPO(CalculationRuleV7 domain) {
        CalculationRuleV7PO po = new CalculationRuleV7PO();
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

    private CalculationRuleV7 toDomain(CalculationRuleV7PO po) {
        return CalculationRuleV7.reconstruct(CalculationRuleV7.builder()
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
