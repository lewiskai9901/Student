package com.school.management.domain.inspection.repository;

import com.school.management.domain.inspection.model.scoring.PolicyCalcRule;

import java.util.List;

public interface PolicyCalcRuleRepository {

    PolicyCalcRule save(PolicyCalcRule rule);

    List<PolicyCalcRule> findByPolicyId(Long policyId);

    void deleteByPolicyId(Long policyId);
}
