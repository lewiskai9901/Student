package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.platform.WebhookSubscription;

import java.util.List;
import java.util.Optional;

public interface WebhookSubscriptionRepository {

    WebhookSubscription save(WebhookSubscription webhookSubscription);

    Optional<WebhookSubscription> findById(Long id);

    List<WebhookSubscription> findByProjectId(Long projectId);

    List<WebhookSubscription> findByEventType(String eventType);

    List<WebhookSubscription> findAllEnabled();

    void deleteById(Long id);
}
