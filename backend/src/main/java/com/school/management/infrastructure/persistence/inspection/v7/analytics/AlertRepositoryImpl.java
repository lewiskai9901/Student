package com.school.management.infrastructure.persistence.inspection.v7.analytics;

import com.school.management.domain.inspection.model.v7.analytics.Alert;
import com.school.management.domain.inspection.repository.v7.AlertRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class AlertRepositoryImpl implements AlertRepository {

    private final AlertMapper mapper;

    public AlertRepositoryImpl(AlertMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Alert save(Alert alert) {
        AlertPO po = toPO(alert);
        if (alert.getId() == null) {
            mapper.insert(po);
            alert.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return alert;
    }

    @Override
    public Optional<Alert> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<Alert> findByStatus(String status) {
        return mapper.findByStatus(status).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Alert> findRecent(int limit) {
        return mapper.findRecent(limit).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countByStatus(String status) {
        return mapper.countByStatus(status);
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private AlertPO toPO(Alert d) {
        AlertPO po = new AlertPO();
        po.setId(d.getId());
        po.setTenantId(d.getTenantId() != null ? d.getTenantId() : 0L);
        po.setAlertRuleId(d.getAlertRuleId());
        po.setTargetId(d.getTargetId());
        po.setTargetType(d.getTargetType());
        po.setTargetName(d.getTargetName());
        po.setMetricValue(d.getMetricValue());
        po.setThresholdValue(d.getThresholdValue());
        po.setSeverity(d.getSeverity());
        po.setMessage(d.getMessage());
        po.setStatus(d.getStatus());
        po.setAcknowledgedBy(d.getAcknowledgedBy());
        po.setAcknowledgedAt(d.getAcknowledgedAt());
        po.setResolvedAt(d.getResolvedAt());
        po.setTriggeredAt(d.getTriggeredAt());
        return po;
    }

    private Alert toDomain(AlertPO po) {
        return Alert.reconstruct(Alert.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .alertRuleId(po.getAlertRuleId())
                .targetId(po.getTargetId())
                .targetType(po.getTargetType())
                .targetName(po.getTargetName())
                .metricValue(po.getMetricValue())
                .thresholdValue(po.getThresholdValue())
                .severity(po.getSeverity())
                .message(po.getMessage())
                .status(po.getStatus())
                .acknowledgedBy(po.getAcknowledgedBy())
                .acknowledgedAt(po.getAcknowledgedAt())
                .resolvedAt(po.getResolvedAt())
                .triggeredAt(po.getTriggeredAt()));
    }
}
