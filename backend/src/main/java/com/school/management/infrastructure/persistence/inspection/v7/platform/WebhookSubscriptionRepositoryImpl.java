package com.school.management.infrastructure.persistence.inspection.v7.platform;

import com.school.management.domain.inspection.model.v7.platform.WebhookSubscription;
import com.school.management.domain.inspection.repository.v7.WebhookSubscriptionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class WebhookSubscriptionRepositoryImpl implements WebhookSubscriptionRepository {

    private final WebhookSubscriptionMapper mapper;

    public WebhookSubscriptionRepositoryImpl(WebhookSubscriptionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public WebhookSubscription save(WebhookSubscription subscription) {
        WebhookSubscriptionPO po = toPO(subscription);
        if (subscription.getId() == null) {
            mapper.insert(po);
            subscription.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return subscription;
    }

    @Override
    public Optional<WebhookSubscription> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<WebhookSubscription> findByProjectId(Long projectId) {
        return mapper.findByProjectId(projectId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<WebhookSubscription> findByEventType(String eventType) {
        return mapper.findByEventType(eventType).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<WebhookSubscription> findAllEnabled() {
        return mapper.findAllEnabled().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private WebhookSubscriptionPO toPO(WebhookSubscription d) {
        WebhookSubscriptionPO po = new WebhookSubscriptionPO();
        po.setId(d.getId());
        po.setTenantId(d.getTenantId());
        po.setProjectId(d.getProjectId());
        po.setSubscriptionName(d.getSubscriptionName());
        po.setTargetUrl(d.getTargetUrl());
        po.setSecret(d.getSecret());
        po.setEventTypes(d.getEventTypes());
        po.setIsEnabled(d.getIsEnabled());
        po.setRetryCount(d.getRetryCount());
        po.setLastTriggeredAt(d.getLastTriggeredAt());
        po.setLastStatus(d.getLastStatus());
        po.setPlatform(d.getPlatform());
        po.setMessageTemplate(d.getMessageTemplate());
        po.setCreatedAt(d.getCreatedAt());
        po.setUpdatedAt(d.getUpdatedAt());
        return po;
    }

    private WebhookSubscription toDomain(WebhookSubscriptionPO po) {
        return WebhookSubscription.reconstruct(WebhookSubscription.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .projectId(po.getProjectId())
                .subscriptionName(po.getSubscriptionName())
                .targetUrl(po.getTargetUrl())
                .secret(po.getSecret())
                .eventTypes(po.getEventTypes())
                .isEnabled(po.getIsEnabled())
                .retryCount(po.getRetryCount())
                .lastTriggeredAt(po.getLastTriggeredAt())
                .lastStatus(po.getLastStatus())
                .platform(po.getPlatform())
                .messageTemplate(po.getMessageTemplate())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt()));
    }
}
