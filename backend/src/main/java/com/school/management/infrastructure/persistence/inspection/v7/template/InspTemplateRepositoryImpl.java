package com.school.management.infrastructure.persistence.inspection.v7.template;

import com.school.management.domain.inspection.model.v7.execution.TargetType;
import com.school.management.domain.inspection.model.v7.template.InspTemplate;
import com.school.management.domain.inspection.model.v7.template.TemplateStatus;
import com.school.management.domain.inspection.repository.v7.InspTemplateRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class InspTemplateRepositoryImpl implements InspTemplateRepository {

    private final InspTemplateMapper mapper;

    public InspTemplateRepositoryImpl(InspTemplateMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public InspTemplate save(InspTemplate template) {
        InspTemplatePO po = toPO(template);
        if (template.getId() == null) {
            mapper.insert(po);
            template.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return template;
    }

    @Override
    public Optional<InspTemplate> findById(Long id) {
        InspTemplatePO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<InspTemplate> findByTemplateCode(String templateCode) {
        InspTemplatePO po = mapper.findByTemplateCode(templateCode);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<InspTemplate> findByStatus(TemplateStatus status) {
        return mapper.findByStatus(status.name()).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<InspTemplate> findByCatalogId(Long catalogId) {
        return mapper.findByCatalogId(catalogId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<InspTemplate> findPagedWithConditions(int offset, int size,
                                                       TemplateStatus status, Long catalogId, String keyword) {
        String statusStr = status != null ? status.name() : null;
        return mapper.findPagedWithConditions(offset, size, statusStr, catalogId, keyword).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countWithConditions(TemplateStatus status, Long catalogId, String keyword) {
        String statusStr = status != null ? status.name() : null;
        return mapper.countWithConditions(statusStr, catalogId, keyword);
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private InspTemplatePO toPO(InspTemplate domain) {
        InspTemplatePO po = new InspTemplatePO();
        po.setId(domain.getId());
        po.setTenantId(domain.getTenantId() != null ? domain.getTenantId() : 0L);
        po.setTemplateCode(domain.getTemplateCode());
        po.setTemplateName(domain.getTemplateName());
        po.setDescription(domain.getDescription());
        po.setCatalogId(domain.getCatalogId());
        po.setTags(domain.getTags());
        po.setTargetType(domain.getTargetType() != null ? domain.getTargetType().name() : null);
        po.setLatestVersion(domain.getLatestVersion());
        po.setStatus(domain.getStatus() != null ? domain.getStatus().name() : null);
        po.setIsDefault(domain.getIsDefault());
        po.setUseCount(domain.getUseCount());
        po.setLastUsedAt(domain.getLastUsedAt());
        po.setCreatedBy(domain.getCreatedBy());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedBy(domain.getUpdatedBy());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }

    private InspTemplate toDomain(InspTemplatePO po) {
        return InspTemplate.reconstruct(InspTemplate.builder()
                .id(po.getId())
                .templateCode(po.getTemplateCode())
                .templateName(po.getTemplateName())
                .description(po.getDescription())
                .catalogId(po.getCatalogId())
                .tags(po.getTags())
                .targetType(po.getTargetType() != null ? TargetType.valueOf(po.getTargetType()) : null)
                .latestVersion(po.getLatestVersion())
                .status(po.getStatus() != null ? TemplateStatus.valueOf(po.getStatus()) : null)
                .isDefault(po.getIsDefault())
                .useCount(po.getUseCount())
                .lastUsedAt(po.getLastUsedAt())
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedBy(po.getUpdatedBy())
                .updatedAt(po.getUpdatedAt()));
    }
}
