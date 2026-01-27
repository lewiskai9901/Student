package com.school.management.domain.analytics;

import com.school.management.domain.analytics.model.AnalyticsSnapshot;
import com.school.management.domain.analytics.model.SnapshotType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AnalyticsSnapshotRepository {
    AnalyticsSnapshot save(AnalyticsSnapshot snapshot);
    Optional<AnalyticsSnapshot> findById(Long id);
    List<AnalyticsSnapshot> findByTypeAndDate(SnapshotType type, LocalDate date);
    Optional<AnalyticsSnapshot> findLatestByTypeAndScope(SnapshotType type, String scope, Long scopeId);
    List<AnalyticsSnapshot> findByTypeAndDateRange(SnapshotType type, LocalDate startDate, LocalDate endDate);
}
