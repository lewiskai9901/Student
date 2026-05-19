package com.school.management.interfaces.rest.event;

import com.school.management.application.event.EventQueryApplicationService;
import com.school.management.common.result.Result;
import com.school.management.infrastructure.casbin.CasbinAccess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 实体事件查询 API (增强版, 路径 /event/events).
 * 支持按主体+极性查询、统计聚合.
 *
 * <p>L3 (2026-05-19): 8 处直 jdbc 已下沉到 {@link EventQueryApplicationService}.
 */
@RestController
@RequestMapping("/event/events")
@Tag(name = "实体事件查询(增强)", description = "事件查询 API - 含极性筛选、统计聚合")
@RequiredArgsConstructor
public class EventController {

    private final EventQueryApplicationService eventQueryService;

    @GetMapping("/by-subject")
    @Operation(summary = "按主体查询事件流（支持极性筛选和分页）")
    @CasbinAccess(resource = "entity-event", action = "view")
    public Result<Map<String, Object>> getBySubject(
            @RequestParam String subjectType,
            @RequestParam Long subjectId,
            @RequestParam(required = false) String polarity,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String eventType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(eventQueryService.queryBySubject(
            subjectType, subjectId, polarity, category, eventType, page, size));
    }

    @GetMapping("/statistics")
    @Operation(summary = "事件统计（按主体/分类/极性聚合）")
    @CasbinAccess(resource = "entity-event", action = "view")
    public Result<Map<String, Object>> statistics(
            @RequestParam(required = false) String subjectType,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.success(eventQueryService.statistics(subjectType, subjectId, startDate, endDate));
    }
}
