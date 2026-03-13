package com.school.management.interfaces.rest.inspection.v7;

import com.school.management.application.inspection.v7.InspTemplateApplicationService;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.domain.inspection.model.v7.execution.TargetType;
import com.school.management.domain.inspection.model.v7.template.InspTemplate;
import com.school.management.domain.inspection.model.v7.template.TemplateStatus;
import com.school.management.domain.inspection.model.v7.template.TemplateVersion;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import com.school.management.common.PageResult;
import java.util.List;

@RestController
@RequestMapping("/v7/insp/templates")
@RequiredArgsConstructor
public class InspTemplateController {

    private final InspTemplateApplicationService templateService;

    @PostMapping
    @CasbinAccess(resource = "insp:template", action = "create")
    public Result<InspTemplate> createTemplate(@RequestBody CreateTemplateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        TargetType targetType = request.getTargetType() != null
                ? TargetType.valueOf(request.getTargetType()) : null;
        InspTemplate template = templateService.createTemplate(
                request.getTemplateName(), request.getDescription(),
                request.getCatalogId(), request.getTags(),
                targetType, userId);
        return Result.success(template);
    }

    @GetMapping
    @CasbinAccess(resource = "insp:template", action = "view")
    public Result<PageResult<InspTemplate>> listTemplates(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long catalogId,
            @RequestParam(required = false) String keyword) {
        TemplateStatus ts = status != null ? TemplateStatus.valueOf(status) : null;
        return Result.success(templateService.listTemplates(page, size, ts, catalogId, keyword));
    }

    @GetMapping("/{id}")
    @CasbinAccess(resource = "insp:template", action = "view")
    public Result<InspTemplate> getTemplate(@PathVariable Long id) {
        return Result.success(templateService.getTemplate(id)
                .orElseThrow(() -> new IllegalArgumentException("模板不存在: " + id)));
    }

    @PutMapping("/{id}")
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<InspTemplate> updateTemplate(@PathVariable Long id,
                                                @RequestBody UpdateTemplateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        TargetType targetType = request.getTargetType() != null
                ? TargetType.valueOf(request.getTargetType()) : null;
        return Result.success(templateService.updateTemplate(id,
                request.getTemplateName(), request.getDescription(),
                request.getCatalogId(), request.getTags(),
                targetType, userId));
    }

    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "insp:template", action = "delete")
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        templateService.deleteTemplate(id);
        return Result.success();
    }

    @PostMapping("/{id}/publish")
    @CasbinAccess(resource = "insp:template", action = "publish")
    public Result<TemplateVersion> publishTemplate(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(templateService.publishTemplate(id, userId));
    }

    @PostMapping("/{id}/deprecate")
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<Void> deprecateTemplate(@PathVariable Long id) {
        templateService.deprecateTemplate(id);
        return Result.success();
    }

    @PostMapping("/{id}/archive")
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<Void> archiveTemplate(@PathVariable Long id) {
        templateService.archiveTemplate(id);
        return Result.success();
    }

    @PostMapping("/{id}/duplicate")
    @CasbinAccess(resource = "insp:template", action = "create")
    public Result<InspTemplate> duplicateTemplate(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(templateService.duplicateTemplate(id, userId));
    }

    @PostMapping("/{id}/export")
    @CasbinAccess(resource = "insp:template", action = "view")
    public Result<String> exportTemplate(@PathVariable Long id) {
        return Result.success(templateService.exportTemplate(id));
    }

    @GetMapping("/{id}/versions")
    @CasbinAccess(resource = "insp:template", action = "view")
    public Result<List<TemplateVersion>> listVersions(@PathVariable Long id) {
        return Result.success(templateService.listVersions(id));
    }

    @GetMapping("/{id}/versions/{version}")
    @CasbinAccess(resource = "insp:template", action = "view")
    public Result<TemplateVersion> getVersion(@PathVariable Long id,
                                               @PathVariable Integer version) {
        return Result.success(templateService.getVersion(id, version)
                .orElseThrow(() -> new IllegalArgumentException("版本不存在: " + version)));
    }

    // --- Request DTOs ---

    @lombok.Data
    public static class CreateTemplateRequest {
        private String templateName;
        private String description;
        private Long catalogId;
        private String tags;
        private String targetType;
    }

    @lombok.Data
    public static class UpdateTemplateRequest {
        private String templateName;
        private String description;
        private Long catalogId;
        private String tags;
        private String targetType;
    }
}
