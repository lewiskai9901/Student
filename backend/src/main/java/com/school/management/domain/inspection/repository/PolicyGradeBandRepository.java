package com.school.management.domain.inspection.repository;

import com.school.management.domain.inspection.model.scoring.PolicyGradeBand;

import java.util.List;

public interface PolicyGradeBandRepository {

    PolicyGradeBand save(PolicyGradeBand band);

    List<PolicyGradeBand> findByPolicyId(Long policyId);

    void deleteByPolicyId(Long policyId);
}
