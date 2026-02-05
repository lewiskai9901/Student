package com.school.management.application.inspection.export;

import com.school.management.application.analytics.InspectionAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatisticsExportStrategy implements ExportStrategy {

    private final InspectionAnalyticsService analyticsService;

    @Override
    public ExportScenario getScenario() {
        return ExportScenario.STATISTICS_REPORT;
    }

    @Override
    public ExportResult execute(ExportRequest request) {
        log.info("执行统计报表导出: {} ~ {}", request.getStartDate(), request.getEndDate());

        List<Map<String, Object>> ranking = analyticsService.getClassRankingTrend(
                request.getStartDate(), request.getEndDate());
        List<Map<String, Object>> violations = analyticsService.getViolationDistribution(
                request.getStartDate(), request.getEndDate());

        String fileName = String.format("统计报表_%s_%s.xlsx",
                request.getStartDate(), request.getEndDate());

        int totalRecords = ranking.size() + violations.size();

        // TODO: Actual Excel generation with multiple sheets
        log.info("统计报表导出完成: {} 条统计数据", totalRecords);

        return ExportResult.builder()
                .fileName(fileName)
                .recordCount(totalRecords)
                .scenario(ExportScenario.STATISTICS_REPORT)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    @Override
    public long estimateRecordCount(ExportRequest request) {
        return 0; // Estimation not straightforward for aggregated data
    }
}
