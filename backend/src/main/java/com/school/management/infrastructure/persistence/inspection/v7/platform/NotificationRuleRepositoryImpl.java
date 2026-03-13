package com.school.management.infrastructure.persistence.inspection.v7.platform;

import com.school.management.domain.inspection.model.v7.platform.NotificationRule;
import com.school.management.domain.inspection.repository.v7.NotificationRuleRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class NotificationRuleRepositoryImpl implements NotificationRuleRepository {

    private final NotificationRuleMapper mapper;

    public NotificationRuleRepositoryImpl(NotificationRuleMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public NotificationRule save(NotificationRule rule) {
        NotificationRulePO po = toPO(rule);
        if (rule.getId() == null) {
            mapper.insert(po);
            rule.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return rule;
    }

    @Override
    public Optional<NotificationRule> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<NotificationRule> findByProjectId(Long projectId) {
        return mapper.findByProjectId(projectId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<NotificationRule> findByEventType(String eventType) {
        return mapper.findByEventType(eventType).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<NotificationRule> findAllEnabled() {
        return mapper.findAllEnabled().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private NotificationRulePO toPO(NotificationRule d) {
        NotificationRulePO po = new NotificationRulePO();
        po.setId(d.getId());
        po.setTenantId(d.getTenantId());
        po.setProjectId(d.getProjectId());
        po.setRuleName(d.getRuleName());
        po.setEventType(d.getEventType());
        po.setCondition(d.getCondition());
        po.setChannels(d.getChannels());
        po.setRecipientType(d.getRecipientType());
        po.setRecipientConfig(d.getRecipientConfig());
        po.setIsEnabled(d.getIsEnabled());
        po.setPriority(d.getPriority());
        po.setCreatedAt(d.getCreatedAt());
        po.setUpdatedAt(d.getUpdatedAt());
        return po;
    }

    private NotificationRule toDomain(NotificationRulePO po) {
        return NotificationRule.reconstruct(NotificationRule.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .projectId(po.getProjectId())
                .ruleName(po.getRuleName())
                .eventType(po.getEventType())
                .condition(po.getCondition())
                .channels(po.getChannels())
                .recipientType(po.getRecipientType())
                .recipientConfig(po.getRecipientConfig())
                .isEnabled(po.getIsEnabled())
                .priority(po.getPriority())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt()));
    }
}
