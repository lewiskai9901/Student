package com.school.management.interfaces.rest.inspection.v7;

import com.school.management.application.inspection.v7.GradeSchemeApplicationService;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.domain.inspection.model.v7.scoring.GradeDefinition;
import com.school.management.domain.inspection.model.v7.scoring.GradeScheme;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v7/insp/grade-schemes")
@RequiredArgsConstructor
public class GradeSchemeController {

    private final GradeSchemeApplicationService gradeSchemeService;

    @GetMapping
    @CasbinAccess(resource = "insp:template", action = "view")
    public Result<List<GradeScheme>> listSchemes() {
        return Result.success(gradeSchemeService.listSchemes(0L));
    }

    @GetMapping("/{id}")
    @CasbinAccess(resource = "insp:template", action = "view")
    public Result<GradeScheme> getScheme(@PathVariable Long id) {
        return Result.success(gradeSchemeService.getScheme(id));
    }

    @PostMapping
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<GradeScheme> createScheme(@RequestBody CreateGradeSchemeRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<GradeDefinition> grades = toGradeDefinitions(request.getGrades());
        return Result.success(gradeSchemeService.createScheme(
                0L, request.getDisplayName(), request.getDescription(),
                request.getSchemeType(), grades, userId));
    }

    @PostMapping("/clone")
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<GradeScheme> cloneFromPreset(@RequestBody CloneGradeSchemeRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(gradeSchemeService.cloneFromPreset(
                request.getSourceSchemeId(), request.getDisplayName(), 0L, userId));
    }

    @PutMapping("/{id}")
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<GradeScheme> updateScheme(@PathVariable Long id,
                                             @RequestBody UpdateGradeSchemeRequest request) {
        List<GradeDefinition> grades = toGradeDefinitions(request.getGrades());
        return Result.success(gradeSchemeService.updateScheme(
                id, request.getDisplayName(), request.getDescription(), grades));
    }

    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<Void> deleteScheme(@PathVariable Long id) {
        gradeSchemeService.deleteScheme(id);
        return Result.success();
    }

    // ── Helper ─────────────────────────────────────────────

    private List<GradeDefinition> toGradeDefinitions(List<GradeDefRequest> defs) {
        if (defs == null) return null;
        return defs.stream()
                .map(g -> GradeDefinition.create(
                        null, g.getCode(), g.getName(),
                        g.getMinValue(), g.getMaxValue(),
                        g.getColor(), g.getIcon(),
                        g.getSortOrder() != null ? g.getSortOrder() : 0))
                .collect(Collectors.toList());
    }

    // ── Request DTOs ───────────────────────────────────────

    @lombok.Data
    public static class CreateGradeSchemeRequest {
        private String displayName;
        private String description;
        private String schemeType;
        private List<GradeDefRequest> grades;
    }

    @lombok.Data
    public static class CloneGradeSchemeRequest {
        private Long sourceSchemeId;
        private String displayName;
    }

    @lombok.Data
    public static class UpdateGradeSchemeRequest {
        private String displayName;
        private String description;
        private List<GradeDefRequest> grades;
    }

    @lombok.Data
    public static class GradeDefRequest {
        private String code;
        private String name;
        private BigDecimal minValue;
        private BigDecimal maxValue;
        private String color;
        private String icon;
        private Integer sortOrder;
    }
}
