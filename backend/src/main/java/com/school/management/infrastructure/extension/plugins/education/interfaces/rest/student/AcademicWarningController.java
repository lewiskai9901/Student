package com.school.management.infrastructure.extension.plugins.education.interfaces.rest.student;

import com.school.management.common.result.Result;
import com.school.management.infrastructure.casbin.CasbinAccess;
import com.school.management.infrastructure.extension.plugins.education.application.student.AcademicWarningApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 学业预警 REST Controller.
 * <p>M3.2.4 (2026-05-20): 27 处直 jdbc 已下沉到 AcademicWarningApplicationService.
 */
@Slf4j
@Tag(name = "Academic Warning", description = "学业预警管理")
@RestController
@RequestMapping("/academic-warnings")
@RequiredArgsConstructor
public class AcademicWarningController {

    private final AcademicWarningApplicationService warningService;

    // ==================== 预警规则管理 ====================

    @Operation(summary = "查询预警规则列表")
    @GetMapping("/rules")
    @CasbinAccess(resource = "student:warning", action = "view")
    public Result<List<Map<String, Object>>> listRules() {
        return Result.success(warningService.listRules());
    }

    @Operation(summary = "创建预警规则")
    @PostMapping("/rules")
    @CasbinAccess(resource = "student:warning", action = "edit")
    public Result<Long> createRule(@RequestBody Map<String, Object> data) {
        Long id = warningService.createRule(data);
        if (id == null) return Result.error("条件参数格式错误");
        return Result.success(id);
    }

    @Operation(summary = "更新预警规则")
    @PutMapping("/rules/{id}")
    @CasbinAccess(resource = "student:warning", action = "edit")
    public Result<Void> updateRule(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        if (!warningService.updateRule(id, data)) return Result.error("条件参数格式错误");
        return Result.success();
    }

    @Operation(summary = "删除预警规则")
    @DeleteMapping("/rules/{id}")
    @CasbinAccess(resource = "student:warning", action = "edit")
    public Result<Void> deleteRule(@PathVariable Long id) {
        warningService.deleteRule(id);
        return Result.success();
    }

    @Operation(summary = "启用/禁用预警规则")
    @PostMapping("/rules/{id}/toggle")
    @CasbinAccess(resource = "student:warning", action = "edit")
    public Result<Void> toggleRule(@PathVariable Long id) {
        warningService.toggleRule(id);
        return Result.success();
    }

    // ==================== 预警扫描 ====================

    @Operation(summary = "手动触发预警扫描")
    @PostMapping("/scan")
    @CasbinAccess(resource = "student:warning", action = "edit")
    public Result<Map<String, Object>> scanWarnings(@RequestParam Long semesterId) {
        return Result.success(warningService.scanWarnings(semesterId));
    }

    @Operation(summary = "预览扫描结果（不写入DB）")
    @GetMapping("/scan/preview")
    @CasbinAccess(resource = "student:warning", action = "view")
    public Result<List<Map<String, Object>>> previewScan(@RequestParam Long semesterId) {
        return Result.success(warningService.previewScan(semesterId));
    }

    // ==================== 预警记录管理 ====================

    @Operation(summary = "查询预警记录列表")
    @GetMapping
    @CasbinAccess(resource = "student:warning", action = "view")
    public Result<Map<String, Object>> listWarnings(
            @RequestParam(required = false) Integer warningLevel,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long orgUnitId,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) String warningType,
            @RequestParam(required = false) Long semesterId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(warningService.listWarnings(
            warningLevel, status, orgUnitId, studentId, warningType, semesterId, pageNum, pageSize));
    }

    @Operation(summary = "查询预警详情")
    @GetMapping("/{id}")
    @CasbinAccess(resource = "student:warning", action = "view")
    public Result<Map<String, Object>> getWarningDetail(@PathVariable Long id) {
        return Result.success(warningService.getWarningDetail(id));
    }

    @Operation(summary = "确认预警")
    @PostMapping("/{id}/confirm")
    @CasbinAccess(resource = "student:warning", action = "edit")
    public Result<Void> confirmWarning(@PathVariable Long id) {
        warningService.confirmWarning(id);
        return Result.success();
    }

    @Operation(summary = "记录干预措施")
    @PostMapping("/{id}/intervene")
    @CasbinAccess(resource = "student:warning", action = "edit")
    public Result<Void> interveneWarning(@PathVariable Long id, @RequestParam String note) {
        warningService.interveneWarning(id, note);
        return Result.success();
    }

    @Operation(summary = "解除预警")
    @PostMapping("/{id}/dismiss")
    @CasbinAccess(resource = "student:warning", action = "edit")
    public Result<Void> dismissWarning(@PathVariable Long id, @RequestParam(required = false) String note) {
        warningService.dismissWarning(id, note);
        return Result.success();
    }

    @Operation(summary = "预警统计")
    @GetMapping("/statistics")
    @CasbinAccess(resource = "student:warning", action = "view")
    public Result<Map<String, Object>> statistics(@RequestParam(required = false) Long semesterId) {
        return Result.success(warningService.statistics(semesterId));
    }

    @Operation(summary = "学生个人预警历史")
    @GetMapping("/by-student/{studentId}")
    @CasbinAccess(resource = "student:warning", action = "view")
    public Result<List<Map<String, Object>>> studentWarningHistory(@PathVariable Long studentId) {
        return Result.success(warningService.studentWarningHistory(studentId));
    }
}
