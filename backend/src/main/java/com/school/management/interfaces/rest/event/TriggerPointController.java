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
 * 触发点管理 API.
 *
 * <p>L3 (2026-05-19): 9 处直 jdbc 已收拢到 {@link EventConfigApplicationService}.
 * 顺手修了 list() 的 SQL 注入 (原 string 拼接 + replace 单引号防御不足).
 */
@RestController
@RequestMapping("/event/trigger-points")
@Tag(name = "触发点管理", description = "事件触发点配置 API")
@RequiredArgsConstructor
public class TriggerPointController {

    private final EventConfigApplicationService eventConfigService;

    @GetMapping
    @Operation(summary = "获取触发点列表")
    @CasbinAccess(resource = "event-trigger", action = "view")
    public Result<List<Map<String, Object>>> list(
            @RequestParam(required = false) String module) {
        return Result.success(eventConfigService.listTriggerPoints(module));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取触发点详情")
    @CasbinAccess(resource = "event-trigger", action = "view")
    public Result<Map<String, Object>> getById(@PathVariable Long id) {
        Map<String, Object> row = eventConfigService.getTriggerPointById(id);
        if (row == null) {
            return Result.error("触发点不存在");
        }
        return Result.success(row);
    }

    @PostMapping
    @Operation(summary = "创建触发点")
    @CasbinAccess(resource = "event-trigger", action = "add")
    public Result<Void> create(@RequestBody Map<String, Object> body) {
        eventConfigService.createTriggerPoint(body);
        return Result.success();
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新触发点")
    @CasbinAccess(resource = "event-trigger", action = "edit")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        eventConfigService.updateTriggerPoint(id, body);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除触发点")
    @CasbinAccess(resource = "event-trigger", action = "delete")
    public Result<Void> delete(@PathVariable Long id) {
        eventConfigService.deleteTriggerPoint(id);
        return Result.success();
    }

    @PutMapping("/{id}/enable")
    @Operation(summary = "启用触发点")
    @CasbinAccess(resource = "event-trigger", action = "edit")
    public Result<Void> enable(@PathVariable Long id) {
        eventConfigService.enableTriggerPoint(id);
        return Result.success();
    }

    @PutMapping("/{id}/disable")
    @Operation(summary = "禁用触发点")
    @CasbinAccess(resource = "event-trigger", action = "edit")
    public Result<Void> disable(@PathVariable Long id) {
        eventConfigService.disableTriggerPoint(id);
        return Result.success();
    }
}
