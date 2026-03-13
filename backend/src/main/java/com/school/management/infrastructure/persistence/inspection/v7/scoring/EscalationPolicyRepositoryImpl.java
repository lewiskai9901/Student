package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.inspection.model.v7.scoring.EscalationPolicy;
import com.school.management.domain.inspection.repository.v7.EscalationPolicyRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class EscalationPolicyRepositoryImpl implements EscalationPolicyRepository {

    private final EscalationPolicyMapper mapper;

    public EscalationPolicyRepositoryImpl(EscalationPolicyMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public EscalationPolicy save(EscalationPolicy policy) {
        EscalationPolicyPO po = toPO(policy);
        if (policy.getId() == null) {
            mapper.insert(po);
            policy.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return policy;
    }

    @Override
    public Optional<EscalationPolicy> findById(Long id) {
        EscalationPolicyPO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<EscalationPolicy> findByProfileId(Long profileId) {
        return mapper.findByProfileId(profileId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public void deleteByProfileId(Long profileId) {
        mapper.delete(new LambdaQueryWrapper<EscalationPolicyPO>()
                .eq(EscalationPolicyPO::getProfileId, profileId));
    }

    private EscalationPolicyPO toPO(EscalationPolicy d) {
        EscalationPolicyPO po = new EscalationPolicyPO();
        po.setId(d.getId());
        po.setTenantId(d.getTenantId() != null ? d.getTenantId() : 0L);
        po.setProfileId(d.getProfileId());
        po.setPolicyName(d.getPolicyName());
        po.setLookupPeriodDays(d.getLookupPeriodDays());
        po.setEscalationMode(d.getEscalationMode());
        po.setMultiplier(d.getMultiplier());
        po.setAdder(d.getAdder());
        po.setFixedTable(d.getFixedTable());
        po.setMaxEscalationFactor(d.getMaxEscalationFactor());
        po.setMatchBy(d.getMatchBy());
        po.setIsEnabled(d.getIsEnabled());
        po.setCreatedAt(d.getCreatedAt());
        po.setUpdatedAt(d.getUpdatedAt());
        return po;
    }

    private EscalationPolicy toDomain(EscalationPolicyPO po) {
        return EscalationPolicy.reconstruct(EscalationPolicy.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .profileId(po.getProfileId())
                .policyName(po.getPolicyName())
                .lookupPeriodDays(po.getLookupPeriodDays())
                .escalationMode(po.getEscalationMode())
                .multiplier(po.getMultiplier())
                .adder(po.getAdder())
                .fixedTable(po.getFixedTable())
                .maxEscalationFactor(po.getMaxEscalationFactor())
                .matchBy(po.getMatchBy())
                .isEnabled(po.getIsEnabled())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt()));
    }
}
