package com.school.management.interfaces.rest.inspection.v7;

import com.school.management.application.inspection.v7.ReportGenerationService;
import com.school.management.common.result.Result;
import com.school.management.domain.inspection.model.v7.platform.ReportTemplate;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v7/insp/report-templates")
@RequiredArgsConstructor
public class ReportTemplateController {

    private final ReportGenerationService reportService;

    @PostMapping
    @CasbinAccess(resource = "insp:platform", action = "manage")
    public Result<ReportTemplate> create(@RequestBody CreateReportTemplateRequest request) {
        return Result.success(reportService.createTemplate(
                request.getTenantId(), request.getTemplateName(), request.getTemplateCode(),
                request.getReportType(), request.getFormatConfig(), request.getHeaderConfig(),
                request.getIsDefault(), request.getCreatedBy()));
    }

    @PutMapping("/{id}")
    @CasbinAccess(resource = "insp:platform", action = "manage")
    public Result<ReportTemplate> update(@PathVariable Long id,
                                          @RequestBody UpdateReportTemplateRequest request) {
        return Result.success(reportService.updateTemplate(
                id, request.getTemplateName(), request.getReportType(),
                request.getFormatConfig(), request.getHeaderConfig()));
    }

    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "insp:platform", action = "manage")
    public Result<Void> delete(@PathVariable Long id) {
        reportService.deleteTemplate(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    @CasbinAccess(resource = "insp:platform", action = "view")
    public Result<ReportTemplate> findById(@PathVariable Long id) {
        return Result.success(reportService.findById(id));
    }

    @GetMapping
    @CasbinAccess(resource = "insp:platform", action = "view")
    public Result<List<ReportTemplate>> list(@RequestParam(required = false) String reportType) {
        if (reportType != null) {
            return Result.success(reportService.findByReportType(reportType));
        }
        return Result.success(reportService.findAll());
    }

    @PostMapping("/{id}/generate")
    @CasbinAccess(resource = "insp:platform", action = "manage")
    public Result<Map<String, Object>> generate(
            @PathVariable Long id,
            @RequestParam Long projectId,
            @RequestParam String periodType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart) {
        return Result.success(reportService.generateReport(id, projectId, periodType, periodStart));
    }

    @Data
    public static class CreateReportTemplateRequest {
        private Long tenantId;
        private String templateName;
        private String templateCode;
        private String reportType;
        private String formatConfig;
        private String headerConfig;
        private Boolean isDefault;
        private Long createdBy;
    }

    @Data
    public static class UpdateReportTemplateRequest {
        private String templateName;
        private String reportType;
        private String formatConfig;
        private String headerConfig;
    }
}
