package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.scoring.PolicyGradeBand;

import java.util.List;

public interface PolicyGradeBandRepository {

    PolicyGradeBand save(PolicyGradeBand band);

    List<PolicyGradeBand> findByPolicyId(Long policyId);

    void deleteByPolicyId(Long policyId);
}
