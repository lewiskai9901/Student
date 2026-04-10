package com.school.management.interfaces.rest.teaching;

import com.school.management.application.teaching.OfferingApplicationService;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.domain.teaching.model.offering.SemesterOffering;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/teaching/offerings")
@RequiredArgsConstructor
public class OfferingController {
    private final OfferingApplicationService service;

    @GetMapping
    @CasbinAccess(resource = "teaching:offering", action = "view")
    public Result<List<Map<String, Object>>> list(@RequestParam Long semesterId) {
        return Result.success(service.listOfferingsWithCourse(semesterId));
    }

    @PostMapping
    @CasbinAccess(resource = "teaching:offering", action = "edit")
    public Result<SemesterOffering> create(@RequestBody Map<String, Object> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(service.createOffering(body, userId));
    }

    @PutMapping("/{id}")
    @CasbinAccess(resource = "teaching:offering", action = "edit")
    public Result<SemesterOffering> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return Result.success(service.updateOffering(id, body));
    }

    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "teaching:offering", action = "edit")
    public Result<Void> delete(@PathVariable Long id) {
        service.deleteOffering(id);
        return Result.success(null);
    }

    @PostMapping("/{id}/confirm")
    @CasbinAccess(resource = "teaching:offering", action = "edit")
    public Result<Void> confirm(@PathVariable Long id) {
        service.confirmOffering(id);
        return Result.success(null);
    }

    @PostMapping("/import-from-plan")
    @CasbinAccess(resource = "teaching:offering", action = "edit")
    public Result<Map<String, Object>> importFromPlan(@RequestBody Map<String, Object> body) {
        Long semesterId = Long.valueOf(body.get("semesterId").toString());
        Long planId = Long.valueOf(body.get("planId").toString());
        Long userId = SecurityUtils.getCurrentUserId();
        int count = service.importFromPlan(semesterId, planId, userId);
        return Result.success(Map.of("imported", count));
    }

    @PostMapping("/generate-tasks")
    @CasbinAccess(resource = "teaching:offering", action = "edit")
    public Result<Map<String, Object>> generateTasks(@RequestBody Map<String, Object> body) {
        Long semesterId = Long.valueOf(body.get("semesterId").toString());
        Long userId = SecurityUtils.getCurrentUserId();
        int count = service.generateTasksFromAssignments(semesterId, userId);
        return Result.success(Map.of("generated", count));
    }
}
