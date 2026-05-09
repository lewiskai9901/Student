package com.school.management.application.mytodo;

import com.school.management.infrastructure.extension.MyTodoSourcePlugin;
import com.school.management.infrastructure.extension.MyTodoSourcePlugin.TodoItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 我的待办聚合 service — 跨所有 MyTodoSourcePlugin 实现汇总.
 *
 * <p>启动期 Spring 收集所有 MyTodoSourcePlugin bean, 调用方一次拿全所有 source.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MyTodoService {

    private final List<MyTodoSourcePlugin> sources;

    /**
     * 拉指定用户全部待办,按创建时间倒序.
     *
     * @param userId         用户 id
     * @param sourceFilter   可选, 只拉指定 sourceCode 的待办; null 拉全部
     */
    public List<TodoItem> listAll(Long userId, String sourceFilter) {
        return sources.stream()
            .filter(s -> sourceFilter == null || sourceFilter.equals(s.sourceCode()))
            .flatMap(s -> {
                try {
                    return s.fetch(userId).stream();
                } catch (Exception e) {
                    log.warn("[MyTodo] source {} 拉取失败: {}", s.sourceCode(), e.getMessage());
                    return java.util.stream.Stream.empty();
                }
            })
            .sorted(Comparator.comparing(TodoItem::createdAt,
                Comparator.nullsLast(Comparator.reverseOrder())))
            .collect(Collectors.toList());
    }

    /** 计数(轻量,前端 badge 用) */
    public Map<String, Integer> countBySource(Long userId) {
        Map<String, Integer> counts = new java.util.LinkedHashMap<>();
        for (MyTodoSourcePlugin s : sources) {
            try {
                counts.put(s.sourceCode(), s.fetch(userId).size());
            } catch (Exception e) {
                counts.put(s.sourceCode(), 0);
            }
        }
        return counts;
    }
}
