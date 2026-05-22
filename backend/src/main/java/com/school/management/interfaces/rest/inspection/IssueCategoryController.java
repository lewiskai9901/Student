package com.school.management.interfaces.rest.inspection;

import com.school.management.application.inspection.IssueCategoryApplicationService;
import com.school.management.common.result.Result;
import com.school.management.domain.inspection.model.corrective.IssueCategory;
import com.school.management.infrastructure.casbin.CasbinAccess;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inspection/issue-categories")
public class IssueCategoryController {

    private final IssueCategoryApplicationService service;

    public IssueCategoryController(IssueCategoryApplicationService service) {
        this.service = service;
    }

    @GetMapping
    @CasbinAccess(resource = "insp:platform", action = "view")
    public Result<List<IssueCategory>> list() {
        return Result.success(service.getAll());
    }

    @GetMapping("/roots")
    @CasbinAccess(resource = "insp:platform", action = "view")
    public Result<List<IssueCategory>> roots() {
        return Result.success(service.getRoots());
    }

    @GetMapping("/{id}")
    @CasbinAccess(resource = "insp:platform", action = "view")
    public Result<IssueCategory> getById(@PathVariable Long id) {
        return Result.success(service.getById(id));
    }

    @GetMapping("/{id}/children")
    @CasbinAccess(resource = "insp:platform", action = "view")
    public Result<List<IssueCategory>> children(@PathVariable Long id) {
        return Result.success(service.getChildren(id));
    }

    @PostMapping
    @CasbinAccess(resource = "insp:platform", action = "manage")
    public Result<IssueCategory> create(@RequestBody @Valid CreateIssueCategoryRequest body) {
        IssueCategory result = service.create(
                body.getCategoryCode(),
                body.getCategoryName(),
                body.getParentId(),
                body.getDescription(),
                body.getIcon(),
                body.getSortOrder() != null ? body.getSortOrder() : 0,
                null
        );
        return Result.success(result);
    }

    @PutMapping("/{id}")
    @CasbinAccess(resource = "insp:platform", action = "manage")
    public Result<IssueCategory> update(@PathVariable Long id,
                                        @RequestBody @Valid UpdateIssueCategoryRequest body) {
        IssueCategory result = service.update(id,
                body.getCategoryName(),
                body.getDescription(),
                body.getIcon(),
                body.getSortOrder(),
                body.getIsEnabled(),
                null
        );
        return Result.success(result);
    }

    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "insp:platform", action = "manage")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.success(null);
    }

    // ========== Request DTOs ==========

    @Data
    public static class CreateIssueCategoryRequest {
        @NotBlank
        private String categoryCode;
        @NotBlank
        private String categoryName;
        private Long parentId;
        private String description;
        private String icon;
        private Integer sortOrder;
    }

    @Data
    public static class UpdateIssueCategoryRequest {
        @NotBlank
        private String categoryName;
        private String description;
        private String icon;
        private Integer sortOrder;
        private Boolean isEnabled;
    }
}
