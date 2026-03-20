package com.school.management.interfaces.rest.place;

import com.school.management.application.place.PlaceCapacityAlertService;
import com.school.management.application.place.query.CapacityAlertDTO;
import com.school.management.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import com.school.management.infrastructure.casbin.CasbinAccess;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 场所容量告警接口
 * 对标: AWS CloudWatch Alarms API
 */
@Tag(name = "场所容量告警", description = "场所容量告警查询和监控API")
@RestController
@RequestMapping("/v9/places/capacity-alerts")
@RequiredArgsConstructor
public class PlaceCapacityAlertController {

    private final PlaceCapacityAlertService capacityAlertService;

    /**
     * 获取高占用率场所告警列表
     */
    @Operation(summary = "查询容量告警", description = "获取占用率≥80%的场所告警列表")
    @GetMapping
    @CasbinAccess(resource = "place", action = "view")
    public Result<List<CapacityAlertDTO>> getHighOccupancyAlerts(
            @Parameter(description = "场所类型编码（可选）") @RequestParam(required = false) String typeCode) {
        return Result.success(capacityAlertService.getHighOccupancyAlerts(typeCode));
    }

    /**
     * 获取指定场所的告警信息
     */
    @Operation(summary = "查询单个场所告警", description = "获取指定场所的容量告警信息")
    @GetMapping("/{placeId}")
    @CasbinAccess(resource = "place", action = "view")
    public Result<CapacityAlertDTO> getPlaceAlert(
            @Parameter(description = "场所ID") @PathVariable Long placeId) {
        return Result.success(capacityAlertService.getPlaceAlert(placeId));
    }

    /**
     * 检查是否需要告警
     */
    @Operation(summary = "检查告警状态", description = "检查指定场所是否处于告警状态")
    @GetMapping("/{placeId}/should-alert")
    @CasbinAccess(resource = "place", action = "view")
    public Result<AlertCheckResponse> checkAlert(
            @Parameter(description = "场所ID") @PathVariable Long placeId) {
        boolean shouldAlert = capacityAlertService.shouldAlert(placeId);
        return Result.success(new AlertCheckResponse(placeId, shouldAlert));
    }

    /**
     * 获取告警汇总统计
     */
    @Operation(summary = "查询告警汇总", description = "按类型分组统计告警情况")
    @GetMapping("/summary")
    @CasbinAccess(resource = "place", action = "view")
    public Result<List<PlaceCapacityAlertService.TypeAlertSummary>> getAlertSummary() {
        return Result.success(capacityAlertService.getAlertSummaryByType());
    }

    // ===== Response DTOs =====

    /**
     * 告警检查响应
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class AlertCheckResponse {
        private Long placeId;
        private Boolean shouldAlert;
    }
}
