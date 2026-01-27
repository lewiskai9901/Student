package com.school.management.application.analytics;

import com.school.management.domain.analytics.AnalyticsSnapshotRepository;
import com.school.management.domain.analytics.model.AnalyticsSnapshot;
import com.school.management.domain.analytics.model.SnapshotType;
import com.school.management.infrastructure.analytics.InspectionAnalyticsReadModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InspectionAnalyticsService {

    private final AnalyticsSnapshotRepository snapshotRepository;
    private final InspectionAnalyticsReadModel readModel;

    public List<Map<String, Object>> getClassRankingTrend(LocalDate startDate, LocalDate endDate) {
        return readModel.getClassRanking(startDate, endDate);
    }

    public List<Map<String, Object>> getViolationDistribution(LocalDate startDate, LocalDate endDate) {
        return readModel.getViolationDistribution(startDate, endDate);
    }

    public List<Map<String, Object>> getInspectorWorkload(LocalDate startDate, LocalDate endDate) {
        return readModel.getInspectorWorkload(startDate, endDate);
    }

    public List<Map<String, Object>> getDepartmentComparison(LocalDate startDate, LocalDate endDate) {
        return readModel.getDepartmentComparison(startDate, endDate);
    }

    public Optional<AnalyticsSnapshot> getLatestSnapshot(SnapshotType type, String scope, Long scopeId) {
        return snapshotRepository.findLatestByTypeAndScope(type, scope, scopeId);
    }

    public List<AnalyticsSnapshot> getSnapshotHistory(SnapshotType type, LocalDate startDate, LocalDate endDate) {
        return snapshotRepository.findByTypeAndDateRange(type, startDate, endDate);
    }
}
