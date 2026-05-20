package com.school.management.interfaces.rest.system;

import com.school.management.application.system.AuditTrailApplicationService;
import com.school.management.common.result.Result;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Audit trail query API.
 * Read-only access to the audit_trail table.
 */
@RestController("systemAuditTrailController")
@RequestMapping("/audit-trail")
@RequiredArgsConstructor
public class AuditTrailController {

    private final AuditTrailApplicationService auditTrailApplicationService;

    /**
     * Paginated query with optional filters.
     */
    @GetMapping
    @CasbinAccess(resource = "system:audit", action = "view")
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        Map<String, Object> result = auditTrailApplicationService.list(
                module, action, resourceType, operatorId, startDate, endDate, page, size);
        return Result.success(result);
    }

    /**
     * Get audit logs for a specific resource.
     */
    @GetMapping("/by-resource")
    @CasbinAccess(resource = "system:audit", action = "view")
    public Result<List<Map<String, Object>>> getByResource(
            @RequestParam String resourceType,
            @RequestParam String resourceId) {

        List<Map<String, Object>> records =
                auditTrailApplicationService.getByResource(resourceType, resourceId);
        return Result.success(records);
    }
}
