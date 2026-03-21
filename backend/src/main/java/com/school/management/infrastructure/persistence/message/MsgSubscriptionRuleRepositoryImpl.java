package com.school.management.infrastructure.persistence.message;

import com.school.management.domain.message.model.MsgSubscriptionRule;
import com.school.management.domain.message.repository.MsgSubscriptionRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 消息订阅规则仓储实现
 */
@Repository
@RequiredArgsConstructor
public class MsgSubscriptionRuleRepositoryImpl implements MsgSubscriptionRuleRepository {

    private final MsgSubscriptionRuleMapper ruleMapper;

    @Override
    public MsgSubscriptionRule save(MsgSubscriptionRule rule) {
        MsgSubscriptionRulePO po = toPO(rule);
        if (rule.getId() == null) {
            po.setCreatedAt(LocalDateTime.now());
            po.setUpdatedAt(LocalDateTime.now());
            ruleMapper.insert(po);
        } else {
            po.setUpdatedAt(LocalDateTime.now());
            ruleMapper.updateById(po);
        }
        return toDomain(po);
    }

    @Override
    public Optional<MsgSubscriptionRule> findById(Long id) {
        MsgSubscriptionRulePO po = ruleMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<MsgSubscriptionRule> findAll() {
        return ruleMapper.findAll().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<MsgSubscriptionRule> findEnabled() {
        return ruleMapper.findEnabled().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<MsgSubscriptionRule> findByEventType(String eventCategory, String eventType) {
        return ruleMapper.findByEvent(eventCategory, eventType)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        ruleMapper.deleteById(id);
    }

    private MsgSubscriptionRule toDomain(MsgSubscriptionRulePO po) {
        return MsgSubscriptionRule.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .ruleName(po.getRuleName())
                .eventCategory(po.getEventCategory())
                .eventType(po.getEventType())
                .targetMode(po.getTargetMode())
                .targetConfig(po.getTargetConfig())
                .channel(po.getChannel())
                .templateId(po.getTemplateId())
                .isEnabled(po.getIsEnabled())
                .sortOrder(po.getSortOrder())
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    private MsgSubscriptionRulePO toPO(MsgSubscriptionRule rule) {
        MsgSubscriptionRulePO po = new MsgSubscriptionRulePO();
        po.setId(rule.getId());
        po.setTenantId(rule.getTenantId() != null ? rule.getTenantId() : 0L);
        po.setRuleName(rule.getRuleName());
        po.setEventCategory(rule.getEventCategory());
        po.setEventType(rule.getEventType());
        po.setTargetMode(rule.getTargetMode());
        po.setTargetConfig(rule.getTargetConfig());
        po.setChannel(rule.getChannel());
        po.setTemplateId(rule.getTemplateId());
        po.setIsEnabled(rule.getIsEnabled() != null ? rule.getIsEnabled() : 1);
        po.setSortOrder(rule.getSortOrder() != null ? rule.getSortOrder() : 0);
        po.setCreatedBy(rule.getCreatedBy());
        po.setCreatedAt(rule.getCreatedAt());
        po.setUpdatedAt(rule.getUpdatedAt());
        return po;
    }
}
