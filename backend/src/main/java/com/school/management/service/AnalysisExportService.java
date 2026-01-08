package com.school.management.service;

import com.school.management.dto.analysis.AnalysisReportDTO;

/**
 * 分析报告导出服务接口
 *
 * @author Claude
 * @since 2025-12-05
 */
public interface AnalysisExportService {

    /**
     * 导出报告为Excel
     *
     * @param report 分析报告数据
     * @return Excel文件字节数组
     */
    byte[] exportToExcel(AnalysisReportDTO report);

    /**
     * 导出报告为PDF
     *
     * @param report 分析报告数据
     * @return PDF文件字节数组
     */
    byte[] exportToPdf(AnalysisReportDTO report);
}
