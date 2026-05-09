package com.school.management.infrastructure.extension;

import java.util.List;

/**
 * 我的待办源 SPI — 任意模块/插件实现并注册为 Spring bean,
 * MyTodoService 启动时收集并聚合.
 *
 * <p>每个 source 表示一类待办源 (如 "工作流" / "检查任务" / "整改" / "申诉" / "关系授权审批").
 * 用户访问 /my-todos 时 MyTodoService 调所有 source 的 fetch(userId) 合并结果.
 */
public interface MyTodoSourcePlugin {

    /** 唯一标识, 用作前端按 type 过滤 (如 "workflow" / "inspection-task" / "inspection-correction") */
    String sourceCode();

    /** 中文展示名 */
    String sourceName();

    /** 给定用户拉本源待办 */
    List<TodoItem> fetch(Long userId);

    /**
     * 待办项统一格式.
     */
    record TodoItem(
        String id,                    // 跨源唯一,推荐 "{sourceCode}:{businessId}"
        String sourceCode,
        String sourceName,
        String title,                 // 任务标题
        String description,           // 描述
        String priority,              // HIGH / MEDIUM / LOW
        String detailUrl,             // 前端跳转地址(相对 #/path)
        java.time.LocalDateTime createdAt,
        java.time.LocalDateTime dueDate
    ) {}
}
