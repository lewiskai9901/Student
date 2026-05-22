package com.school.management.interfaces.rest.inspection;

import com.school.management.application.inspection.GradeSchemeApplicationService;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.domain.inspection.model.scoring.GradeDefinition;
import com.school.management.domain.inspection.model.scoring.GradeScheme;
import com.school.management.infrastructure.casbin.CasbinAccess;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/inspection/grade-schemes")
@RequiredArgsConstructor
public class GradeSchemeController {

    private final GradeSchemeApplicationService gradeSchemeService;

    /**
     * 评分方案 scope 占位 — 单租户下固定 0L (无租户隔离).
     * 将来启用多租户时改为从 TenantContext 解析.
     */
    private static final long DEFAULT_SCOPE = 0L;

    @GetMapping
    @CasbinAccess(resource = "insp:template", action = "view")
    public Result<List<GradeScheme>> listSchemes() {
        return Result.success(gradeSchemeService.listSchemes(DEFAULT_SCOPE));
    }

    @GetMapping("/{id}")
    @CasbinAccess(resource = "insp:template", action = "view")
    public Result<GradeScheme> getScheme(@PathVariable Long id) {
        return Result.success(gradeSchemeService.getScheme(id));
    }

    @PostMapping
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<GradeScheme> createScheme(@RequestBody @Valid CreateGradeSchemeRequest request) {
        Long userId = SecurityUtils.requireCurrentUserId();
        List<GradeDefinition> grades = toGradeDefinitions(request.getGrades());
        return Result.success(gradeSchemeService.createScheme(
                DEFAULT_SCOPE, request.getDisplayName(), request.getDescription(),
                request.getSchemeType(), grades, userId));
    }

    @PostMapping("/clone")
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<GradeScheme> cloneFromPreset(@RequestBody @Valid CloneGradeSchemeRequest request) {
        Long userId = SecurityUtils.requireCurrentUserId();
        return Result.success(gradeSchemeService.cloneFromPreset(
                request.getSourceSchemeId(), request.getDisplayName(), DEFAULT_SCOPE, userId));
    }

    @PutMapping("/{id}")
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<GradeScheme> updateScheme(@PathVariable Long id,
                                             @RequestBody @Valid UpdateGradeSchemeRequest request) {
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
        @NotBlank
        private String displayName;
        private String description;
        @NotBlank
        private String schemeType;
        private List<GradeDefRequest> grades;
    }

    @lombok.Data
    public static class CloneGradeSchemeRequest {
        @NotNull
        private Long sourceSchemeId;
        @NotBlank
        private String displayName;
    }

    @lombok.Data
    public static class UpdateGradeSchemeRequest {
        @NotBlank
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
