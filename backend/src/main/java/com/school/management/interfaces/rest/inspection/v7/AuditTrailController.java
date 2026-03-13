package com.school.management.interfaces.rest.inspection.v7;

import com.school.management.application.inspection.v7.AuditTrailApplicationService;
import com.school.management.common.result.Result;
import com.school.management.domain.inspection.model.v7.platform.AuditTrailEntry;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/v7/insp/audit-trail")
@RequiredArgsConstructor
public class AuditTrailController {

    private final AuditTrailApplicationService auditTrailService;

    @GetMapping
    @CasbinAccess(resource = "insp:platform", action = "view")
    public Result<List<AuditTrailEntry>> search(
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) Long resourceId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        if (resourceType != null && resourceId != null) {
            return Result.success(auditTrailService.findByResource(resourceType, resourceId));
        }
        if (userId != null) {
            return Result.success(auditTrailService.findByUser(userId));
        }
        if (from != null && to != null) {
            return Result.success(auditTrailService.findByDateRange(from, to));
        }
        return Result.success(auditTrailService.findRecent(100));
    }

    @GetMapping("/recent")
    @CasbinAccess(resource = "insp:platform", action = "view")
    public Result<List<AuditTrailEntry>> findRecent(
            @RequestParam(defaultValue = "50") int limit) {
        return Result.success(auditTrailService.findRecent(limit));
    }
}
