package com.school.management.controller.evaluation;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.common.PageResult;
import com.school.management.common.result.Result;
import com.school.management.entity.evaluation.StudentHonorApplication;
import com.school.management.service.evaluation.StudentHonorApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 学生荣誉申报管理控制器
 *
 * @author Claude
 * @since 2025-11-28
 */
@Slf4j
@RestController
@RequestMapping("/evaluation/honor-applications")
@RequiredArgsConstructor
@Tag(name = "学生荣誉申报管理", description = "学生荣誉申报相关接口")
public class StudentHonorApplicationController {

    private final StudentHonorApplicationService applicationService;

    /**
     * 分页查询荣誉申报
     */
    @GetMapping
    @Operation(summary = "分页查询荣誉申报")
    @PreAuthorize("hasAuthority('evaluation:honor:list')")
    public Result<PageResult<Map<String, Object>>> pageApplications(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "周期ID") @RequestParam(required = false) Long periodId,
            @Parameter(description = "学生姓名") @RequestParam(required = false) String studentName,
            @Parameter(description = "学号") @RequestParam(required = false) String studentNo,
            @Parameter(description = "荣誉类型ID") @RequestParam(required = false) Long honorTypeId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "班级ID") @RequestParam(required = false) Long classId,
            @Parameter(description = "院系ID") @RequestParam(required = false) Long departmentId) {

        log.info("分页查询荣誉申报: pageNum={}, pageSize={}", pageNum, pageSize);

        Page<Map<String, Object>> page = new Page<>(pageNum, pageSize);
        Map<String, Object> query = new HashMap<>();
        query.put("periodId", periodId);
        query.put("studentName", studentName);
        query.put("studentNo", studentNo);
        query.put("honorTypeId", honorTypeId);
        query.put("status", status);
        query.put("classId", classId);
        query.put("departmentId", departmentId);

        Page<Map<String, Object>> result = applicationService.pageApplications(page, query);
        return Result.success(PageResult.from(result));
    }

    /**
     * 获取申报详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取申报详情")
    @PreAuthorize("hasAuthority('evaluation:honor:list')")
    public Result<Map<String, Object>> getApplicationDetail(
            @Parameter(description = "申报ID") @PathVariable Long id) {
        log.info("获取申报详情: id={}", id);
        Map<String, Object> detail = applicationService.getApplicationDetail(id);
        return Result.success(detail);
    }

    /**
     * 提交荣誉申报
     */
    @PostMapping
    @Operation(summary = "提交荣誉申报")
    @PreAuthorize("hasAuthority('evaluation:honor:apply')")
    public Result<Long> submitApplication(@RequestBody StudentHonorApplication application) {
        log.info("提交荣誉申报: studentId={}, honorTypeId={}",
                application.getStudentId(), application.getHonorTypeId());
        Long id = applicationService.submitApplication(application);
        return Result.success(id);
    }

    /**
     * 更新荣誉申报
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新荣誉申报")
    @PreAuthorize("hasAuthority('evaluation:honor:apply')")
    public Result<Void> updateApplication(
            @Parameter(description = "申报ID") @PathVariable Long id,
            @RequestBody StudentHonorApplication application) {
        log.info("更新荣誉申报: id={}", id);
        application.setId(id);
        applicationService.updateApplication(application);
        return Result.success();
    }

    /**
     * 撤回申报
     */
    @PostMapping("/{id}/withdraw")
    @Operation(summary = "撤回申报")
    @PreAuthorize("hasAuthority('evaluation:honor:apply')")
    public Result<Void> withdrawApplication(
            @Parameter(description = "申报ID") @PathVariable Long id) {
        log.info("撤回申报: id={}", id);
        applicationService.withdrawApplication(id);
        return Result.success();
    }

    /**
     * 班级审核
     */
    @PostMapping("/{id}/class-review")
    @Operation(summary = "班级审核")
    @PreAuthorize("hasAuthority('evaluation:honor:class-review')")
    public Result<Void> classReview(
            @Parameter(description = "申报ID") @PathVariable Long id,
            @RequestBody Map<String, Object> params) {
        log.info("班级审核: id={}", id);
        boolean approved = (Boolean) params.get("approved");
        String comment = (String) params.get("comment");
        applicationService.classReview(id, approved, comment);
        return Result.success();
    }

    /**
     * 院系审核
     */
    @PostMapping("/{id}/department-review")
    @Operation(summary = "院系审核")
    @PreAuthorize("hasAuthority('evaluation:honor:dept-review')")
    public Result<Void> departmentReview(
            @Parameter(description = "申报ID") @PathVariable Long id,
            @RequestBody Map<String, Object> params) {
        log.info("院系审核: id={}", id);
        boolean approved = (Boolean) params.get("approved");
        String comment = (String) params.get("comment");
        applicationService.departmentReview(id, approved, comment);
        return Result.success();
    }

    /**
     * 学校审核
     */
    @PostMapping("/{id}/school-review")
    @Operation(summary = "学校审核")
    @PreAuthorize("hasAuthority('evaluation:honor:school-review')")
    public Result<Void> schoolReview(
            @Parameter(description = "申报ID") @PathVariable Long id,
            @RequestBody Map<String, Object> params) {
        log.info("学校审核: id={}", id);
        boolean approved = (Boolean) params.get("approved");
        String comment = (String) params.get("comment");
        applicationService.schoolReview(id, approved, comment);
        return Result.success();
    }

    /**
     * 批量审核
     */
    @PostMapping("/batch-review")
    @Operation(summary = "批量审核")
    @PreAuthorize("hasAnyAuthority('evaluation:honor:class-review', 'evaluation:honor:dept-review', 'evaluation:honor:school-review')")
    public Result<Integer> batchReview(@RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<Long> ids = (List<Long>) params.get("ids");
        String level = (String) params.get("level");
        boolean approved = (Boolean) params.get("approved");
        String comment = (String) params.get("comment");

        log.info("批量审核: level={}, count={}", level, ids.size());
        int count = applicationService.batchReview(ids, level, approved, comment);
        return Result.success(count);
    }

    /**
     * 获取学生的申报列表
     */
    @GetMapping("/student/{studentId}")
    @Operation(summary = "获取学生的申报列表")
    @PreAuthorize("hasAuthority('evaluation:honor:list')")
    public Result<List<Map<String, Object>>> getStudentApplications(
            @Parameter(description = "学生ID") @PathVariable Long studentId,
            @Parameter(description = "周期ID") @RequestParam(required = false) Long periodId) {
        log.info("获取学生申报列表: studentId={}, periodId={}", studentId, periodId);
        List<Map<String, Object>> list = applicationService.getStudentApplications(studentId, periodId);
        return Result.success(list);
    }

    /**
     * 获取我的申报列表
     */
    @GetMapping("/my")
    @Operation(summary = "获取我的申报列表")
    @PreAuthorize("hasAuthority('evaluation:honor:apply')")
    public Result<List<Map<String, Object>>> getMyApplications(
            @Parameter(description = "周期ID") @RequestParam(required = false) Long periodId) {
        // 从SecurityContext获取当前认证用户ID
        Long studentId = com.school.management.util.SecurityUtils.getCurrentUserId();
        if (studentId == null) {
            log.warn("未获取到当前用户ID，请确保已登录");
            return Result.error("未认证用户");
        }
        log.info("获取我的申报列表: studentId={}", studentId);
        List<Map<String, Object>> list = applicationService.getStudentApplications(studentId, periodId);
        return Result.success(list);
    }

    /**
     * 获取待审核列表
     */
    @GetMapping("/pending-review")
    @Operation(summary = "获取待审核列表")
    @PreAuthorize("hasAnyAuthority('evaluation:honor:class-review', 'evaluation:honor:dept-review', 'evaluation:honor:school-review')")
    public Result<List<Map<String, Object>>> getPendingReviewList(
            @Parameter(description = "审核级别") @RequestParam String level) {
        Long reviewerId = com.school.management.util.SecurityUtils.getCurrentUserId();
        log.info("获取待审核列表: level={}, reviewerId={}", level, reviewerId);
        List<Map<String, Object>> list = applicationService.getPendingReviewList(level, reviewerId);
        return Result.success(list);
    }

    /**
     * 检查是否可申报
     */
    @GetMapping("/check-can-apply")
    @Operation(summary = "检查是否可申报")
    @PreAuthorize("hasAuthority('evaluation:honor:apply')")
    public Result<Map<String, Object>> checkCanApply(
            @Parameter(description = "学生ID") @RequestParam Long studentId,
            @Parameter(description = "荣誉类型ID") @RequestParam Long honorTypeId,
            @Parameter(description = "周期ID") @RequestParam Long periodId) {
        log.info("检查是否可申报: studentId={}, honorTypeId={}", studentId, honorTypeId);
        Map<String, Object> result = applicationService.checkCanApply(studentId, honorTypeId, periodId);
        return Result.success(result);
    }

    /**
     * 获取可申报的荣誉类型
     */
    @GetMapping("/available-honor-types")
    @Operation(summary = "获取可申报的荣誉类型")
    @PreAuthorize("hasAuthority('evaluation:honor:apply')")
    public Result<List<Map<String, Object>>> getAvailableHonorTypes(
            @Parameter(description = "学生ID") @RequestParam Long studentId,
            @Parameter(description = "周期ID") @RequestParam Long periodId) {
        log.info("获取可申报荣誉类型: studentId={}, periodId={}", studentId, periodId);
        List<Map<String, Object>> list = applicationService.getAvailableHonorTypes(studentId, periodId);
        return Result.success(list);
    }

    /**
     * 获取审核统计
     */
    @GetMapping("/statistics/{periodId}")
    @Operation(summary = "获取审核统计")
    @PreAuthorize("hasAuthority('evaluation:honor:list')")
    public Result<Map<String, Object>> getReviewStatistics(
            @Parameter(description = "周期ID") @PathVariable Long periodId) {
        log.info("获取审核统计: periodId={}", periodId);
        Map<String, Object> statistics = applicationService.getReviewStatistics(periodId);
        return Result.success(statistics);
    }

    /**
     * 导出申报数据
     */
    @GetMapping("/export")
    @Operation(summary = "导出申报数据")
    @PreAuthorize("hasAuthority('evaluation:honor:export')")
    public Result<List<Map<String, Object>>> exportApplications(
            @Parameter(description = "周期ID") @RequestParam(required = false) Long periodId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        log.info("导出申报数据: periodId={}", periodId);
        Map<String, Object> query = new HashMap<>();
        query.put("periodId", periodId);
        query.put("status", status);
        List<Map<String, Object>> data = applicationService.exportApplications(query);
        return Result.success(data);
    }
}
