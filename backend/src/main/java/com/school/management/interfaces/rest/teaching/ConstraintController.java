package com.school.management.interfaces.rest.teaching;

import com.school.management.application.teaching.ConstraintApplicationService;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.domain.teaching.model.scheduling.SchedulingConstraint;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/teaching/constraints")
@RequiredArgsConstructor
public class ConstraintController {
    private final ConstraintApplicationService service;

    @GetMapping
    @CasbinAccess(resource = "teaching:constraint", action = "view")
    public Result<List<SchedulingConstraint>> list(
            @RequestParam Long semesterId,
            @RequestParam(required = false) Integer level,
            @RequestParam(required = false) Long targetId) {
        return Result.success(service.list(semesterId, level, targetId));
    }

    @PostMapping
    @CasbinAccess(resource = "teaching:constraint", action = "edit")
    public Result<SchedulingConstraint> create(@RequestBody Map<String, Object> body) {
        return Result.success(service.create(body, SecurityUtils.getCurrentUserId()));
    }

    @PutMapping("/{id}")
    @CasbinAccess(resource = "teaching:constraint", action = "edit")
    public Result<SchedulingConstraint> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return Result.success(service.update(id, body));
    }

    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "teaching:constraint", action = "edit")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.success(null);
    }

    @PostMapping("/{id}/enable")
    @CasbinAccess(resource = "teaching:constraint", action = "edit")
    public Result<Void> enable(@PathVariable Long id) {
        service.enable(id);
        return Result.success(null);
    }

    @PostMapping("/{id}/disable")
    @CasbinAccess(resource = "teaching:constraint", action = "edit")
    public Result<Void> disable(@PathVariable Long id) {
        service.disable(id);
        return Result.success(null);
    }

    @GetMapping("/time-matrix")
    @CasbinAccess(resource = "teaching:constraint", action = "view")
    public Result<List<List<Map<String, Object>>>> getTimeMatrix(
            @RequestParam Long semesterId,
            @RequestParam(required = false) Integer level,
            @RequestParam(required = false) Long targetId) {
        return Result.success(service.getTimeMatrix(semesterId, level, targetId));
    }
}
