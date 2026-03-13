package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.platform.NotificationRule;

import java.util.List;
import java.util.Optional;

public interface NotificationRuleRepository {

    NotificationRule save(NotificationRule notificationRule);

    Optional<NotificationRule> findById(Long id);

    List<NotificationRule> findByProjectId(Long projectId);

    List<NotificationRule> findByEventType(String eventType);

    List<NotificationRule> findAllEnabled();

    void deleteById(Long id);
}
