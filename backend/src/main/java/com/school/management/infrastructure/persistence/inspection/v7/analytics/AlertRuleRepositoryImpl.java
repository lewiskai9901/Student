package com.school.management.infrastructure.persistence.inspection.v7.analytics;

import com.school.management.domain.inspection.model.v7.analytics.AlertRule;
import com.school.management.domain.inspection.repository.v7.AlertRuleRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class AlertRuleRepositoryImpl implements AlertRuleRepository {

    private final AlertRuleMapper mapper;

    public AlertRuleRepositoryImpl(AlertRuleMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public AlertRule save(AlertRule alertRule) {
        AlertRulePO po = toPO(alertRule);
        if (alertRule.getId() == null) {
            mapper.insert(po);
            alertRule.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return alertRule;
    }

    @Override
    public Optional<AlertRule> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<AlertRule> findAll() {
        return mapper.selectList(null).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AlertRule> findEnabled() {
        return mapper.findEnabled().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private AlertRulePO toPO(AlertRule d) {
        AlertRulePO po = new AlertRulePO();
        po.setId(d.getId());
        po.setTenantId(d.getTenantId() != null ? d.getTenantId() : 0L);
        po.setRuleName(d.getRuleName());
        po.setMetricType(d.getMetricType());
        po.setThresholdConfig(d.getThresholdConfig());
        po.setSeverity(d.getSeverity());
        po.setNotificationChannels(d.getNotificationChannels());
        po.setIsEnabled(d.getIsEnabled());
        po.setProjectId(d.getProjectId());
        po.setCreatedBy(d.getCreatedBy());
        po.setCreatedAt(d.getCreatedAt());
        po.setUpdatedAt(d.getUpdatedAt());
        return po;
    }

    private AlertRule toDomain(AlertRulePO po) {
        return AlertRule.reconstruct(AlertRule.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .ruleName(po.getRuleName())
                .metricType(po.getMetricType())
                .thresholdConfig(po.getThresholdConfig())
                .severity(po.getSeverity())
                .notificationChannels(po.getNotificationChannels())
                .isEnabled(po.getIsEnabled())
                .projectId(po.getProjectId())
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt()));
    }
}
