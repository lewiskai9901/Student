package com.school.management.interfaces.rest.mytodo;

import com.school.management.application.mytodo.MyTodoService;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.infrastructure.extension.MyTodoSourcePlugin.TodoItem;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/my-todos")
@RequiredArgsConstructor
public class MyTodoController {

    private final MyTodoService myTodoService;

    /** 我的全部待办 (可按 source 过滤) */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Result<List<TodoItem>> list(@RequestParam(required = false) String source) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return Result.success(List.of());
        return Result.success(myTodoService.listAll(userId, source));
    }

    /** 各来源待办计数 (badge 用) */
    @GetMapping("/counts")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Integer>> counts() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return Result.success(Map.of());
        return Result.success(myTodoService.countBySource(userId));
    }
}
