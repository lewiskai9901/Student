package com.school.management.interfaces.rest.teaching;

import com.school.management.application.teaching.OfferingApplicationService;
import com.school.management.common.result.Result;
import com.school.management.domain.teaching.model.offering.ClassCourseAssignment;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/teaching/class-assignments")
@RequiredArgsConstructor
public class ClassAssignmentController {
    private final OfferingApplicationService service;

    @GetMapping
    @CasbinAccess(resource = "teaching:offering", action = "view")
    public Result<List<ClassCourseAssignment>> list(
            @RequestParam Long semesterId,
            @RequestParam(required = false) Long orgUnitId) {
        return Result.success(service.listAssignments(semesterId, orgUnitId));
    }

    @PostMapping
    @CasbinAccess(resource = "teaching:offering", action = "edit")
    public Result<ClassCourseAssignment> create(@RequestBody Map<String, Object> body) {
        return Result.success(service.createAssignment(body));
    }

    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "teaching:offering", action = "edit")
    public Result<Void> delete(@PathVariable Long id) {
        service.deleteAssignment(id);
        return Result.success(null);
    }

    @PostMapping("/batch-confirm")
    @CasbinAccess(resource = "teaching:offering", action = "edit")
    public Result<Void> batchConfirm(@RequestBody Map<String, Object> body) {
        service.batchConfirmAssignments(
            Long.parseLong(body.get("semesterId").toString()),
            Long.parseLong(body.get("orgUnitId").toString())
        );
        return Result.success(null);
    }
}
