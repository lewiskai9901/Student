package com.school.management.application.inspection.export;

import com.school.management.domain.inspection.model.ClassInspectionRecord;
import com.school.management.domain.inspection.repository.ClassInspectionRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RatingReportExportStrategy implements ExportStrategy {

    private final ClassInspectionRecordRepository recordRepository;

    @Override
    public ExportScenario getScenario() {
        return ExportScenario.RATING_REPORT;
    }

    @Override
    public ExportResult execute(ExportRequest request) {
        log.info("执行评级报表导出: {} ~ {}", request.getStartDate(), request.getEndDate());

        List<ClassInspectionRecord> records = recordRepository.findByClassIdAndDateRange(
                null, request.getStartDate(), request.getEndDate());

        String fileName = String.format("评级报表_%s_%s.xlsx",
                request.getStartDate(), request.getEndDate());

        // TODO: Actual Excel generation with rating calculations
        log.info("评级报表导出完成: {} 个班级记录", records.size());

        return ExportResult.builder()
                .fileName(fileName)
                .recordCount(records.size())
                .scenario(ExportScenario.RATING_REPORT)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    @Override
    public long estimateRecordCount(ExportRequest request) {
        return recordRepository.findByClassIdAndDateRange(
                null, request.getStartDate(), request.getEndDate()).size();
    }
}
