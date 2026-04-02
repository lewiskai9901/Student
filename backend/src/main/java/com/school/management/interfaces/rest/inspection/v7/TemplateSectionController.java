package com.school.management.interfaces.rest.inspection.v7;

import com.school.management.application.inspection.v7.TemplateSectionApplicationService;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.domain.inspection.model.v7.execution.TargetType;
import com.school.management.domain.inspection.model.v7.template.TemplateSection;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * V62 统一分区模型 — 子分区控制器
 *
 * API路径变更为 /v7/insp/sections（不再嵌套在模板下）。
 * 所有分区操作通过 parentSectionId 定位在分区树中的位置。
 */
@Slf4j
@RestController
@RequestMapping("/v7/insp/sections")
@RequiredArgsConstructor
public class TemplateSectionController {

    private final TemplateSectionApplicationService sectionService;

    @PostMapping
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<TemplateSection> createChildSection(@RequestBody CreateChildSectionRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        TargetType targetType;
        try {
            targetType = request.targetType() != null
                    ? TargetType.valueOf(request.targetType()) : null;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("无效的目标类型: " + request.targetType());
        }
        return Result.success(sectionService.createChildSection(
                request.parentSectionId(), request.sectionCode(), request.sectionName(),
                targetType, request.isRepeatable(),
                request.conditionLogic(), request.sortOrder(), userId));
    }

    @GetMapping("/{id}")
    @CasbinAccess(resource = "insp:template", action = "view")
    public Result<TemplateSection> getSection(@PathVariable Long id) {
        return Result.success(sectionService.getSection(id));
    }

    @GetMapping("/children/{parentId}")
    @CasbinAccess(resource = "insp:template", action = "view")
    public Result<List<TemplateSection>> listChildren(@PathVariable Long parentId) {
        return Result.success(sectionService.listChildren(parentId));
    }

    @GetMapping("/tree/{rootId}")
    @CasbinAccess(resource = "insp:template", action = "view")
    public Result<List<TemplateSection>> getSectionTree(@PathVariable Long rootId) {
        return Result.success(sectionService.getSectionTree(rootId));
    }

    @PutMapping("/{id}")
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<TemplateSection> updateSection(@PathVariable Long id,
                                                  @RequestBody UpdateSectionRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        TargetType targetType;
        try {
            targetType = request.targetType() != null
                    ? TargetType.valueOf(request.targetType()) : null;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("无效的目标类型: " + request.targetType());
        }
        return Result.success(sectionService.updateSection(id,
                request.sectionName(), targetType,
                request.targetSourceMode(), request.targetTypeFilter(),
                request.isRepeatable(), request.conditionLogic(),
                request.inputMode(), userId));
    }

    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<Void> deleteSection(@PathVariable Long id) {
        sectionService.deleteSection(id);
        return Result.success();
    }

    @PostMapping("/reorder")
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<Void> reorderSections(@RequestBody ReorderSectionsRequest request) {
        sectionService.reorderSections(request.parentSectionId(), request.sectionIds());
        return Result.success();
    }

    @PutMapping("/{id}/scoring-config")
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<TemplateSection> updateScoringConfig(@PathVariable Long id,
                                                        @RequestBody UpdateScoringConfigRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(sectionService.updateScoringConfig(id, request.scoringConfig(), userId));
    }

    @PutMapping("/{id}/status")
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @RequestBody UpdateStatusRequest request) {
        sectionService.updateSectionStatus(id, request.status());
        return Result.success();
    }

    // --- Request DTOs ---

    public record CreateChildSectionRequest(
            Long parentSectionId,
            String sectionCode,
            String sectionName,
            String targetType,
            Boolean isRepeatable,
            String conditionLogic,
            Integer sortOrder
    ) {}

    public record UpdateSectionRequest(
            String sectionName,
            String targetType,
            String targetSourceMode,
            String targetTypeFilter,
            Boolean isRepeatable,
            String conditionLogic,
            String inputMode
    ) {}

    public record ReorderSectionsRequest(
            Long parentSectionId,
            List<Long> sectionIds
    ) {}

    public record UpdateScoringConfigRequest(
            String scoringConfig
    ) {}

    public record UpdateStatusRequest(
            String status
    ) {}
}
