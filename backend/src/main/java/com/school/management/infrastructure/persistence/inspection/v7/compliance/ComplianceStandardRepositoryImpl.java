package com.school.management.infrastructure.persistence.inspection.v7.compliance;

import com.school.management.domain.inspection.model.v7.compliance.ComplianceStandard;
import com.school.management.domain.inspection.repository.v7.ComplianceStandardRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ComplianceStandardRepositoryImpl implements ComplianceStandardRepository {

    private final ComplianceStandardMapper mapper;

    public ComplianceStandardRepositoryImpl(ComplianceStandardMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ComplianceStandard save(ComplianceStandard standard) {
        ComplianceStandardPO po = toPO(standard);
        if (standard.getId() == null) {
            mapper.insert(po);
            standard.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return standard;
    }

    @Override
    public Optional<ComplianceStandard> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<ComplianceStandard> findAll() {
        return mapper.selectList(null).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<ComplianceStandard> findByCode(String standardCode) {
        return Optional.ofNullable(mapper.findByCode(standardCode)).map(this::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private ComplianceStandardPO toPO(ComplianceStandard d) {
        ComplianceStandardPO po = new ComplianceStandardPO();
        po.setId(d.getId());
        po.setStandardCode(d.getStandardCode());
        po.setStandardName(d.getStandardName());
        po.setIssuingBody(d.getIssuingBody());
        po.setEffectiveDate(d.getEffectiveDate());
        po.setVersion(d.getStandardVersion());
        po.setDescription(d.getDescription());
        po.setCreatedAt(d.getCreatedAt());
        po.setUpdatedAt(d.getUpdatedAt());
        return po;
    }

    private ComplianceStandard toDomain(ComplianceStandardPO po) {
        return ComplianceStandard.reconstruct(ComplianceStandard.builder()
                .id(po.getId())
                .standardCode(po.getStandardCode())
                .standardName(po.getStandardName())
                .issuingBody(po.getIssuingBody())
                .effectiveDate(po.getEffectiveDate())
                .standardVersion(po.getVersion())
                .description(po.getDescription())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt()));
    }
}
