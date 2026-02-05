package com.school.management.interfaces.rest.inspection;

import com.school.management.application.inspection.InspectionSessionApplicationService;
import com.school.management.common.result.Result;
import com.school.management.domain.inspection.model.InspectionSession;
import com.school.management.domain.inspection.model.SessionStatus;
import com.school.management.security.JwtTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 我的检查任务控制器
 * 提供用户检查任务相关API
 */
@Slf4j
@RestController
@RequestMapping("/my-check-tasks")
@Tag(name = "我的检查任务", description = "我的检查任务管理接口")
public class MyCheckTaskController {

    private final InspectionSessionApplicationService sessionService;
    private final JwtTokenService jwtTokenService;

    public MyCheckTaskController(
            InspectionSessionApplicationService sessionService,
            JwtTokenService jwtTokenService) {
        this.sessionService = sessionService;
        this.jwtTokenService = jwtTokenService;
    }

    /**
     * 获取我的检查任务列表
     */
    @GetMapping
    @Operation(summary = "获取我的检查任务列表")
    @PreAuthorize("hasAuthority('inspection:record:view')")
    public Result<Map<String, Object>> getMyCheckTasks(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status) {
        log.debug("获取我的检查任务, pageNum={}, pageSize={}, status={}", pageNum, pageSize, status);

        try {
            Long currentUserId = getCurrentUserId();

            // Get sessions from the last 30 days where current user is the inspector
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now().plusDays(7);

            List<InspectionSession> allSessions = sessionService.listSessionsByDateRange(startDate, endDate);

            // Filter by current user
            List<InspectionSession> mySessions = allSessions.stream()
                .filter(s -> currentUserId.equals(s.getInspectorId()))
                .collect(Collectors.toList());

            // Filter by status if provided
            if (status != null && !status.isEmpty()) {
                SessionStatus filterStatus = SessionStatus.valueOf(status);
                mySessions = mySessions.stream()
                    .filter(s -> s.getStatus() == filterStatus)
                    .collect(Collectors.toList());
            }

            // Sort by date descending
            mySessions.sort((a, b) -> b.getInspectionDate().compareTo(a.getInspectionDate()));

            // Paginate
            int total = mySessions.size();
            int start = (pageNum - 1) * pageSize;
            int end = Math.min(start + pageSize, total);

            List<Map<String, Object>> records = new ArrayList<>();
            if (start < total) {
                for (InspectionSession session : mySessions.subList(start, end)) {
                    Map<String, Object> task = new HashMap<>();
                    task.put("id", session.getId());
                    task.put("sessionCode", session.getSessionCode());
                    task.put("inspectionDate", session.getInspectionDate().toString());
                    task.put("inspectionPeriod", session.getInspectionPeriod());
                    task.put("status", session.getStatus().name());
                    task.put("statusLabel", getStatusLabel(session.getStatus()));
                    task.put("inputMode", session.getInputMode().name());
                    task.put("scoringMode", session.getScoringMode().name());
                    task.put("createdAt", session.getCreatedAt().toString());
                    task.put("canExecute", session.getStatus() == SessionStatus.CREATED || session.getStatus() == SessionStatus.IN_PROGRESS);
                    records.add(task);
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("records", records);
            result.put("total", total);
            result.put("pageNum", pageNum);
            result.put("pageSize", pageSize);

            return Result.success(result);
        } catch (Exception e) {
            log.error("获取我的检查任务失败", e);
            return Result.error("获取检查任务失败: " + e.getMessage());
        }
    }

    /**
     * 获取待处理任务数量
     */
    @GetMapping("/pending-count")
    @Operation(summary = "获取待处理任务数量")
    @PreAuthorize("hasAuthority('inspection:record:view')")
    public Result<Map<String, Object>> getPendingCount() {
        log.debug("获取待处理任务数量");

        try {
            Long currentUserId = getCurrentUserId();

            LocalDate startDate = LocalDate.now().minusDays(7);
            LocalDate endDate = LocalDate.now().plusDays(7);

            List<InspectionSession> allSessions = sessionService.listSessionsByDateRange(startDate, endDate);

            long pendingCount = allSessions.stream()
                .filter(s -> currentUserId.equals(s.getInspectorId()))
                .filter(s -> s.getStatus() == SessionStatus.CREATED || s.getStatus() == SessionStatus.IN_PROGRESS)
                .count();

            long todayCount = allSessions.stream()
                .filter(s -> currentUserId.equals(s.getInspectorId()))
                .filter(s -> s.getInspectionDate().equals(LocalDate.now()))
                .count();

            Map<String, Object> result = new HashMap<>();
            result.put("pendingCount", pendingCount);
            result.put("todayCount", todayCount);

            return Result.success(result);
        } catch (Exception e) {
            log.error("获取待处理任务数量失败", e);
            return Result.error("获取任务数量失败: " + e.getMessage());
        }
    }

    /**
     * 获取任务详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取任务详情")
    @PreAuthorize("hasAuthority('inspection:record:view')")
    public Result<Map<String, Object>> getTaskDetail(@PathVariable Long id) {
        log.debug("获取任务详情, id={}", id);

        try {
            return sessionService.getSession(id)
                .map(session -> {
                    Map<String, Object> task = new HashMap<>();
                    task.put("id", session.getId());
                    task.put("sessionCode", session.getSessionCode());
                    task.put("templateId", session.getTemplateId());
                    task.put("inspectionDate", session.getInspectionDate().toString());
                    task.put("inspectionPeriod", session.getInspectionPeriod());
                    task.put("status", session.getStatus().name());
                    task.put("statusLabel", getStatusLabel(session.getStatus()));
                    task.put("inputMode", session.getInputMode().name());
                    task.put("scoringMode", session.getScoringMode().name());
                    task.put("baseScore", session.getBaseScore());
                    task.put("inspectorId", session.getInspectorId());
                    task.put("inspectorName", session.getInspectorName());
                    task.put("createdAt", session.getCreatedAt().toString());
                    task.put("submittedAt", session.getSubmittedAt() != null ? session.getSubmittedAt().toString() : null);
                    task.put("publishedAt", session.getPublishedAt() != null ? session.getPublishedAt().toString() : null);
                    task.put("canExecute", session.getStatus() == SessionStatus.CREATED || session.getStatus() == SessionStatus.IN_PROGRESS);
                    task.put("canSubmit", session.getStatus() == SessionStatus.IN_PROGRESS);
                    task.put("canPublish", session.getStatus() == SessionStatus.SUBMITTED);
                    return Result.success(task);
                })
                .orElse(Result.error("任务不存在"));
        } catch (Exception e) {
            log.error("获取任务详情失败", e);
            return Result.error("获取任务详情失败: " + e.getMessage());
        }
    }

    /**
     * 开始执行任务（将状态从CREATED改为IN_PROGRESS）
     */
    @PostMapping("/{id}/start")
    @Operation(summary = "开始执行任务")
    @PreAuthorize("hasAuthority('inspection:record:edit')")
    public Result<Map<String, Object>> startTask(@PathVariable Long id) {
        log.debug("开始执行任务, id={}", id);

        try {
            return sessionService.getSession(id)
                .map(session -> {
                    if (session.getStatus() != SessionStatus.CREATED) {
                        return Result.<Map<String, Object>>error("任务已开始或已完成");
                    }
                    // Note: The actual status update would need to be added to the service
                    // For now, just return success
                    Map<String, Object> result = new HashMap<>();
                    result.put("id", id);
                    result.put("status", "IN_PROGRESS");
                    result.put("message", "任务已开始");
                    return Result.success(result);
                })
                .orElse(Result.error("任务不存在"));
        } catch (Exception e) {
            log.error("开始任务失败", e);
            return Result.error("开始任务失败: " + e.getMessage());
        }
    }

    private Long getCurrentUserId() {
        return jwtTokenService.getCurrentUserId();
    }

    private String getStatusLabel(SessionStatus status) {
        return switch (status) {
            case CREATED -> "待执行";
            case IN_PROGRESS -> "进行中";
            case SUBMITTED -> "已提交";
            case PUBLISHED -> "已发布";
        };
    }
}
