package com.school.management.interfaces.rest.teaching;

import com.school.management.application.teaching.ConflictDetectionService;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.domain.teaching.model.scheduling.ScheduleConflictRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/teaching/conflicts")
@RequiredArgsConstructor
public class ConflictController {
    private final ConflictDetectionService service;

    @PostMapping("/feasibility-check")
    public Result<Map<String, Object>> feasibilityCheck(@RequestParam Long semesterId) {
        return Result.success(service.feasibilityCheck(semesterId));
    }

    @PostMapping("/detect")
    public Result<List<ScheduleConflictRecord>> detect(@RequestParam Long semesterId) {
        return Result.success(service.detectConflicts(semesterId));
    }

    @GetMapping
    public Result<List<ScheduleConflictRecord>> list(
            @RequestParam Long semesterId,
            @RequestParam(required = false) Integer status) {
        return Result.success(service.listConflicts(semesterId, status));
    }

    @PostMapping("/{id}/resolve")
    public Result<Void> resolve(@PathVariable Long id, @RequestBody Map<String, String> body) {
        service.resolveConflict(id, body.get("note"), SecurityUtils.getCurrentUserId());
        return Result.success(null);
    }

    @PostMapping("/{id}/ignore")
    public Result<Void> ignore(@PathVariable Long id, @RequestBody Map<String, String> body) {
        service.ignoreConflict(id, body.get("note"), SecurityUtils.getCurrentUserId());
        return Result.success(null);
    }
}
