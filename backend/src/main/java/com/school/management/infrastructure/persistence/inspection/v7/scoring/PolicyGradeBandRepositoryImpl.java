package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.school.management.domain.inspection.model.v7.scoring.PolicyGradeBand;
import com.school.management.domain.inspection.repository.v7.PolicyGradeBandRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class PolicyGradeBandRepositoryImpl implements PolicyGradeBandRepository {

    private final PolicyGradeBandMapper mapper;

    public PolicyGradeBandRepositoryImpl(PolicyGradeBandMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public PolicyGradeBand save(PolicyGradeBand band) {
        PolicyGradeBandPO po = toPO(band);
        if (band.getId() == null) {
            mapper.insert(po);
            band.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return band;
    }

    @Override
    public List<PolicyGradeBand> findByPolicyId(Long policyId) {
        return mapper.findByPolicyId(policyId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByPolicyId(Long policyId) {
        mapper.deleteByPolicyId(policyId);
    }

    private PolicyGradeBandPO toPO(PolicyGradeBand d) {
        PolicyGradeBandPO po = new PolicyGradeBandPO();
        po.setId(d.getId());
        po.setPolicyId(d.getPolicyId());
        po.setGradeCode(d.getGradeCode());
        po.setGradeName(d.getGradeName());
        po.setMinPercent(d.getMinPercent());
        po.setMaxPercent(d.getMaxPercent());
        po.setSortOrder(d.getSortOrder());
        po.setCreatedAt(d.getCreatedAt());
        return po;
    }

    private PolicyGradeBand toDomain(PolicyGradeBandPO po) {
        return PolicyGradeBand.reconstruct(PolicyGradeBand.builder()
                .id(po.getId())
                .policyId(po.getPolicyId())
                .gradeCode(po.getGradeCode())
                .gradeName(po.getGradeName())
                .minPercent(po.getMinPercent())
                .maxPercent(po.getMaxPercent())
                .sortOrder(po.getSortOrder())
                .createdAt(po.getCreatedAt()));
    }
}
