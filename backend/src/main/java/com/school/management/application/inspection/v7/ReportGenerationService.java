package com.school.management.application.inspection.v7;

import com.school.management.domain.inspection.model.v7.platform.ReportTemplate;
import com.school.management.domain.inspection.repository.v7.ReportTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Service
public class ReportGenerationService {

    private final ReportTemplateRepository templateRepository;

    // ========== Template CRUD ==========

    @Transactional
    public ReportTemplate createTemplate(Long tenantId, String templateName, String templateCode,
                                         String reportType, String formatConfig,
                                         String headerConfig, Boolean isDefault, Long createdBy) {
        ReportTemplate template = ReportTemplate.reconstruct(ReportTemplate.builder()
                .tenantId(tenantId)
                .templateName(templateName)
                .templateCode(templateCode)
                .reportType(reportType)
                .formatConfig(formatConfig)
                .headerConfig(headerConfig)
                .isDefault(isDefault)
                .isEnabled(true)
                .createdBy(createdBy));
        ReportTemplate saved = templateRepository.save(template);
        log.info("Created report template: name={}, code={}, type={}", templateName, templateCode, reportType);
        return saved;
    }

    @Transactional
    public ReportTemplate updateTemplate(Long id, String templateName, String reportType,
                                         String formatConfig, String headerConfig) {
        ReportTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("报表模板不存在: " + id));
        template.updateTemplate(templateName, reportType, formatConfig, headerConfig);
        ReportTemplate saved = templateRepository.save(template);
        log.info("Updated report template: id={}, name={}", id, templateName);
        return saved;
    }

    @Transactional
    public void deleteTemplate(Long id) {
        templateRepository.deleteById(id);
        log.info("Deleted report template: id={}", id);
    }

    @Transactional(readOnly = true)
    public ReportTemplate findById(Long id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("报表模板不存在: " + id));
    }

    @Transactional(readOnly = true)
    public List<ReportTemplate> findAll() {
        return templateRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<ReportTemplate> findByReportType(String reportType) {
        return templateRepository.findByReportType(reportType);
    }

    // ========== Report Generation (placeholder) ==========

    /**
     * 生成报表（占位实现）。
     * 实际报表生成逻辑较为复杂，当前仅返回模板元数据 + 请求参数作为 mock 数据。
     *
     * @param templateId  报表模板 ID
     * @param projectId   项目 ID
     * @param periodType  周期类型，如 "DAILY" / "WEEKLY" / "MONTHLY"
     * @param periodStart 周期开始日期
     * @return 包含模板信息和参数的 Map
     */
    @Transactional(readOnly = true)
    public Map<String, Object> generateReport(Long templateId, Long projectId,
                                              String periodType, LocalDate periodStart) {
        ReportTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("报表模板不存在: " + templateId));

        log.info("Generating report: templateId={}, projectId={}, periodType={}, periodStart={}",
                templateId, projectId, periodType, periodStart);

        Map<String, Object> result = new HashMap<>();
        result.put("templateId", template.getId());
        result.put("templateName", template.getTemplateName());
        result.put("templateCode", template.getTemplateCode());
        result.put("reportType", template.getReportType());
        result.put("formatConfig", template.getFormatConfig());
        result.put("headerConfig", template.getHeaderConfig());
        result.put("projectId", projectId);
        result.put("periodType", periodType);
        result.put("periodStart", periodStart);
        result.put("generatedAt", java.time.LocalDateTime.now());
        result.put("status", "PLACEHOLDER");
        result.put("message", "实际报表生成逻辑待实现，当前返回模板元数据与请求参数");

        return result;
    }
}
