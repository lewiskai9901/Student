package com.school.management.application.inspection.export;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ExportRequest {
    private ExportScenario scenario;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long sessionId;
    private List<Long> classIds;
    private List<Long> categoryIds;
    private String format; // EXCEL, PDF
}
