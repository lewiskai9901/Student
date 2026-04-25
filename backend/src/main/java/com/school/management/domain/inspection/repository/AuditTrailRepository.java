package com.school.management.domain.inspection.repository;

import com.school.management.domain.inspection.model.platform.AuditTrailEntry;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditTrailRepository {

    AuditTrailEntry save(AuditTrailEntry auditTrailEntry);

    List<AuditTrailEntry> findByResourceTypeAndId(String resourceType, Long resourceId);

    List<AuditTrailEntry> findByUserId(Long userId);

    List<AuditTrailEntry> findRecent(int limit);

    List<AuditTrailEntry> findByDateRange(LocalDateTime from, LocalDateTime to);
}
