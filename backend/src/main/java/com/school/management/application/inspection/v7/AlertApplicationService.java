package com.school.management.application.inspection.v7;

import com.school.management.domain.inspection.model.v7.analytics.Alert;
import com.school.management.domain.inspection.model.v7.analytics.AlertRule;
import com.school.management.domain.inspection.repository.v7.AlertRepository;
import com.school.management.domain.inspection.repository.v7.AlertRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class AlertApplicationService {

    private final AlertRuleRepository alertRuleRepository;
    private final AlertRepository alertRepository;

    // ========== Alert Rules ==========

    @Transactional
    public AlertRule createAlertRule(String ruleName, String metricType, String thresholdConfig,
                                     String severity, String notificationChannels,
                                     Long projectId, Long createdBy) {
        AlertRule rule = AlertRule.create(ruleName, metricType, thresholdConfig,
                severity, notificationChannels, projectId, createdBy);
        AlertRule saved = alertRuleRepository.save(rule);
        log.info("Created alert rule '{}' (metricType={})", ruleName, metricType);
        return saved;
    }

    @Transactional
    public AlertRule updateAlertRule(Long id, String ruleName, String metricType, String thresholdConfig,
                                     String severity, String notificationChannels, Long projectId) {
        AlertRule rule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("告警规则不存在: " + id));
        rule.updateRule(ruleName, metricType, thresholdConfig, severity, notificationChannels, projectId);
        AlertRule saved = alertRuleRepository.save(rule);
        log.info("Updated alert rule {}", id);
        return saved;
    }

    @Transactional
    public void deleteAlertRule(Long id) {
        alertRuleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("告警规则不存在: " + id));
        alertRuleRepository.deleteById(id);
        log.info("Deleted alert rule {}", id);
    }

    @Transactional(readOnly = true)
    public List<AlertRule> getAlertRules() {
        return alertRuleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<AlertRule> getEnabledAlertRules() {
        return alertRuleRepository.findEnabled();
    }

    @Transactional(readOnly = true)
    public AlertRule getAlertRule(Long id) {
        return alertRuleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("告警规则不存在: " + id));
    }

    // ========== Alerts ==========

    @Transactional
    public Alert createAlert(Long alertRuleId, Long targetId, String targetType,
                              String targetName, BigDecimal metricValue,
                              BigDecimal thresholdValue, String severity, String message) {
        Alert alert = Alert.create(alertRuleId, targetId, targetType, targetName,
                metricValue, thresholdValue, severity, message);
        Alert saved = alertRepository.save(alert);
        log.info("Created alert for rule {} target {}:{}", alertRuleId, targetType, targetId);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Alert> getAlerts(String status) {
        if (status != null && !status.isBlank()) {
            return alertRepository.findByStatus(status);
        }
        return alertRepository.findRecent(100);
    }

    @Transactional(readOnly = true)
    public List<Alert> getRecentAlerts(int limit) {
        return alertRepository.findRecent(limit);
    }

    @Transactional(readOnly = true)
    public Alert getAlert(Long id) {
        return alertRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("告警不存在: " + id));
    }

    @Transactional
    public Alert acknowledgeAlert(Long alertId, Long userId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("告警不存在: " + alertId));
        alert.acknowledge(userId);
        alertRepository.save(alert);
        log.info("Alert {} acknowledged by user {}", alertId, userId);
        return alert;
    }

    @Transactional
    public Alert resolveAlert(Long alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("告警不存在: " + alertId));
        alert.resolve();
        alertRepository.save(alert);
        log.info("Alert {} resolved", alertId);
        return alert;
    }

    @Transactional
    public Alert dismissAlert(Long alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("告警不存在: " + alertId));
        alert.dismiss();
        alertRepository.save(alert);
        log.info("Alert {} dismissed", alertId);
        return alert;
    }
}
