package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.school.management.domain.inspection.model.v7.scoring.PolicyCalcRule;
import com.school.management.domain.inspection.repository.v7.PolicyCalcRuleRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class PolicyCalcRuleRepositoryImpl implements PolicyCalcRuleRepository {

    private final PolicyCalcRuleMapper mapper;

    public PolicyCalcRuleRepositoryImpl(PolicyCalcRuleMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public PolicyCalcRule save(PolicyCalcRule rule) {
        PolicyCalcRulePO po = toPO(rule);
        if (rule.getId() == null) {
            mapper.insert(po);
            rule.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return rule;
    }

    @Override
    public List<PolicyCalcRule> findByPolicyId(Long policyId) {
        return mapper.findByPolicyId(policyId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByPolicyId(Long policyId) {
        mapper.deleteByPolicyId(policyId);
    }

    private PolicyCalcRulePO toPO(PolicyCalcRule d) {
        PolicyCalcRulePO po = new PolicyCalcRulePO();
        po.setId(d.getId());
        po.setPolicyId(d.getPolicyId());
        po.setRuleCode(d.getRuleCode());
        po.setRuleName(d.getRuleName());
        po.setRuleType(d.getRuleType());
        po.setPriority(d.getPriority());
        po.setConfig(d.getConfig());
        po.setIsEnabled(d.getIsEnabled());
        po.setCreatedAt(d.getCreatedAt());
        po.setUpdatedAt(d.getUpdatedAt());
        return po;
    }

    private PolicyCalcRule toDomain(PolicyCalcRulePO po) {
        return PolicyCalcRule.reconstruct(PolicyCalcRule.builder()
                .id(po.getId())
                .policyId(po.getPolicyId())
                .ruleCode(po.getRuleCode())
                .ruleName(po.getRuleName())
                .ruleType(po.getRuleType())
                .priority(po.getPriority())
                .config(po.getConfig())
                .isEnabled(po.getIsEnabled())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt()));
    }
}
