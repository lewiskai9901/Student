package com.school.management.application.workflow;

import com.school.management.infrastructure.extension.MyTodoSourcePlugin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Flowable 用户任务作为待办源 — 对接 P3 我的待办页.
 *
 * <p>仅当 Flowable TaskService 存在时启用 (dev profile exclude 时无声跳过).
 */
@Slf4j
@Component
@ConditionalOnBean(TaskService.class)
@RequiredArgsConstructor
public class MyWorkflowTodoSource implements MyTodoSourcePlugin {

    private final TaskService taskService;

    @Override public String sourceCode() { return "workflow"; }
    @Override public String sourceName() { return "工作流任务"; }

    @Override
    public List<TodoItem> fetch(Long userId) {
        if (userId == null) return List.of();
        String me = String.valueOf(userId);
        List<Task> tasks = taskService.createTaskQuery()
            .or()
                .taskAssignee(me)
                .taskCandidateUser(me)
            .endOr()
            .orderByTaskCreateTime().desc()
            .list();
        return tasks.stream().map(t -> new TodoItem(
            "workflow:" + t.getId(),
            "workflow",
            "工作流任务",
            t.getName() != null ? t.getName() : "未命名任务",
            "流程实例:" + t.getProcessInstanceId(),
            "MEDIUM",
            "/workflow/my-tasks?taskId=" + t.getId(),
            t.getCreateTime() != null ? t.getCreateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime() : null,
            t.getDueDate() != null ? t.getDueDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime() : null
        )).collect(Collectors.toList());
    }
}
