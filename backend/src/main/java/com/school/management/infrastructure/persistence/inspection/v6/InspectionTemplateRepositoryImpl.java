package com.school.management.infrastructure.persistence.inspection.v6;

import com.school.management.domain.inspection.model.v6.InspectionTemplate;
import com.school.management.domain.inspection.repository.v6.InspectionTemplateRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class InspectionTemplateRepositoryImpl implements InspectionTemplateRepository {

    private final InspectionTemplateMapper mapper;

    public InspectionTemplateRepositoryImpl(InspectionTemplateMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public InspectionTemplate save(InspectionTemplate template) {
        InspectionTemplatePO po = toPO(template);
        if (po.getId() == null) {
            mapper.insert(po);
            template.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return template;
    }

    @Override
    public Optional<InspectionTemplate> findById(Long id) {
        InspectionTemplatePO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<InspectionTemplate> findAll() {
        return mapper.selectList(null).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private InspectionTemplatePO toPO(InspectionTemplate t) {
        InspectionTemplatePO po = new InspectionTemplatePO();
        po.setId(t.getId());
        po.setTemplateCode(t.getTemplateCode());
        po.setTemplateName(t.getTemplateName());
        po.setDescription(t.getDescription());
        po.setCurrentVersion(t.getCurrentVersion());
        po.setApplicableScope(t.getApplicableScope());
        po.setStatus(t.getStatus());
        po.setIsDefault(t.getIsDefault());
        po.setUseCount(t.getUseCount());
        po.setLastUsedAt(t.getLastUsedAt());
        po.setCreatedBy(t.getCreatedBy());
        po.setUpdatedBy(t.getUpdatedBy());
        return po;
    }

    private InspectionTemplate toDomain(InspectionTemplatePO po) {
        InspectionTemplate t = new InspectionTemplate();
        t.setId(po.getId());
        t.setTemplateCode(po.getTemplateCode());
        t.setTemplateName(po.getTemplateName());
        t.setDescription(po.getDescription());
        t.setCurrentVersion(po.getCurrentVersion());
        t.setApplicableScope(po.getApplicableScope());
        t.setStatus(po.getStatus());
        t.setIsDefault(po.getIsDefault());
        t.setUseCount(po.getUseCount());
        t.setLastUsedAt(po.getLastUsedAt());
        t.setCreatedBy(po.getCreatedBy());
        t.setUpdatedBy(po.getUpdatedBy());
        t.setCreatedAt(po.getCreatedAt());
        t.setUpdatedAt(po.getUpdatedAt());
        return t;
    }
}
