package com.school.management.interfaces.rest.behavior;

import com.school.management.application.behavior.BehaviorApplicationService;
import com.school.management.common.result.Result;
import com.school.management.domain.behavior.model.BehaviorAlert;
import com.school.management.security.JwtTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v2/behavior-alerts")
@Tag(name = "Behavior Alerts", description = "学生行为预警管理")
public class BehaviorAlertController {

    private final BehaviorApplicationService service;
    private final JwtTokenService jwtTokenService;

    public BehaviorAlertController(BehaviorApplicationService service,
                                    JwtTokenService jwtTokenService) {
        this.service = service;
        this.jwtTokenService = jwtTokenService;
    }

    @GetMapping("/class/{classId}")
    @Operation(summary = "获取班级预警列表")
    @PreAuthorize("hasAuthority('behavior:alert:view')")
    public Result<List<BehaviorAlertResponse>> listByClass(@PathVariable Long classId) {
        return Result.success(service.listAlertsByClassId(classId).stream()
                .map(BehaviorAlertResponse::fromDomain)
                .collect(Collectors.toList()));
    }

    @GetMapping("/student/{studentId}")
    @Operation(summary = "获取学生预警列表")
    @PreAuthorize("hasAuthority('behavior:alert:view')")
    public Result<List<BehaviorAlertResponse>> listByStudent(@PathVariable Long studentId) {
        return Result.success(service.listAlertsByStudentId(studentId).stream()
                .map(BehaviorAlertResponse::fromDomain)
                .collect(Collectors.toList()));
    }

    @GetMapping("/class/{classId}/unhandled-count")
    @Operation(summary = "获取班级未处理预警数")
    @PreAuthorize("hasAuthority('behavior:alert:view')")
    public Result<Long> unhandledCount(@PathVariable Long classId) {
        return Result.success(service.countUnhandledAlerts(classId));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "标记预警已读")
    @PreAuthorize("hasAuthority('behavior:alert:handle')")
    public Result<BehaviorAlertResponse> markRead(@PathVariable Long id) {
        BehaviorAlert alert = service.markAlertRead(id);
        return Result.success(BehaviorAlertResponse.fromDomain(alert));
    }

    @PutMapping("/{id}/handle")
    @Operation(summary = "处理预警")
    @PreAuthorize("hasAuthority('behavior:alert:handle')")
    public Result<BehaviorAlertResponse> handle(@PathVariable Long id, @RequestBody Map<String, String> body) {
        BehaviorAlert alert = service.handleAlert(id, jwtTokenService.getCurrentUserId(), body.get("note"));
        return Result.success(BehaviorAlertResponse.fromDomain(alert));
    }
}
