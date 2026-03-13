package com.school.management.interfaces.rest.inspection.v7;

import com.school.management.application.inspection.v7.IssueCategoryApplicationService;
import com.school.management.common.result.Result;
import com.school.management.domain.inspection.model.v7.corrective.IssueCategory;
import com.school.management.infrastructure.casbin.CasbinAccess;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v7/insp/issue-categories")
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
    public Result<IssueCategory> create(@RequestBody Map<String, Object> body) {
        IssueCategory result = service.create(
                (String) body.get("categoryCode"),
                (String) body.get("categoryName"),
                body.get("parentId") != null ? Long.valueOf(body.get("parentId").toString()) : null,
                (String) body.get("description"),
                (String) body.get("icon"),
                body.get("sortOrder") != null ? Integer.valueOf(body.get("sortOrder").toString()) : 0,
                null
        );
        return Result.success(result);
    }

    @PutMapping("/{id}")
    @CasbinAccess(resource = "insp:platform", action = "manage")
    public Result<IssueCategory> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        IssueCategory result = service.update(id,
                (String) body.get("categoryName"),
                (String) body.get("description"),
                (String) body.get("icon"),
                body.get("sortOrder") != null ? Integer.valueOf(body.get("sortOrder").toString()) : null,
                body.get("isEnabled") != null ? Boolean.valueOf(body.get("isEnabled").toString()) : null,
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
}
