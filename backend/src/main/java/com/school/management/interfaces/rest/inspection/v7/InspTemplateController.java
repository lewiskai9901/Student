package com.school.management.interfaces.rest.inspection.v7;

import com.school.management.application.inspection.v7.InspTemplateApplicationService;
import com.school.management.common.PageResult;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.domain.inspection.model.v7.template.TemplateSection;
import com.school.management.domain.inspection.model.v7.template.TemplateStatus;
import com.school.management.domain.inspection.model.v7.template.TemplateVersion;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * V62 统一分区模型 — 根分区（模板）控制器
 *
 * API路径保持 /v7/insp/templates，但底层操作的是根分区（parentSectionId=null）。
 */
@Slf4j
@RestController
@RequestMapping("/v7/insp/templates")
@RequiredArgsConstructor
public class InspTemplateController {

    private final InspTemplateApplicationService templateService;

    @PostMapping
    @CasbinAccess(resource = "insp:template", action = "create")
    public Result<TemplateSection> createRootSection(@RequestBody CreateTemplateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        TemplateSection root = templateService.createRootSection(
                request.name(), request.description(),
                request.catalogId(), request.tags(), userId);
        return Result.success(root);
    }

    @GetMapping
    @CasbinAccess(resource = "insp:template", action = "view")
    public Result<PageResult<TemplateSection>> listRootSections(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long catalogId,
            @RequestParam(required = false) String keyword) {
        TemplateStatus ts = status != null ? TemplateStatus.valueOf(status) : null;
        return Result.success(templateService.listRootSections(page, size, ts, catalogId, keyword));
    }

    @GetMapping("/{id}")
    @CasbinAccess(resource = "insp:template", action = "view")
    public Result<TemplateSection> getRootSection(@PathVariable Long id) {
        return Result.success(templateService.getRootSection(id)
                .orElseThrow(() -> new IllegalArgumentException("根分区不存在: " + id)));
    }

    @PutMapping("/{id}")
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<TemplateSection> updateRootSection(@PathVariable Long id,
                                                      @RequestBody UpdateTemplateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(templateService.updateRootSection(id,
                request.name(), request.description(),
                request.catalogId(), request.tags(), userId));
    }

    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "insp:template", action = "delete")
    public Result<Void> deleteRootSection(@PathVariable Long id) {
        templateService.deleteRootSection(id);
        return Result.success();
    }

    @PostMapping("/{id}/publish")
    @CasbinAccess(resource = "insp:template", action = "publish")
    public Result<TemplateVersion> publishRootSection(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(templateService.publishRootSection(id, userId));
    }

    @PostMapping("/{id}/deprecate")
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<Void> deprecateRootSection(@PathVariable Long id) {
        templateService.deprecateRootSection(id);
        return Result.success();
    }

    @PostMapping("/{id}/archive")
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<Void> archiveRootSection(@PathVariable Long id) {
        templateService.archiveRootSection(id);
        return Result.success();
    }

    @PostMapping("/{id}/duplicate")
    @CasbinAccess(resource = "insp:template", action = "create")
    public Result<TemplateSection> duplicateRootSection(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(templateService.duplicateRootSection(id, userId));
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

    @GetMapping("/{id}/export")
    @CasbinAccess(resource = "insp:template", action = "view")
    public Result<String> exportRootSection(@PathVariable Long id) {
        return Result.success(templateService.exportRootSection(id));
    }

    // --- Request DTOs ---

    public record CreateTemplateRequest(
            String name,
            String description,
            Long catalogId,
            String tags
    ) {}

    public record UpdateTemplateRequest(
            String name,
            String description,
            Long catalogId,
            String tags
    ) {}
}
