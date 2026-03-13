package com.school.management.infrastructure.persistence.inspection.v7.template;

import com.school.management.domain.inspection.model.v7.template.TemplateModuleRef;
import com.school.management.domain.inspection.repository.v7.TemplateModuleRefRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class TemplateModuleRefRepositoryImpl implements TemplateModuleRefRepository {

    private final TemplateModuleRefMapper mapper;

    public TemplateModuleRefRepositoryImpl(TemplateModuleRefMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public TemplateModuleRef save(TemplateModuleRef ref) {
        TemplateModuleRefPO po = toPO(ref);
        if (ref.getId() == null) {
            mapper.insert(po);
            ref.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return ref;
    }

    @Override
    public Optional<TemplateModuleRef> findById(Long id) {
        TemplateModuleRefPO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<TemplateModuleRef> findByCompositeTemplateId(Long compositeTemplateId) {
        return mapper.findByCompositeTemplateId(compositeTemplateId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public void deleteByCompositeTemplateId(Long compositeTemplateId) {
        mapper.softDeleteByCompositeTemplateId(compositeTemplateId);
    }

    private TemplateModuleRefPO toPO(TemplateModuleRef domain) {
        TemplateModuleRefPO po = new TemplateModuleRefPO();
        po.setId(domain.getId());
        po.setTenantId(0L);
        po.setCompositeTemplateId(domain.getCompositeTemplateId());
        po.setModuleTemplateId(domain.getModuleTemplateId());
        po.setSortOrder(domain.getSortOrder());
        po.setWeight(domain.getWeight());
        po.setOverrideConfig(domain.getOverrideConfig());
        po.setCreatedAt(domain.getCreatedAt());
        return po;
    }

    private TemplateModuleRef toDomain(TemplateModuleRefPO po) {
        return TemplateModuleRef.reconstruct(TemplateModuleRef.builder()
                .id(po.getId())
                .compositeTemplateId(po.getCompositeTemplateId())
                .moduleTemplateId(po.getModuleTemplateId())
                .sortOrder(po.getSortOrder())
                .weight(po.getWeight())
                .overrideConfig(po.getOverrideConfig())
                .createdAt(po.getCreatedAt()));
    }
}
