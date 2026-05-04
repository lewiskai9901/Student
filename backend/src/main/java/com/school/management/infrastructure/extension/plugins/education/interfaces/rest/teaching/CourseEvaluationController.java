package com.school.management.infrastructure.extension.plugins.education.interfaces.rest.teaching;

import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.infrastructure.casbin.CasbinAccess;
import com.school.management.infrastructure.extension.plugins.education.application.teaching.CourseEvaluationService;
import com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.teaching.evaluation.CourseEvaluationPO;
import com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.teaching.evaluation.EvaluationIndicatorPO;
import com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.teaching.evaluation.EvaluationResponsePO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 学生评教 REST 端点.
 *
 * 路径: /teaching/evaluations
 *   GET    /                              — 列出评教活动
 *   POST   /                              — 创建活动 (admin)
 *   PUT    /{id}                          — 更新活动 (admin)
 *   POST   /{id}/start                    — 启动评教 (admin)
 *   POST   /{id}/close                    — 结束评教 (admin)
 *   DELETE /{id}                          — 删除 (admin)
 *
 *   GET    /{id}/indicators                — 列出指标项
 *   POST   /{id}/indicators                — 添加指标 (admin)
 *   DELETE /{id}/indicators/{indicatorId}  — 删除指标 (admin)
 *
 *   GET    /{id}/my-pending-tasks          — 学生: 我的待评 task
 *   POST   /{id}/responses                 — 学生: 提交评教
 *   GET    /{id}/tasks/{taskId}/summary    — 教师/管理员: 某 task 汇总
 */
@RestController
@RequestMapping("/teaching/evaluations")
@RequiredArgsConstructor
public class CourseEvaluationController {

    private final CourseEvaluationService service;

    /* ---------- 评教活动 ---------- */
    @GetMapping
    @CasbinAccess(resource = "teaching:task", action = "view")
    public Result<List<CourseEvaluationPO>> list(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) Integer status) {
        return Result.success(service.listEvaluations(semesterId, status));
    }

    @GetMapping("/{id}")
    @CasbinAccess(resource = "teaching:task", action = "view")
    public Result<CourseEvaluationPO> get(@PathVariable Long id) {
        return Result.success(service.getEvaluation(id));
    }

    @PostMapping
    @CasbinAccess(resource = "teaching:task", action = "edit")
    public Result<CourseEvaluationPO> create(@RequestBody CourseEvaluationPO body) {
        return Result.success(service.createEvaluation(body, SecurityUtils.getCurrentUserId()));
    }

    @PutMapping("/{id}")
    @CasbinAccess(resource = "teaching:task", action = "edit")
    public Result<CourseEvaluationPO> update(@PathVariable Long id, @RequestBody CourseEvaluationPO body) {
        return Result.success(service.updateEvaluation(id, body));
    }

    @PostMapping("/{id}/start")
    @CasbinAccess(resource = "teaching:task", action = "edit")
    public Result<Void> start(@PathVariable Long id) {
        service.startEvaluation(id);
        return Result.success(null);
    }

    @PostMapping("/{id}/close")
    @CasbinAccess(resource = "teaching:task", action = "edit")
    public Result<Void> close(@PathVariable Long id) {
        service.closeEvaluation(id);
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "teaching:task", action = "edit")
    public Result<Void> delete(@PathVariable Long id) {
        service.deleteEvaluation(id);
        return Result.success(null);
    }

    /* ---------- 指标项 ---------- */
    @GetMapping("/{id}/indicators")
    @CasbinAccess(resource = "teaching:task", action = "view")
    public Result<List<EvaluationIndicatorPO>> listIndicators(@PathVariable Long id) {
        return Result.success(service.listIndicators(id));
    }

    @PostMapping("/{id}/indicators")
    @CasbinAccess(resource = "teaching:task", action = "edit")
    public Result<EvaluationIndicatorPO> createIndicator(@PathVariable Long id,
                                                         @RequestBody EvaluationIndicatorPO body) {
        return Result.success(service.createIndicator(id, body));
    }

    @DeleteMapping("/{id}/indicators/{indicatorId}")
    @CasbinAccess(resource = "teaching:task", action = "edit")
    public Result<Void> deleteIndicator(@PathVariable Long id, @PathVariable Long indicatorId) {
        service.deleteIndicator(indicatorId);
        return Result.success(null);
    }

    /* ---------- 学生提交 ---------- */
    @GetMapping("/{id}/my-pending-tasks")
    @CasbinAccess(resource = "teaching:task", action = "view")
    public Result<List<Map<String, Object>>> myPending(@PathVariable Long id) {
        return Result.success(service.listMyPendingTasks(id, SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/{id}/responses")
    @CasbinAccess(resource = "teaching:task", action = "view")
    public Result<EvaluationResponsePO> submit(@PathVariable Long id,
                                               @RequestBody EvaluationResponsePO body) {
        return Result.success(service.submit(id, SecurityUtils.getCurrentUserId(), body));
    }

    /* ---------- 汇总 ---------- */
    @GetMapping("/{id}/tasks/{taskId}/summary")
    @CasbinAccess(resource = "teaching:task", action = "view")
    public Result<Map<String, Object>> taskSummary(@PathVariable Long id, @PathVariable Long taskId) {
        return Result.success(service.taskSummary(id, taskId));
    }
}
