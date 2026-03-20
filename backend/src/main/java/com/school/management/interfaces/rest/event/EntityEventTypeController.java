package com.school.management.interfaces.rest.event;

import com.school.management.application.event.EntityEventTypeApplicationService;
import com.school.management.common.result.Result;
import com.school.management.domain.event.model.EntityEventType;
import com.school.management.infrastructure.casbin.CasbinAccess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 事件类型管理 API
 */
@RestController
@RequestMapping("/entity-event-types")
@Tag(name = "事件类型管理", description = "通用实体事件类型配置 API")
@RequiredArgsConstructor
public class EntityEventTypeController {

    private final EntityEventTypeApplicationService typeService;

    @GetMapping
    @Operation(summary = "获取所有事件类型")
    @CasbinAccess(resource = "entity-event-type", action = "view")
    public Result<List<EntityEventType>> listTypes(
            @RequestParam(required = false) String category) {
        if (category != null) {
            return Result.success(typeService.listByCategory(category.toUpperCase()));
        }
        return Result.success(typeService.listTypes());
    }

    @GetMapping("/enabled")
    @Operation(summary = "获取启用的事件类型")
    @CasbinAccess(resource = "entity-event-type", action = "view")
    public Result<List<EntityEventType>> listEnabled() {
        return Result.success(typeService.listEnabled());
    }

    @PostMapping
    @Operation(summary = "创建事件类型")
    @CasbinAccess(resource = "entity-event-type", action = "add")
    public Result<EntityEventType> create(@RequestBody EntityEventType type) {
        return Result.success(typeService.create(type));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新事件类型")
    @CasbinAccess(resource = "entity-event-type", action = "edit")
    public Result<EntityEventType> update(@PathVariable Long id,
                                           @RequestBody EntityEventType type) {
        return Result.success(typeService.update(id, type));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除事件类型")
    @CasbinAccess(resource = "entity-event-type", action = "delete")
    public Result<Void> delete(@PathVariable Long id) {
        typeService.delete(id);
        return Result.success();
    }
}
