package com.school.management.infrastructure.persistence.inspection.v7.template;

import com.school.management.domain.inspection.model.v7.template.TemplateCatalog;
import com.school.management.domain.inspection.repository.v7.TemplateCatalogRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class TemplateCatalogRepositoryImpl implements TemplateCatalogRepository {

    private final TemplateCatalogMapper mapper;

    public TemplateCatalogRepositoryImpl(TemplateCatalogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public TemplateCatalog save(TemplateCatalog catalog) {
        TemplateCatalogPO po = toPO(catalog);
        if (catalog.getId() == null) {
            mapper.insert(po);
            catalog.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return catalog;
    }

    @Override
    public Optional<TemplateCatalog> findById(Long id) {
        TemplateCatalogPO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<TemplateCatalog> findByCatalogCode(String catalogCode) {
        TemplateCatalogPO po = mapper.findByCatalogCode(catalogCode);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<TemplateCatalog> findAll() {
        return mapper.findAllEnabled().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<TemplateCatalog> findByParentId(Long parentId) {
        return mapper.findByParentId(parentId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private TemplateCatalogPO toPO(TemplateCatalog domain) {
        TemplateCatalogPO po = new TemplateCatalogPO();
        po.setId(domain.getId());
        po.setTenantId(domain.getTenantId() != null ? domain.getTenantId() : 0L);
        po.setParentId(domain.getParentId());
        po.setCatalogCode(domain.getCatalogCode());
        po.setCatalogName(domain.getCatalogName());
        po.setDescription(domain.getDescription());
        po.setIcon(domain.getIcon());
        po.setSortOrder(domain.getSortOrder());
        po.setIsEnabled(domain.getIsEnabled());
        po.setCreatedBy(domain.getCreatedBy());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedBy(domain.getUpdatedBy());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }

    private TemplateCatalog toDomain(TemplateCatalogPO po) {
        return TemplateCatalog.reconstruct(TemplateCatalog.builder()
                .id(po.getId())
                .parentId(po.getParentId())
                .catalogCode(po.getCatalogCode())
                .catalogName(po.getCatalogName())
                .description(po.getDescription())
                .icon(po.getIcon())
                .sortOrder(po.getSortOrder())
                .isEnabled(po.getIsEnabled())
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedBy(po.getUpdatedBy())
                .updatedAt(po.getUpdatedAt()));
    }
}
