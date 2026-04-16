package com.school.management.interfaces.rest.activity;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.management.common.result.Result;
import com.school.management.infrastructure.casbin.CasbinAccess;
import com.school.management.infrastructure.activity.ActivityEventDTO;
import com.school.management.infrastructure.activity.ActivityEventQuery;
import com.school.management.infrastructure.activity.impl.ActivityEventRepositoryImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 统一活动事件 REST API
 */
@RestController
@RequestMapping("/activity-events")
@RequiredArgsConstructor
public class ActivityEventController {

    private final ActivityEventRepositoryImpl activityEventRepository;

    /**
     * 系统级审计日志（分页+筛选）
     */
    @GetMapping
    @CasbinAccess(resource = "system:audit", action = "view")
    public Result<Map<String, Object>> listEvents(ActivityEventQuery query) {
        IPage<ActivityEventDTO> page = activityEventRepository.queryPage(query);
        return Result.success(Map.of(
                "records", page.getRecords(),
                "total", page.getTotal(),
                "pageNum", page.getCurrent(),
                "pageSize", page.getSize()
        ));
    }

    /**
     * 资源级时间线
     */
    @GetMapping("/resource/{resourceType}/{resourceId}")
    @CasbinAccess(resource = "system:audit", action = "view")
    public Result<List<ActivityEventDTO>> getResourceTimeline(
            @PathVariable String resourceType,
            @PathVariable String resourceId,
            @RequestParam(defaultValue = "50") int limit) {
        return Result.success(activityEventRepository.findByResource(resourceType, resourceId, limit));
    }

    /**
     * 用户级活动历史
     */
    @GetMapping("/user/{userId}")
    @CasbinAccess(resource = "system:audit", action = "view")
    public Result<List<ActivityEventDTO>> getUserActivity(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "50") int limit) {
        return Result.success(activityEventRepository.findByUser(userId, limit));
    }

    /**
     * 统计
     */
    @GetMapping("/stats")
    @CasbinAccess(resource = "system:audit", action = "view")
    public Result<Map<String, Object>> getStats(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return Result.success(activityEventRepository.getStats(startTime, endTime));
    }
}
