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
public class DeductionDetailExportStrategy implements ExportStrategy {

    private final ClassInspectionRecordRepository recordRepository;

    @Override
    public ExportScenario getScenario() {
        return ExportScenario.DEDUCTION_DETAIL;
    }

    @Override
    public ExportResult execute(ExportRequest request) {
        log.info("执行扣分明细导出: {} ~ {}", request.getStartDate(), request.getEndDate());

        List<ClassInspectionRecord> records = loadRecords(request);
        int totalDeductions = records.stream()
                .mapToInt(r -> r.getDeductions() != null ? r.getDeductions().size() : 0)
                .sum();

        // Generate filename
        String fileName = String.format("扣分明细_%s_%s.xlsx",
                request.getStartDate(), request.getEndDate());

        // TODO: Actual Excel generation via Apache POI
        log.info("扣分明细导出完成: {} 条记录, {} 条扣分明细", records.size(), totalDeductions);

        return ExportResult.builder()
                .fileName(fileName)
                .recordCount(totalDeductions)
                .scenario(ExportScenario.DEDUCTION_DETAIL)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    @Override
    public long estimateRecordCount(ExportRequest request) {
        return loadRecords(request).stream()
                .mapToLong(r -> r.getDeductions() != null ? r.getDeductions().size() : 0)
                .sum();
    }

    private List<ClassInspectionRecord> loadRecords(ExportRequest request) {
        if (request.getSessionId() != null) {
            return recordRepository.findBySessionId(request.getSessionId());
        }
        return recordRepository.findByClassIdAndDateRange(
                null, request.getStartDate(), request.getEndDate());
    }
}
