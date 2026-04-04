package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.school.management.domain.inspection.model.v7.scoring.GradeScheme;
import com.school.management.domain.inspection.repository.v7.GradeSchemeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class GradeSchemeRepositoryImpl implements GradeSchemeRepository {

    private final GradeSchemeMapper mapper;

    @Override
    public GradeScheme save(GradeScheme scheme) {
        GradeSchemePO po = toPO(scheme);
        if (scheme.getId() == null) {
            mapper.insert(po);
            scheme.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return scheme;
    }

    @Override
    public Optional<GradeScheme> findById(Long id) {
        GradeSchemePO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<GradeScheme> findByTenantIdOrSystem(Long tenantId) {
        return mapper.findByTenantIdOrSystem(tenantId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private GradeSchemePO toPO(GradeScheme domain) {
        GradeSchemePO po = new GradeSchemePO();
        po.setId(domain.getId());
        po.setTenantId(domain.getTenantId());
        po.setDisplayName(domain.getDisplayName());
        po.setDescription(domain.getDescription());
        po.setSchemeType(domain.getSchemeType());
        po.setIsSystem(domain.getIsSystem());
        po.setCreatedBy(domain.getCreatedBy());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }

    private GradeScheme toDomain(GradeSchemePO po) {
        return GradeScheme.reconstruct(GradeScheme.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .displayName(po.getDisplayName())
                .description(po.getDescription())
                .schemeType(po.getSchemeType())
                .isSystem(po.getIsSystem())
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt()));
    }
}
