package com.school.management.application.inspection;

import com.school.management.infrastructure.extension.MyTodoSourcePlugin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * inspection 我的检查任务作为待办源.
 *
 * <p>不接 inspection 应用服务,直查 inspection_tasks 表 (避免循环依赖,保持 source 轻量).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MyInspectionTaskTodoSource implements MyTodoSourcePlugin {

    private final JdbcTemplate jdbcTemplate;

    @Override public String sourceCode() { return "inspection-task"; }
    @Override public String sourceName() { return "检查任务"; }

    @Override
    public List<TodoItem> fetch(Long userId) {
        if (userId == null) return List.of();
        try {
            // I1: 表名修复 inspection_tasks → insp_tasks (历史 bug, 之前一直返回空)
            //     URL 修复 /inspection/v7/tasks → /inspection/tasks (V7 后缀已去)
            //     inspector_id 已是用户自身 scope, 不需额外 org_unit_id 过滤
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, task_code, deadline, status FROM insp_tasks " +
                "WHERE inspector_id = ? AND deleted = 0 " +
                "  AND status IN ('PENDING', 'CLAIMED', 'IN_PROGRESS') " +
                "ORDER BY deadline ASC LIMIT 100",
                userId);
            return rows.stream().map(row -> new TodoItem(
                "inspection-task:" + row.get("id"),
                sourceCode(),
                sourceName(),
                String.valueOf(row.getOrDefault("task_code", "检查任务")),
                "状态:" + row.get("status"),
                "MEDIUM",
                "/inspection/tasks/" + row.get("id"),
                null,
                row.get("deadline") instanceof Timestamp ts ? ts.toLocalDateTime() : null
            )).collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("[MyTodo:inspection-task] 查询失败: {}", e.getMessage());
            return List.of();
        }
    }
}
