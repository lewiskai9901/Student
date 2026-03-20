package com.school.management.interfaces.rest.inspection.v7;

import com.school.management.application.inspection.v7.TemplateModuleRefApplicationService;
import com.school.management.common.result.Result;
import com.school.management.domain.inspection.model.v7.template.TemplateModuleRef;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @deprecated V62: 模块引用功能已被统一分区模型的引用分区(refSectionId)替代。
 * 使用 TemplateSectionController 的 createRefSection 代替。
 */
@Deprecated
@RestController
@RequestMapping("/v7/insp/templates/{templateId}/module-refs")
@RequiredArgsConstructor
public class TemplateModuleRefController {

    private final TemplateModuleRefApplicationService moduleRefService;

    @PostMapping
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<TemplateModuleRef> addModuleRef(@PathVariable Long templateId,
                                                   @RequestBody AddModuleRefRequest request) {
        return Result.success(moduleRefService.addModuleRef(
                templateId, request.getModuleTemplateId(),
                request.getSortOrder(), request.getWeight()));
    }

    @GetMapping
    @CasbinAccess(resource = "insp:template", action = "view")
    public Result<List<TemplateModuleRef>> listModuleRefs(@PathVariable Long templateId) {
        return Result.success(moduleRefService.listModuleRefs(templateId));
    }

    @PutMapping("/{refId}")
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<TemplateModuleRef> updateModuleRef(@PathVariable Long templateId,
                                                      @PathVariable Long refId,
                                                      @RequestBody UpdateModuleRefRequest request) {
        return Result.success(moduleRefService.updateModuleRef(
                refId, request.getWeight(), request.getOverrideConfig()));
    }

    @DeleteMapping("/{refId}")
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<Void> removeModuleRef(@PathVariable Long templateId,
                                         @PathVariable Long refId) {
        moduleRefService.removeModuleRef(refId);
        return Result.success();
    }

    @PostMapping("/reorder")
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<Void> reorderModuleRefs(@PathVariable Long templateId,
                                           @RequestBody ReorderRequest request) {
        moduleRefService.reorderModuleRefs(templateId, request.getRefIds());
        return Result.success();
    }

    @lombok.Data
    public static class AddModuleRefRequest {
        private Long moduleTemplateId;
        private Integer sortOrder;
        private Integer weight;
    }

    @lombok.Data
    public static class UpdateModuleRefRequest {
        private Integer weight;
        private String overrideConfig;
    }

    @lombok.Data
    public static class ReorderRequest {
        private List<Long> refIds;
    }
}
