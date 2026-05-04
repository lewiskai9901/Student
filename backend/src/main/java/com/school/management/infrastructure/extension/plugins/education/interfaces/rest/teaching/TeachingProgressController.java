package com.school.management.infrastructure.extension.plugins.education.interfaces.rest.teaching;

import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.infrastructure.casbin.CasbinAccess;
import com.school.management.infrastructure.extension.plugins.education.application.teaching.TeachingProgressService;
import com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.teaching.progress.TeachingProgressPO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 教学进度跟踪 REST 端点.
 *
 * 路径: /teaching/progress
 *   GET    /by-task/{taskId}        — 某 task 的全部周次进度
 *   GET    /                        — 按学期/班级/周/状态筛选
 *   POST   /                        — 创建一条记录 (教师录入)
 *   PUT    /{id}                    — 更新某周授课实况
 *   DELETE /{id}                    — 删除
 *   GET    /summary/{taskId}        — 任务完成度统计
 */
@RestController
@RequestMapping("/teaching/progress")
@RequiredArgsConstructor
public class TeachingProgressController {

    private final TeachingProgressService service;

    @GetMapping("/by-task/{taskId}")
    @CasbinAccess(resource = "teaching:task", action = "view")
    public Result<List<TeachingProgressPO>> listByTask(@PathVariable Long taskId) {
        return Result.success(service.listByTask(taskId));
    }

    @GetMapping
    @CasbinAccess(resource = "teaching:task", action = "view")
    public Result<List<TeachingProgressPO>> list(
            @RequestParam Long semesterId,
            @RequestParam(required = false) Long orgUnitId,
            @RequestParam(required = false) Integer weekNumber,
            @RequestParam(required = false) Integer status) {
        return Result.success(service.listBySemester(semesterId, orgUnitId, weekNumber, status));
    }

    @PostMapping
    @CasbinAccess(resource = "teaching:task", action = "edit")
    public Result<TeachingProgressPO> create(@RequestBody TeachingProgressPO body) {
        return Result.success(service.create(body, SecurityUtils.getCurrentUserId()));
    }

    @PutMapping("/{id}")
    @CasbinAccess(resource = "teaching:task", action = "edit")
    public Result<TeachingProgressPO> update(@PathVariable Long id, @RequestBody TeachingProgressPO body) {
        return Result.success(service.update(id, body, SecurityUtils.getCurrentUserId()));
    }

    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "teaching:task", action = "edit")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.success(null);
    }

    @GetMapping("/summary/{taskId}")
    @CasbinAccess(resource = "teaching:task", action = "view")
    public Result<Map<String, Object>> summary(@PathVariable Long taskId) {
        return Result.success(service.taskSummary(taskId));
    }
}
