package com.school.management.infrastructure.persistence.message;

import com.school.management.domain.message.model.MsgTemplate;
import com.school.management.domain.message.repository.MsgTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 消息模板仓储实现
 */
@Repository
@RequiredArgsConstructor
public class MsgTemplateRepositoryImpl implements MsgTemplateRepository {

    private final MsgTemplateMapper templateMapper;

    @Override
    public MsgTemplate save(MsgTemplate template) {
        MsgTemplatePO po = toPO(template);
        if (template.getId() == null) {
            po.setCreatedAt(LocalDateTime.now());
            po.setUpdatedAt(LocalDateTime.now());
            templateMapper.insert(po);
        } else {
            po.setUpdatedAt(LocalDateTime.now());
            templateMapper.updateById(po);
        }
        return toDomain(po);
    }

    @Override
    public Optional<MsgTemplate> findById(Long id) {
        MsgTemplatePO po = templateMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<MsgTemplate> findAll() {
        return templateMapper.findAll().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<MsgTemplate> findByCode(String templateCode) {
        MsgTemplatePO po = templateMapper.findByCode(templateCode);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        templateMapper.deleteById(id);
    }

    private MsgTemplate toDomain(MsgTemplatePO po) {
        return MsgTemplate.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .templateCode(po.getTemplateCode())
                .templateName(po.getTemplateName())
                .titleTemplate(po.getTitleTemplate())
                .contentTemplate(po.getContentTemplate())
                .isSystem(po.getIsSystem())
                .isEnabled(po.getIsEnabled())
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    private MsgTemplatePO toPO(MsgTemplate template) {
        MsgTemplatePO po = new MsgTemplatePO();
        po.setId(template.getId());
        po.setTenantId(template.getTenantId() != null ? template.getTenantId() : 0L);
        po.setTemplateCode(template.getTemplateCode());
        po.setTemplateName(template.getTemplateName());
        po.setTitleTemplate(template.getTitleTemplate());
        po.setContentTemplate(template.getContentTemplate());
        po.setIsSystem(template.getIsSystem() != null ? template.getIsSystem() : 0);
        po.setIsEnabled(template.getIsEnabled() != null ? template.getIsEnabled() : 1);
        po.setCreatedBy(template.getCreatedBy());
        po.setCreatedAt(template.getCreatedAt());
        po.setUpdatedAt(template.getUpdatedAt());
        return po;
    }
}
