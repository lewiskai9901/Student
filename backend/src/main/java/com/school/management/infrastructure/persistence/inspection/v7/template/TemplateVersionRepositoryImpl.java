package com.school.management.infrastructure.persistence.inspection.v7.template;

import com.school.management.domain.inspection.model.v7.template.TemplateVersion;
import com.school.management.domain.inspection.repository.v7.TemplateVersionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class TemplateVersionRepositoryImpl implements TemplateVersionRepository {

    private final TemplateVersionMapper mapper;

    public TemplateVersionRepositoryImpl(TemplateVersionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public TemplateVersion save(TemplateVersion version) {
        TemplateVersionPO po = toPO(version);
        if (version.getId() == null) {
            mapper.insert(po);
            version.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return version;
    }

    @Override
    public Optional<TemplateVersion> findById(Long id) {
        TemplateVersionPO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<TemplateVersion> findByTemplateIdAndVersion(Long templateId, Integer version) {
        TemplateVersionPO po = mapper.findByTemplateIdAndVersion(templateId, version);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<TemplateVersion> findByTemplateId(Long templateId) {
        return mapper.findByTemplateId(templateId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<TemplateVersion> findLatestByTemplateId(Long templateId) {
        TemplateVersionPO po = mapper.findLatestByTemplateId(templateId);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    private TemplateVersionPO toPO(TemplateVersion domain) {
        TemplateVersionPO po = new TemplateVersionPO();
        po.setId(domain.getId());
        po.setTenantId(domain.getTenantId() != null ? domain.getTenantId() : 0L);
        po.setTemplateId(domain.getTemplateId());
        po.setVersion(domain.getVersion());
        po.setStructureSnapshot(domain.getStructureSnapshot());
        po.setScoringProfileSnapshot(domain.getScoringProfileSnapshot());
        po.setCreatedBy(domain.getCreatedBy());
        po.setCreatedAt(domain.getCreatedAt());
        return po;
    }

    private TemplateVersion toDomain(TemplateVersionPO po) {
        return TemplateVersion.reconstruct(
                po.getId(), po.getTemplateId(), po.getVersion(),
                po.getStructureSnapshot(), po.getScoringProfileSnapshot(),
                po.getCreatedBy(), po.getCreatedAt());
    }
}
