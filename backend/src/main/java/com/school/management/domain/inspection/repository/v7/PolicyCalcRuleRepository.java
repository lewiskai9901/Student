package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.scoring.PolicyCalcRule;

import java.util.List;

public interface PolicyCalcRuleRepository {

    PolicyCalcRule save(PolicyCalcRule rule);

    List<PolicyCalcRule> findByPolicyId(Long policyId);

    void deleteByPolicyId(Long policyId);
}
