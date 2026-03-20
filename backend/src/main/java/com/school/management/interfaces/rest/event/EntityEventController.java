package com.school.management.interfaces.rest.event;

import com.school.management.application.event.EntityEventApplicationService;
import com.school.management.common.result.Result;
import com.school.management.domain.event.model.EntityEvent;
import com.school.management.infrastructure.casbin.CasbinAccess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 实体事件 API
 */
@RestController
@RequestMapping("/entity-events")
@Tag(name = "实体事件", description = "通用实体事件时间线 API")
@RequiredArgsConstructor
public class EntityEventController {

    private final EntityEventApplicationService eventService;

    @GetMapping("/subject/{type}/{id}")
    @Operation(summary = "按主体查询事件时间线")
    @CasbinAccess(resource = "entity-event", action = "view")
    public Result<List<EntityEvent>> getSubjectTimeline(
            @PathVariable String type,
            @PathVariable Long id,
            @RequestParam(defaultValue = "50") int limit) {
        return Result.success(eventService.getSubjectTimeline(type.toUpperCase(), id, limit));
    }

    @GetMapping("/related/{type}/{id}")
    @Operation(summary = "按关联主体查询事件时间线")
    @CasbinAccess(resource = "entity-event", action = "view")
    public Result<List<EntityEvent>> getRelatedTimeline(
            @PathVariable String type,
            @PathVariable Long id,
            @RequestParam(defaultValue = "50") int limit) {
        return Result.success(eventService.getRelatedTimeline(type.toUpperCase(), id, limit));
    }

    @GetMapping("/stats/{type}/{id}")
    @Operation(summary = "获取主体事件统计")
    @CasbinAccess(resource = "entity-event", action = "view")
    public Result<List<Map<String, Object>>> getSubjectStats(
            @PathVariable String type,
            @PathVariable Long id) {
        return Result.success(eventService.getSubjectStats(type.toUpperCase(), id));
    }
}
