package com.school.management.infrastructure.extension.plugins.education.interfaces.rest.teaching;

import com.school.management.common.result.Result;
import com.school.management.infrastructure.casbin.CasbinAccess;
import com.school.management.infrastructure.extension.plugins.education.application.teaching.ScheduleExportService;
import com.school.management.infrastructure.extension.plugins.education.application.teaching.scheduling.ScheduleAdjustmentApplicationService;
import com.school.management.infrastructure.extension.plugins.education.application.teaching.scheduling.ScheduleEntryApplicationService;
import com.school.management.infrastructure.extension.plugins.education.application.teaching.scheduling.ScheduleInstanceApplicationService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 排课与调课管理 REST Controller — M3.2.5 拆为 3 个应用服务后保留薄表现层
 *
 * 业务子域:
 * <ul>
 *   <li>{@link ScheduleInstanceApplicationService} — 实况课表 / 课时统计 / 代课</li>
 *   <li>{@link ScheduleAdjustmentApplicationService} — 调课申请审批执行</li>
 *   <li>{@link ScheduleEntryApplicationService} — 排课条目 / 方案 / 配置 / 自习课 (兜底)</li>
 * </ul>
 *
 * 涉及表: schedule_entries / schedule_adjustments / schedule_instances /
 *        course_schedules / system_configs / teaching_tasks / org_units 等.
 */
@Slf4j
@RestController
@RequestMapping("/teaching")
@RequiredArgsConstructor
public class TeachingScheduleController {

    private final ScheduleInstanceApplicationService instanceAppService;
    private final ScheduleAdjustmentApplicationService adjustmentAppService;
    private final ScheduleEntryApplicationService entryAppService;
    private final ScheduleExportService exportService;

    // ==================== 实况课表 ====================

    @GetMapping("/instances")
    @CasbinAccess(resource = "teaching:schedule", action = "view")
    public Result<List<Map<String, Object>>> listInstances(
            @RequestParam Long semesterId,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) Integer weekNumber,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Long orgUnitId,
            @RequestParam(required = false) Long classroomId) {
        return Result.success(instanceAppService.listInstances(semesterId, date, weekNumber, teacherId, orgUnitId, classroomId));
    }

    @PostMapping("/instances/generate")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Map<String, Object>> generateInstances(@RequestBody Map<String, Object> data) {
        if (!instanceAppService.isInstanceServiceEnabled()) return Result.error("服务未启用");
        Long semesterId = Long.valueOf(data.get("semesterId").toString());
        return Result.success(instanceAppService.generateInstances(semesterId));
    }

    @PostMapping("/instances/apply-event")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Map<String, Object>> applyEvent(@RequestBody Map<String, Object> data) {
        if (!instanceAppService.isInstanceServiceEnabled()) return Result.error("服务未启用");
        Long eventId = ((Number) data.get("eventId")).longValue();
        int affected = instanceAppService.applyCalendarEvent(eventId);
        return Result.success(Map.of("affected", affected));
    }

    // ==================== 课时统计 ====================

    @GetMapping("/statistics/hours")
    @CasbinAccess(resource = "teaching:schedule", action = "view")
    public Result<Map<String, Object>> hoursStatistics(
            @RequestParam Long semesterId,
            @RequestParam String groupBy,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) Integer weekNumber,
            @RequestParam(required = false) Integer month) {
        try {
            return Result.success(instanceAppService.hoursStatistics(semesterId, groupBy, period, weekNumber, month));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    // ==================== 代课 ====================

    @PostMapping("/instances/{id}/substitute")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Void> substituteTeacher(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        Long newTeacherId = Long.valueOf(data.get("teacherId").toString());
        String reason = (String) data.getOrDefault("reason", "代课");
        instanceAppService.substituteTeacher(id, newTeacherId, reason);
        return Result.success();
    }

    @PostMapping("/instances/{id}/cancel")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Void> cancelInstance(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        String reason = (String) data.getOrDefault("reason", "临时取消");
        instanceAppService.cancelInstance(id, reason);
        return Result.success();
    }

    @PostMapping("/instances/{id}/restore")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Void> restoreInstance(@PathVariable Long id) {
        instanceAppService.restoreInstance(id);
        return Result.success();
    }

    // ==================== 节次配置 & 数据就绪 ====================

    @GetMapping("/schedule-config")
    @CasbinAccess(resource = "teaching:schedule", action = "view")
    public Result<Map<String, Object>> getScheduleConfig(@RequestParam Long semesterId) {
        return Result.success(entryAppService.getScheduleConfig(semesterId));
    }

    @PutMapping("/schedule-config")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Void> saveScheduleConfig(@RequestBody Map<String, Object> data) {
        boolean ok = entryAppService.saveScheduleConfig(data);
        return ok ? Result.success() : Result.error("保存失败");
    }

    @GetMapping("/schedule-readiness")
    @CasbinAccess(resource = "teaching:schedule", action = "view")
    public Result<Map<String, Object>> checkReadiness(@RequestParam Long semesterId) {
        return Result.success(entryAppService.checkReadiness(semesterId));
    }

    // ==================== 排课方案 (CourseSchedule) ====================

    @GetMapping("/schedule-plans")
    @CasbinAccess(resource = "teaching:schedule", action = "view")
    public Result<List<Map<String, Object>>> listSchedulePlans(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) Integer status) {
        return Result.success(entryAppService.listSchedulePlans(semesterId, status));
    }

    @GetMapping("/schedule-plans/{id}")
    @CasbinAccess(resource = "teaching:schedule", action = "view")
    public Result<Map<String, Object>> getSchedulePlan(@PathVariable Long id) {
        return Result.success(entryAppService.getSchedulePlan(id));
    }

    @PostMapping("/schedule-plans")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Map<String, Object>> createSchedulePlan(@RequestBody Map<String, Object> data) {
        return Result.success(entryAppService.createSchedulePlan(data));
    }

    @PutMapping("/schedule-plans/{id}")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Void> updateSchedulePlan(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        entryAppService.updateSchedulePlan(id, data);
        return Result.success();
    }

    @DeleteMapping("/schedule-plans/{id}")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Void> deleteSchedulePlan(@PathVariable Long id) {
        entryAppService.deleteSchedulePlan(id);
        return Result.success();
    }

    @PostMapping("/schedule-plans/{id}/publish")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Void> publishSchedulePlan(@PathVariable Long id) {
        entryAppService.publishSchedulePlan(id);
        return Result.success();
    }

    @PostMapping("/schedule-plans/{id}/archive")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Void> archiveSchedulePlan(@PathVariable Long id) {
        entryAppService.archiveSchedulePlan(id);
        return Result.success();
    }

    // ==================== 课表条目 ====================

    @GetMapping("/schedules")
    @CasbinAccess(resource = "teaching:schedule", action = "view")
    public Result<List<Map<String, Object>>> listSchedules(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) Integer status) {
        return Result.success(entryAppService.listSchedules(semesterId, status));
    }

    @GetMapping("/schedules/{id}")
    @CasbinAccess(resource = "teaching:schedule", action = "view")
    public Result<Map<String, Object>> getSchedule(@PathVariable Long id) {
        return Result.success(entryAppService.getSchedule(id));
    }

    @PostMapping("/schedules")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Map<String, Object>> createSchedule(@RequestBody Map<String, Object> data) {
        return Result.success(entryAppService.createSchedule(data));
    }

    @PutMapping("/schedules/{id}")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Void> updateSchedule(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        entryAppService.updateSchedule(id, data);
        return Result.success();
    }

    @DeleteMapping("/schedules/{id}")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Void> deleteSchedule(@PathVariable Long id) {
        entryAppService.deleteSchedule(id);
        return Result.success();
    }

    /** 锁定/解锁排课条目，锁定后自动排课不会覆盖 */
    @PostMapping("/schedules/{id}/toggle-lock")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Map<String, Object>> toggleLock(@PathVariable Long id) {
        return Result.success(entryAppService.toggleLock(id));
    }

    /** 批量锁定/解锁 */
    @PostMapping("/schedules/batch-lock")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Map<String, Object>> batchLock(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Number> ids = (List<Number>) body.get("ids");
        int lock = ((Number) body.getOrDefault("lock", 1)).intValue();
        return Result.success(entryAppService.batchLock(ids, lock));
    }

    /**
     * 重置排课：清除排课条目 + 重置任务排课状态。已锁定的条目不会被清除。
     */
    @PostMapping("/schedules/reset")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Map<String, Object>> resetSchedule(@RequestBody Map<String, Object> body) {
        Long semesterId = ((Number) body.get("semesterId")).longValue();
        boolean keepLocked = body.get("keepLocked") == null || Boolean.TRUE.equals(body.get("keepLocked"));
        return Result.success(entryAppService.resetSchedule(semesterId, keepLocked));
    }

    @GetMapping("/schedule-teachers")
    @CasbinAccess(resource = "teaching:schedule", action = "view")
    public Result<List<Map<String, Object>>> getTeachersGroupedByDept() {
        return Result.success(entryAppService.getTeachersGroupedByDept());
    }

    @GetMapping("/schedules/by-class/{classId}")
    @CasbinAccess(resource = "teaching:schedule", action = "view")
    public Result<List<Map<String, Object>>> getSchedulesByClass(
            @PathVariable("classId") Long orgUnitId,
            @RequestParam(required = false) Long semesterId) {
        return Result.success(entryAppService.getSchedulesByClass(orgUnitId, semesterId));
    }

    @GetMapping("/schedules/by-teacher/{teacherId}")
    @CasbinAccess(resource = "teaching:schedule", action = "view")
    public Result<List<Map<String, Object>>> getSchedulesByTeacher(
            @PathVariable Long teacherId,
            @RequestParam(required = false) Long semesterId) {
        return Result.success(entryAppService.getSchedulesByTeacher(teacherId, semesterId));
    }

    @GetMapping("/schedules/by-classroom/{classroomId}")
    @CasbinAccess(resource = "teaching:schedule", action = "view")
    public Result<List<Map<String, Object>>> getSchedulesByClassroom(
            @PathVariable Long classroomId,
            @RequestParam(required = false) Long semesterId) {
        return Result.success(entryAppService.getSchedulesByClassroom(classroomId, semesterId));
    }

    // ==================== 智能排课 / 拖拽移动 / 冲突检测 ====================

    @PostMapping("/schedules/auto-schedule")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Map<String, Object>> autoSchedule(@RequestBody Map<String, Object> params) {
        try {
            return Result.success(entryAppService.autoSchedule(params));
        } catch (IllegalStateException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/schedules/{id}/move")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Map<String, Object>> moveEntry(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        int newDay = Integer.parseInt(body.get("dayOfWeek").toString());
        int newPeriod = Integer.parseInt(body.get("periodStart").toString());
        Long classroomId = body.get("classroomId") != null ? Long.parseLong(body.get("classroomId").toString()) : null;
        entryAppService.moveEntry(id, newDay, newPeriod, classroomId);
        return Result.success(Map.of("success", true));
    }

    @PostMapping("/schedules/check-move-conflict")
    @CasbinAccess(resource = "teaching:schedule", action = "view")
    public Result<Map<String, Object>> checkMoveConflict(@RequestBody Map<String, Object> body) {
        Long entryId = Long.parseLong(body.get("entryId").toString());
        Long semesterId = Long.parseLong(body.get("semesterId").toString());
        int newDay = Integer.parseInt(body.get("dayOfWeek").toString());
        int newPeriod = Integer.parseInt(body.get("periodStart").toString());
        return Result.success(entryAppService.checkMoveConflict(entryId, semesterId, newDay, newPeriod));
    }

    // ==================== 课表导出 ====================

    @GetMapping("/schedules/export/class/{classId}")
    @CasbinAccess(resource = "teaching:schedule", action = "view")
    public void exportClassSchedule(
            @PathVariable Long orgUnitId,
            @RequestParam Long semesterId,
            HttpServletResponse response) throws java.io.IOException {
        byte[] data = exportService.exportClassSchedule(semesterId, orgUnitId);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=class_schedule.xlsx");
        response.getOutputStream().write(data);
        response.getOutputStream().flush();
    }

    @GetMapping("/schedules/export/teacher/{teacherId}")
    @CasbinAccess(resource = "teaching:schedule", action = "view")
    public void exportTeacherSchedule(
            @PathVariable Long teacherId,
            @RequestParam Long semesterId,
            HttpServletResponse response) throws java.io.IOException {
        byte[] data = exportService.exportTeacherSchedule(semesterId, teacherId);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=teacher_schedule.xlsx");
        response.getOutputStream().write(data);
        response.getOutputStream().flush();
    }

    // ==================== 调课管理 ====================

    @GetMapping("/adjustments")
    @CasbinAccess(resource = "teaching:schedule", action = "view")
    public Result<Map<String, Object>> listAdjustments(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(adjustmentAppService.listAdjustments(status, page, size));
    }

    @GetMapping("/adjustments/{id}")
    @CasbinAccess(resource = "teaching:schedule", action = "view")
    public Result<Map<String, Object>> getAdjustment(@PathVariable Long id) {
        return Result.success(adjustmentAppService.getAdjustment(id));
    }

    @PostMapping("/adjustments")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Map<String, Object>> createAdjustment(@RequestBody Map<String, Object> data) {
        return Result.success(adjustmentAppService.createAdjustment(data));
    }

    @PostMapping("/adjustments/{id}/approve")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Void> approveAdjustment(@PathVariable Long id) {
        adjustmentAppService.approveAdjustment(id);
        return Result.success();
    }

    @PostMapping("/adjustments/{id}/reject")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Void> rejectAdjustment(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        String comment = (String) data.get("approvalComment");
        adjustmentAppService.rejectAdjustment(id, comment);
        return Result.success();
    }

    @PostMapping("/adjustments/{id}/execute")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Void> executeAdjustment(@PathVariable Long id) {
        adjustmentAppService.executeAdjustment(id);
        return Result.success();
    }

    @PostMapping("/adjustments/{id}/cancel")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Void> cancelAdjustment(@PathVariable Long id) {
        adjustmentAppService.cancelAdjustment(id);
        return Result.success();
    }

    @GetMapping("/adjustments/my-applications")
    @CasbinAccess(resource = "teaching:schedule", action = "view")
    public Result<Map<String, Object>> myApplications(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(adjustmentAppService.myApplications(status, page, size));
    }

    @GetMapping("/adjustments/pending-approvals")
    @CasbinAccess(resource = "teaching:schedule", action = "view")
    public Result<Map<String, Object>> pendingApprovals(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(adjustmentAppService.pendingApprovals(page, size));
    }

    // ==================== 自习课填充 ====================

    /**
     * 自动填充自习课：扫描所有班级的空课位，插入 schedule_type=4 的自习课条目。
     * 连续空节次合并为一个条目。
     */
    @PostMapping("/self-study/fill")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Map<String, Object>> fillSelfStudy(@RequestBody Map<String, Object> body) {
        Long semesterId = ((Number) body.get("semesterId")).longValue();
        int maxPeriods = body.get("maxPeriods") != null ? ((Number) body.get("maxPeriods")).intValue() : 8;
        int maxWeekday = body.get("maxWeekday") != null ? ((Number) body.get("maxWeekday")).intValue() : 5;
        int startWeek = body.get("startWeek") != null ? ((Number) body.get("startWeek")).intValue() : 1;
        int endWeek = body.get("endWeek") != null ? ((Number) body.get("endWeek")).intValue()
                : entryAppService.getSemesterTeachingWeeks(semesterId);
        return Result.success(entryAppService.fillSelfStudy(semesterId, maxPeriods, maxWeekday, startWeek, endWeek));
    }

    /**
     * 清除所有自习课条目 (schedule_type=4)
     */
    @DeleteMapping("/self-study/clear")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Map<String, Object>> clearSelfStudy(@RequestParam Long semesterId) {
        return Result.success(entryAppService.clearSelfStudy(semesterId));
    }
}
