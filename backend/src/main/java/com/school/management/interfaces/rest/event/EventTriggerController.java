package com.school.management.interfaces.rest.event;

import com.school.management.application.event.EventConfigApplicationService;
import com.school.management.common.result.Result;
import com.school.management.infrastructure.casbin.CasbinAccess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 事件触发器管理 API.
 *
 * <p>L3 (2026-05-19): 9 处直 jdbc 已收拢到 {@link EventConfigApplicationService}.
 * Controller 只负责 HTTP 绑定 + 权限注解, 业务逻辑全部下沉.
 */
@RestController
@RequestMapping("/event/triggers")
@Tag(name = "事件触发器管理", description = "事件触发器配置 API")
@RequiredArgsConstructor
public class EventTriggerController {

    private final EventConfigApplicationService eventConfigService;

    @GetMapping
    @Operation(summary = "获取触发器列表")
    @CasbinAccess(resource = "event-trigger", action = "view")
    public Result<List<Map<String, Object>>> list(
            @RequestParam(required = false) String pointCode,
            @RequestParam(required = false) String eventType) {
        return Result.success(eventConfigService.listTriggers(pointCode, eventType));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取触发器详情")
    @CasbinAccess(resource = "event-trigger", action = "view")
    public Result<Map<String, Object>> getById(@PathVariable Long id) {
        Map<String, Object> row = eventConfigService.getTriggerById(id);
        if (row == null) {
            return Result.error("触发器不存在");
        }
        return Result.success(row);
    }

    @PostMapping
    @Operation(summary = "创建触发器")
    @CasbinAccess(resource = "event-trigger", action = "add")
    public Result<Void> create(@RequestBody Map<String, Object> body) {
        eventConfigService.createTrigger(body);
        return Result.success();
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新触发器")
    @CasbinAccess(resource = "event-trigger", action = "edit")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        eventConfigService.updateTrigger(id, body);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除触发器")
    @CasbinAccess(resource = "event-trigger", action = "delete")
    public Result<Void> delete(@PathVariable Long id) {
        eventConfigService.deleteTrigger(id);
        return Result.success();
    }

    @PutMapping("/{id}/enable")
    @Operation(summary = "启用触发器")
    @CasbinAccess(resource = "event-trigger", action = "edit")
    public Result<Void> enable(@PathVariable Long id) {
        eventConfigService.enableTrigger(id);
        return Result.success();
    }

    @PutMapping("/{id}/disable")
    @Operation(summary = "禁用触发器")
    @CasbinAccess(resource = "event-trigger", action = "edit")
    public Result<Void> disable(@PathVariable Long id) {
        eventConfigService.disableTrigger(id);
        return Result.success();
    }

    @PostMapping("/test")
    @Operation(summary = "测试触发器匹配")
    @CasbinAccess(resource = "event-trigger", action = "view")
    @SuppressWarnings("unchecked")
    public Result<List<Map<String, Object>>> test(@RequestBody Map<String, Object> body) {
        String pointCode = (String) body.get("pointCode");
        Map<String, Object> context = (Map<String, Object>) body.get("context");
        if (pointCode == null || context == null) {
            return Result.error("pointCode和context不能为空");
        }
        return Result.success(eventConfigService.testTrigger(pointCode, context));
    }
}
