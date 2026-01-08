package com.school.management.controller;

import com.school.management.common.result.Result;
import com.school.management.dto.export.*;
import com.school.management.service.CheckExportTemplateService;
import com.school.management.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 检查导出模板控制器
 */
@Slf4j
@RestController
@RequestMapping
@RequiredArgsConstructor
public class CheckExportTemplateController {

    private final CheckExportTemplateService exportTemplateService;

    /**
     * 创建导出模板
     */
    @PostMapping("/check-plans/{planId}/export-templates")
    @PreAuthorize("hasAuthority('quantification:plan:edit')")
    public Result<ExportTemplateDTO> createTemplate(
            @PathVariable Long planId,
            @Valid @RequestBody ExportTemplateRequest request) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        ExportTemplateDTO result = exportTemplateService.createTemplate(planId, request, operatorId);
        return Result.success(result);
    }

    /**
     * 获取检查计划的导出模板列表
     */
    @GetMapping("/check-plans/{planId}/export-templates")
    @PreAuthorize("hasAuthority('quantification:plan:view')")
    public Result<List<ExportTemplateDTO>> getTemplatesByPlan(@PathVariable Long planId) {
        List<ExportTemplateDTO> templates = exportTemplateService.getTemplatesByPlanId(planId);
        return Result.success(templates);
    }

    /**
     * 获取模板详情
     */
    @GetMapping("/export-templates/{templateId}")
    @PreAuthorize("hasAuthority('quantification:plan:view')")
    public Result<ExportTemplateDTO> getTemplate(@PathVariable Long templateId) {
        ExportTemplateDTO template = exportTemplateService.getTemplate(templateId);
        return Result.success(template);
    }

    /**
     * 更新导出模板
     */
    @PutMapping("/export-templates/{templateId}")
    @PreAuthorize("hasAuthority('quantification:plan:edit')")
    public Result<ExportTemplateDTO> updateTemplate(
            @PathVariable Long templateId,
            @Valid @RequestBody ExportTemplateRequest request) {
        ExportTemplateDTO result = exportTemplateService.updateTemplate(templateId, request);
        return Result.success(result);
    }

    /**
     * 删除导出模板
     */
    @DeleteMapping("/export-templates/{templateId}")
    @PreAuthorize("hasAuthority('quantification:plan:edit')")
    public Result<Void> deleteTemplate(@PathVariable Long templateId) {
        exportTemplateService.deleteTemplate(templateId);
        return Result.success();
    }

    /**
     * 获取日常检查可用的导出模板
     */
    @GetMapping("/daily-checks/{checkId}/export-templates")
    @PreAuthorize("hasAuthority('quantification:check:view')")
    public Result<List<ExportTemplateDTO>> getTemplatesForCheck(@PathVariable Long checkId) {
        List<ExportTemplateDTO> templates = exportTemplateService.getTemplatesForCheck(checkId);
        return Result.success(templates);
    }

    /**
     * 获取导出预览
     */
    @PostMapping("/daily-checks/{checkId}/export/preview")
    @PreAuthorize("hasAuthority('quantification:check:view')")
    public Result<ExportPreviewDTO> getExportPreview(
            @PathVariable Long checkId,
            @Valid @RequestBody ExportRequest request) {
        ExportPreviewDTO preview = exportTemplateService.getExportPreview(checkId, request);
        return Result.success(preview);
    }

    /**
     * 导出文件下载
     */
    @PostMapping("/daily-checks/{checkId}/export/download")
    @PreAuthorize("hasAuthority('quantification:check:view')")
    public ResponseEntity<byte[]> exportDownload(
            @PathVariable Long checkId,
            @Valid @RequestBody ExportRequest request) {

        byte[] fileBytes = exportTemplateService.exportFile(checkId, request);
        String fileName = exportTemplateService.getExportFileName(checkId, request);

        // 确定Content-Type
        String contentType;
        if (fileName.endsWith(".pdf")) {
            contentType = "application/pdf";
        } else if (fileName.endsWith(".docx")) {
            contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        } else if (fileName.endsWith(".xlsx")) {
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        } else {
            contentType = "application/octet-stream";
        }

        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .contentType(MediaType.parseMediaType(contentType))
                .body(fileBytes);
    }
}
