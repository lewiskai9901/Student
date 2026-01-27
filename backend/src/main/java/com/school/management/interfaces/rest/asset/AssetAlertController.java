package com.school.management.interfaces.rest.asset;

import com.school.management.application.asset.AssetAlertApplicationService;
import com.school.management.application.asset.query.AssetAlertDTO;
import com.school.management.common.result.Result;
import com.school.management.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 资产预警控制器
 */
@Tag(name = "资产预警", description = "资产预警管理接口")
@RestController
@RequestMapping("/asset-alerts")
@RequiredArgsConstructor
public class AssetAlertController {

    private final AssetAlertApplicationService alertService;

    @Operation(summary = "获取预警详情")
    @GetMapping("/{id}")
    public Result<AssetAlertDTO> getAlert(@PathVariable Long id) {
        return Result.success(alertService.getAlert(id));
    }

    @Operation(summary = "获取我的未读预警")
    @GetMapping("/unread")
    public Result<List<AssetAlertDTO>> getUnreadAlerts(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return Result.success(alertService.getUnreadAlerts(user.getId()));
    }

    @Operation(summary = "获取我的未处理预警")
    @GetMapping("/unhandled")
    public Result<List<AssetAlertDTO>> getUnhandledAlerts(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return Result.success(alertService.getUnhandledAlerts(user.getId()));
    }

    @Operation(summary = "标记为已读")
    @PostMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id) {
        alertService.markAsRead(id);
        return Result.success();
    }

    @Operation(summary = "全部标记为已读")
    @PostMapping("/read-all")
    public Result<Void> markAllAsRead(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        alertService.markAllAsRead(user.getId());
        return Result.success();
    }

    @Operation(summary = "处理预警")
    @PostMapping("/{id}/handle")
    public Result<Void> handleAlert(
            @PathVariable Long id,
            @RequestBody(required = false) HandleAlertRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        alertService.handleAlert(
                id,
                user.getId(),
                user.getRealName(),
                request != null ? request.getRemark() : null
        );
        return Result.success();
    }

    @Operation(summary = "分页查询预警列表")
    @GetMapping
    public Result<Map<String, Object>> queryAlerts(
            @RequestParam(required = false) Integer alertType,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) Boolean isHandled,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        List<AssetAlertDTO> list = alertService.queryAlerts(
                alertType, isRead, isHandled, null, pageNum, pageSize
        );
        int total = alertService.countAlerts(alertType, isRead, isHandled, null);

        Map<String, Object> result = new HashMap<>();
        result.put("records", list);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return Result.success(result);
    }

    @Operation(summary = "获取预警统计")
    @GetMapping("/statistics")
    public Result<Map<String, Integer>> getStatistics() {
        return Result.success(alertService.getAlertStatistics());
    }

    @Operation(summary = "获取未读数量")
    @GetMapping("/unread/count")
    public Result<Integer> countUnread(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return Result.success(alertService.countUnread(user.getId()));
    }
}
