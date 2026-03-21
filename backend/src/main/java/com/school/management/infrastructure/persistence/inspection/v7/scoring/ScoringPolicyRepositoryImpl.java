package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.school.management.domain.inspection.model.v7.scoring.ScoringPolicy;
import com.school.management.domain.inspection.repository.v7.ScoringPolicyRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ScoringPolicyRepositoryImpl implements ScoringPolicyRepository {

    private final ScoringPolicyMapper mapper;

    public ScoringPolicyRepositoryImpl(ScoringPolicyMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ScoringPolicy save(ScoringPolicy policy) {
        ScoringPolicyPO po = toPO(policy);
        if (policy.getId() == null) {
            mapper.insert(po);
            policy.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return policy;
    }

    @Override
    public Optional<ScoringPolicy> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public Optional<ScoringPolicy> findByCode(String policyCode) {
        return Optional.ofNullable(mapper.findByCode(policyCode)).map(this::toDomain);
    }

    @Override
    public List<ScoringPolicy> findAll() {
        return mapper.selectList(null).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ScoringPolicy> findEnabled() {
        return mapper.findEnabled().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private ScoringPolicyPO toPO(ScoringPolicy d) {
        ScoringPolicyPO po = new ScoringPolicyPO();
        po.setId(d.getId());
        po.setTenantId(d.getTenantId() != null ? d.getTenantId() : 0L);
        po.setPolicyCode(d.getPolicyCode());
        po.setPolicyName(d.getPolicyName());
        po.setDescription(d.getDescription());
        po.setMaxScore(d.getMaxScore());
        po.setMinScore(d.getMinScore());
        po.setPrecisionDigits(d.getPrecisionDigits());
        po.setIsSystem(d.getIsSystem());
        po.setIsEnabled(d.getIsEnabled());
        po.setSortOrder(d.getSortOrder());
        po.setCreatedBy(d.getCreatedBy());
        po.setCreatedAt(d.getCreatedAt());
        po.setUpdatedBy(d.getUpdatedBy());
        po.setUpdatedAt(d.getUpdatedAt());
        return po;
    }

    private ScoringPolicy toDomain(ScoringPolicyPO po) {
        return ScoringPolicy.reconstruct(ScoringPolicy.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .policyCode(po.getPolicyCode())
                .policyName(po.getPolicyName())
                .description(po.getDescription())
                .maxScore(po.getMaxScore())
                .minScore(po.getMinScore())
                .precisionDigits(po.getPrecisionDigits())
                .isSystem(po.getIsSystem())
                .isEnabled(po.getIsEnabled())
                .sortOrder(po.getSortOrder())
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedBy(po.getUpdatedBy())
                .updatedAt(po.getUpdatedAt()));
    }
}
