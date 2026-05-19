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
 * 事件类型管理 API (增强版, 路径 /event/types).
 * 支持按分类分组、分类极性、分类 CRUD.
 *
 * <p>L3 (2026-05-19): 13 处直 jdbc 已收拢到 {@link EventConfigApplicationService}.
 */
@RestController
@RequestMapping("/event/types")
@Tag(name = "事件类型管理(增强)", description = "事件类型配置 API - 按分类分组, 含极性")
@RequiredArgsConstructor
public class EventTypeController {

    private final EventConfigApplicationService eventConfigService;

    @GetMapping
    @Operation(summary = "获取事件类型列表（按分类分组）",
        description = "includeDisabled=true 时返回所属插件被禁的事件类型 (pluginEnabled=false), 用于管理员视图灰显")
    @CasbinAccess(resource = "entity-event-type", action = "view")
    public Result<List<Map<String, Object>>> listGrouped(
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "false") Boolean includeDisabled) {
        return Result.success(eventConfigService.listEventTypesGrouped(category, includeDisabled));
    }

    @GetMapping("/categories")
    @Operation(summary = "获取事件分类列表（含极性）")
    @CasbinAccess(resource = "entity-event-type", action = "view")
    public Result<List<Map<String, Object>>> listCategories() {
        return Result.success(eventConfigService.listEventCategories());
    }

    @PostMapping
    @Operation(summary = "创建事件类型")
    @CasbinAccess(resource = "entity-event-type", action = "add")
    public Result<Void> create(@RequestBody Map<String, Object> body) {
        eventConfigService.createEventType(body);
        return Result.success();
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新事件类型")
    @CasbinAccess(resource = "entity-event-type", action = "edit")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        if (!eventConfigService.updateEventType(id, body)) {
            return Result.error("事件类型不存在");
        }
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除事件类型")
    @CasbinAccess(resource = "entity-event-type", action = "delete")
    public Result<Void> delete(@PathVariable Long id) {
        String err = eventConfigService.deleteEventType(id);
        if (err != null) {
            return Result.error(err);
        }
        return Result.success();
    }

    @PostMapping("/categories")
    @Operation(summary = "创建事件分类（批量插入空分类占位）")
    @CasbinAccess(resource = "entity-event-type", action = "add")
    public Result<Void> createCategory(@RequestBody Map<String, Object> body) {
        String err = eventConfigService.createEventCategory(
            (String) body.get("categoryCode"),
            (String) body.get("categoryName"),
            (String) body.get("categoryPolarity"));
        if (err != null) {
            return Result.error(err);
        }
        return Result.success(null);
    }
}
