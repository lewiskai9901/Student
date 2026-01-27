package com.school.management.application.inspection.export;

/**
 * 导出策略接口
 */
public interface ExportStrategy {
    ExportScenario getScenario();
    ExportResult execute(ExportRequest request);
    long estimateRecordCount(ExportRequest request);
}
