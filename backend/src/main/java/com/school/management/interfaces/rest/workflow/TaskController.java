package com.school.management.interfaces.rest.workflow;

import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户任务管理 — 我的待办/认领/完成/转办.
 */
@RestController("workflowTaskController")
@RequestMapping("/workflow/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    /** 我的待办 (assignee=我 OR 候选人含我) */
    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    public Result<List<Map<String, Object>>> myTasks() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return Result.success(List.of());
        String me = String.valueOf(userId);
        List<Task> tasks = taskService.createTaskQuery()
            .or()
                .taskAssignee(me)
                .taskCandidateUser(me)
            .endOr()
            .orderByTaskCreateTime().desc()
            .list();
        return Result.success(tasks.stream().map(this::toRow).collect(Collectors.toList()));
    }

    /** 任务详情 */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> getById(@PathVariable String id) {
        Task t = taskService.createTaskQuery().taskId(id).singleResult();
        if (t == null) return Result.error("任务不存在或已完成: " + id);
        return Result.success(toRow(t));
    }

    /** 认领候选人任务 — 仅候选人可认领, 已被他人认领则拒绝 */
    @PostMapping("/{id}/claim")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> claim(@PathVariable String id) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return Result.error("未登录");
        String me = String.valueOf(userId);
        Task t = taskService.createTaskQuery().taskId(id).singleResult();
        if (t == null) return Result.error("任务不存在或已完成: " + id);
        if (t.getAssignee() != null && !me.equals(t.getAssignee())) {
            return Result.error(403, "该任务已被他人认领");
        }
        boolean candidate = taskService.createTaskQuery()
            .taskId(id).taskCandidateUser(me).count() > 0;
        if (!candidate && t.getAssignee() == null) {
            return Result.error(403, "无权认领该任务 — 非候选人");
        }
        taskService.claim(id, me);
        return Result.success(null);
    }

    /** 取消认领(放回候选池) — 仅当前受理人可操作 */
    @PostMapping("/{id}/unclaim")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> unclaim(@PathVariable String id) {
        Task t = taskService.createTaskQuery().taskId(id).singleResult();
        if (t == null) return Result.error("任务不存在或已完成: " + id);
        if (!isMyTask(t)) return Result.error(403, "无权操作该任务 — 仅受理人可取消认领");
        taskService.unclaim(id);
        return Result.success(null);
    }

    /** 完成任务(可附带 variables) — 仅当前受理人可操作 */
    @PostMapping("/{id}/complete")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> complete(@PathVariable String id, @RequestBody(required = false) CompleteRequest req) {
        Task t = taskService.createTaskQuery().taskId(id).singleResult();
        if (t == null) return Result.error("任务不存在或已完成: " + id);
        if (!isMyTask(t)) return Result.error(403, "无权完成该任务 — 仅受理人可完成");
        Map<String, Object> vars = (req != null && req.getVariables() != null) ? req.getVariables() : Map.of();
        taskService.complete(id, vars);
        return Result.success(null);
    }

    /** 转办给其他人 — 仅当前受理人可操作 */
    @PostMapping("/{id}/delegate")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> delegate(@PathVariable String id, @RequestBody DelegateRequest req) {
        Task t = taskService.createTaskQuery().taskId(id).singleResult();
        if (t == null) return Result.error("任务不存在或已完成: " + id);
        if (!isMyTask(t)) return Result.error(403, "无权转办该任务 — 仅受理人可转办");
        taskService.delegateTask(id, req.getAssignee());
        return Result.success(null);
    }

    /** 当前登录用户是否为该任务受理人 */
    private boolean isMyTask(Task t) {
        Long me = SecurityUtils.getCurrentUserId();
        return me != null && String.valueOf(me).equals(t.getAssignee());
    }

    private Map<String, Object> toRow(Task t) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", t.getId());
        row.put("name", t.getName() != null ? t.getName() : "");
        row.put("assignee", t.getAssignee() != null ? t.getAssignee() : "");
        row.put("processInstanceId", t.getProcessInstanceId() != null ? t.getProcessInstanceId() : "");
        row.put("processDefinitionId", t.getProcessDefinitionId() != null ? t.getProcessDefinitionId() : "");
        row.put("createTime", t.getCreateTime());
        row.put("dueDate", t.getDueDate());
        return row;
    }

    @Data public static class CompleteRequest { private Map<String, Object> variables; }
    @Data public static class DelegateRequest { private String assignee; }
}
