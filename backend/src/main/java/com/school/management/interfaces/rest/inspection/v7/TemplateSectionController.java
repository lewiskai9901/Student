package com.school.management.interfaces.rest.inspection.v7;

import com.school.management.application.inspection.v7.TemplateSectionApplicationService;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.domain.inspection.model.v7.template.TemplateSection;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v7/insp/templates/{templateId}/sections")
@RequiredArgsConstructor
public class TemplateSectionController {

    private final TemplateSectionApplicationService sectionService;

    @PostMapping
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<TemplateSection> createSection(@PathVariable Long templateId,
                                                  @RequestBody CreateSectionRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(sectionService.createSection(
                templateId, request.getSectionCode(), request.getSectionName(),
                request.getWeight(),
                request.getIsRepeatable(), request.getConditionLogic(),
                request.getSortOrder(), userId));
    }

    @GetMapping
    @CasbinAccess(resource = "insp:template", action = "view")
    public Result<List<TemplateSection>> listSections(@PathVariable Long templateId) {
        return Result.success(sectionService.listSections(templateId));
    }

    @PutMapping("/{sectionId}")
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<TemplateSection> updateSection(@PathVariable Long templateId,
                                                  @PathVariable Long sectionId,
                                                  @RequestBody UpdateSectionRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(sectionService.updateSection(sectionId,
                request.getSectionName(),
                request.getWeight(), request.getIsRepeatable(),
                request.getConditionLogic(), userId));
    }

    @DeleteMapping("/{sectionId}")
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<Void> deleteSection(@PathVariable Long templateId,
                                       @PathVariable Long sectionId) {
        sectionService.deleteSection(sectionId);
        return Result.success();
    }

    @PutMapping("/reorder")
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<Void> reorderSections(@PathVariable Long templateId,
                                         @RequestBody List<Long> sectionIds) {
        sectionService.reorderSections(templateId, sectionIds);
        return Result.success();
    }

    @lombok.Data
    public static class CreateSectionRequest {
        private String sectionCode;
        private String sectionName;
        private Integer weight;
        private Boolean isRepeatable;
        private String conditionLogic;
        private Integer sortOrder;
    }

    @lombok.Data
    public static class UpdateSectionRequest {
        private String sectionName;
        private Integer weight;
        private Boolean isRepeatable;
        private String conditionLogic;
    }
}
