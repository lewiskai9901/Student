package com.school.management.application.inspection.v7;

import com.school.management.domain.inspection.model.v7.platform.NotificationRule;
import com.school.management.domain.inspection.repository.v7.NotificationRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class NotificationRuleApplicationService {

    private final NotificationRuleRepository ruleRepository;

    // ========== CRUD ==========

    @Transactional
    public NotificationRule create(Long projectId, String ruleName, String eventType,
                                   String channels, String recipientType,
                                   String recipientConfig, String condition,
                                   Integer priority) {
        NotificationRule rule = NotificationRule.reconstruct(NotificationRule.builder()
                .projectId(projectId)
                .ruleName(ruleName)
                .eventType(eventType)
                .channels(channels)
                .recipientType(recipientType)
                .recipientConfig(recipientConfig)
                .condition(condition)
                .priority(priority)
                .isEnabled(true));
        NotificationRule saved = ruleRepository.save(rule);
        log.info("Created notification rule: name={}, eventType={}, projectId={}", ruleName, eventType, projectId);
        return saved;
    }

    @Transactional
    public NotificationRule update(Long id, String ruleName, String eventType,
                                   String channels, String recipientType,
                                   String recipientConfig, String condition,
                                   Integer priority) {
        NotificationRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("通知规则不存在: " + id));
        rule.updateRule(ruleName, eventType, condition, channels, recipientType, recipientConfig, priority);
        NotificationRule saved = ruleRepository.save(rule);
        log.info("Updated notification rule: id={}, name={}", id, ruleName);
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        ruleRepository.deleteById(id);
        log.info("Deleted notification rule: id={}", id);
    }

    @Transactional(readOnly = true)
    public NotificationRule findById(Long id) {
        return ruleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("通知规则不存在: " + id));
    }

    @Transactional(readOnly = true)
    public List<NotificationRule> findByProjectId(Long projectId) {
        return ruleRepository.findByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public List<NotificationRule> findAllEnabled() {
        return ruleRepository.findAllEnabled();
    }

    // ========== Enable / Disable ==========

    @Transactional
    public NotificationRule enable(Long id) {
        NotificationRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("通知规则不存在: " + id));
        rule.enable();
        NotificationRule saved = ruleRepository.save(rule);
        log.info("Enabled notification rule: id={}, name={}", id, rule.getRuleName());
        return saved;
    }

    @Transactional
    public NotificationRule disable(Long id) {
        NotificationRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("通知规则不存在: " + id));
        rule.disable();
        NotificationRule saved = ruleRepository.save(rule);
        log.info("Disabled notification rule: id={}, name={}", id, rule.getRuleName());
        return saved;
    }
}
