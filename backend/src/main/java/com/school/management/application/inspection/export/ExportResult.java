package com.school.management.application.inspection.export;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ExportResult {
    private String fileName;
    private String filePath;
    private String downloadUrl;
    private long fileSize;
    private int recordCount;
    private ExportScenario scenario;
    private LocalDateTime generatedAt;
}
