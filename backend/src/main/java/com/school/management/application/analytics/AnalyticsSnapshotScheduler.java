package com.school.management.application.analytics;

import com.school.management.domain.analytics.AnalyticsSnapshotRepository;
import com.school.management.domain.analytics.model.AnalyticsSnapshot;
import com.school.management.domain.analytics.model.SnapshotType;
import com.school.management.infrastructure.analytics.InspectionAnalyticsReadModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsSnapshotScheduler {

    private final AnalyticsSnapshotRepository snapshotRepository;
    private final InspectionAnalyticsReadModel readModel;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Daily snapshot generation at 01:30.
     */
    @Scheduled(cron = "0 30 1 * * ?")
    @Transactional
    public void generateDailySnapshots() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate monthAgo = yesterday.minusDays(30);
        log.info("Starting daily analytics snapshot generation for date: {}", yesterday);

        generateSnapshot(SnapshotType.CLASS_RANKING, null, null,
            readModel.getClassRanking(monthAgo, yesterday), yesterday);

        generateSnapshot(SnapshotType.VIOLATION_DISTRIBUTION, null, null,
            readModel.getViolationDistribution(monthAgo, yesterday), yesterday);

        generateSnapshot(SnapshotType.INSPECTOR_WORKLOAD, null, null,
            readModel.getInspectorWorkload(monthAgo, yesterday), yesterday);

        generateSnapshot(SnapshotType.DEPARTMENT_TREND, null, null,
            readModel.getDepartmentComparison(monthAgo, yesterday), yesterday);

        log.info("Daily analytics snapshot generation completed for date: {}", yesterday);
    }

    private void generateSnapshot(SnapshotType type, String scope, Long scopeId,
                                   List<Map<String, Object>> data, LocalDate date) {
        try {
            String json = objectMapper.writeValueAsString(data);
            AnalyticsSnapshot snapshot = AnalyticsSnapshot.builder()
                .snapshotType(type)
                .snapshotScope(scope)
                .scopeId(scopeId)
                .snapshotDate(date)
                .dataJson(json)
                .build();
            snapshotRepository.save(snapshot);
            log.info("Generated {} snapshot for date {}", type, date);
        } catch (Exception e) {
            log.error("Failed to generate {} snapshot: {}", type, e.getMessage(), e);
        }
    }
}
