package com.school.management.interfaces.rest.access;

import com.school.management.application.access.AccessRelationHistoryApplicationService;
import com.school.management.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 关系变更历史查询.
 */
@RestController
@RequestMapping("/access-relations/history")
@RequiredArgsConstructor
public class AccessRelationHistoryController {

    private final AccessRelationHistoryApplicationService historyService;

    /** 查某 subject (人) 的所有关系变更. */
    @GetMapping("/by-subject")
    @PreAuthorize("hasAuthority('system:audit:view')")
    public Result<List<Map<String, Object>>> bySubject(
            @RequestParam String subjectType,
            @RequestParam Long subjectId,
            @RequestParam(defaultValue = "100") int limit) {
        return Result.success(historyService.findBySubject(subjectType, subjectId, limit));
    }

    /** 查某 resource (东西) 的所有关系变更. */
    @GetMapping("/by-resource")
    @PreAuthorize("hasAuthority('system:audit:view')")
    public Result<List<Map<String, Object>>> byResource(
            @RequestParam String resourceType,
            @RequestParam Long resourceId,
            @RequestParam(defaultValue = "100") int limit) {
        return Result.success(historyService.findByResource(resourceType, resourceId, limit));
    }

    /** 最近 N 天变更 (审计页用). */
    @GetMapping("/recent")
    @PreAuthorize("hasAuthority('system:audit:view')")
    public Result<List<Map<String, Object>>> recent(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "200") int limit) {
        return Result.success(historyService.findRecent(days, limit));
    }
}
