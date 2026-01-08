package com.school.management.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.management.common.result.Result;
import com.school.management.dto.CheckItemAppealDTO;
import com.school.management.dto.request.AppealCreateRequest;
import com.school.management.dto.request.AppealReviewRequest;
import com.school.management.dto.vo.AppealListVO;
import com.school.management.dto.vo.AppealStatisticsVO;
import com.school.management.dto.vo.RankingChangeVO;
import com.school.management.service.CheckItemAppealService;
import com.school.management.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 申诉管理Controller (V1 - 已弃用)
 *
 * @author system
 * @since 2024-12-29
 * @deprecated 使用 V2 API {@link com.school.management.interfaces.rest.inspection.AppealController} 替代
 *             V2 API 路径: /api/v2/appeals
 */
@Deprecated(since = "2.0.0", forRemoval = false)
@Slf4j
@RestController
@RequestMapping("/quantification/appeals")
@RequiredArgsConstructor
@Tag(name = "申诉管理", description = "申诉管理相关接口")
public class CheckItemAppealController {

    private final CheckItemAppealService appealService;

    @PostMapping
    @PreAuthorize("hasAuthority('quantification:appeal:create')")
    @Operation(summary = "提交申诉", description = "提交新的申诉")
    public Result<CheckItemAppealDTO> createAppeal(
            @Validated @RequestBody AppealCreateRequest request) {
        log.info("提交申诉: itemId={}", request.getItemId());

        Long currentUserId = getCurrentUserId();

        CheckItemAppealDTO appeal = appealService.createAppeal(request, currentUserId);
        return Result.success(appeal);
    }

    @PutMapping("/{appealId}/review")
    @PreAuthorize("hasAuthority('quantification:appeal:review')")
    @Operation(summary = "审核申诉")
    public Result<CheckItemAppealDTO> reviewAppeal(
            @PathVariable Long appealId,
            @Validated @RequestBody AppealReviewRequest request) {
        log.info("审核申诉: appealId={}, status={}", appealId, request.getApprovalStatus());

        request.setAppealId(appealId);
        Long currentUserId = getCurrentUserId();

        CheckItemAppealDTO appeal = appealService.reviewAppeal(request, currentUserId);
        return Result.success(appeal);
    }

    @PutMapping("/{appealId}/withdraw")
    @PreAuthorize("hasAuthority('quantification:appeal:withdraw')")
    @Operation(summary = "撤销申诉")
    public Result<Boolean> withdrawAppeal(
            @PathVariable Long appealId,
            @Parameter(description = "撤销原因") @RequestParam String reason) {
        log.info("撤销申诉: appealId={}", appealId);

        Long currentUserId = getCurrentUserId();
        boolean success = appealService.withdrawAppeal(appealId, reason, currentUserId);
        return Result.success(success);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('quantification:appeal:list')")
    @Operation(summary = "分页查询申诉列表")
    public Result<IPage<AppealListVO>> listAppeals(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "页大小") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "年级ID") @RequestParam(required = false) Long gradeId,
            @Parameter(description = "班级ID") @RequestParam(required = false) Long classId,
            @Parameter(description = "申诉人ID") @RequestParam(required = false) Long appellantId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "申诉类型") @RequestParam(required = false) Integer appealType,
            @Parameter(description = "开始时间") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword) {

        IPage<AppealListVO> page = appealService.listAppeals(
                pageNum, pageSize, gradeId, classId, appellantId,
                status, appealType, startTime, endTime, keyword
        );
        return Result.success(page);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('quantification:appeal:review')")
    @Operation(summary = "查询待审核的申诉(我的待办)")
    public Result<List<AppealListVO>> listPendingAppeals() {
        Long currentUserId = getCurrentUserId();
        List<AppealListVO> appeals = appealService.listPendingAppeals(currentUserId);
        return Result.success(appeals);
    }

    @GetMapping("/publicity")
    @PreAuthorize("hasAuthority('quantification:appeal:list')")
    @Operation(summary = "查询公示中的申诉")
    public Result<List<AppealListVO>> listPublicityAppeals() {
        List<AppealListVO> appeals = appealService.listPublicityAppeals();
        return Result.success(appeals);
    }

    @GetMapping("/{appealId}")
    @PreAuthorize("hasAuthority('quantification:appeal:view')")
    @Operation(summary = "查询申诉详情")
    public Result<CheckItemAppealDTO> getAppealDetail(@PathVariable Long appealId) {
        CheckItemAppealDTO appeal = appealService.getAppealDetail(appealId);
        if (appeal == null) {
            return Result.error("申诉不存在");
        }
        return Result.success(appeal);
    }

    @GetMapping("/code/{appealCode}")
    @PreAuthorize("hasAuthority('quantification:appeal:view')")
    @Operation(summary = "根据申诉编号查询申诉")
    public Result<CheckItemAppealDTO> getAppealByCode(@PathVariable String appealCode) {
        CheckItemAppealDTO appeal = appealService.getAppealByCode(appealCode);
        if (appeal == null) {
            return Result.error("申诉不存在");
        }
        return Result.success(appeal);
    }

    @GetMapping("/class/{classId}/history")
    @PreAuthorize("hasAuthority('quantification:appeal:list')")
    @Operation(summary = "查询班级的申诉历史")
    public Result<List<AppealListVO>> listAppealHistory(
            @PathVariable Long classId,
            @Parameter(description = "限制数量") @RequestParam(defaultValue = "10") Integer limit) {
        List<AppealListVO> appeals = appealService.listAppealHistory(classId, limit);
        return Result.success(appeals);
    }

    @GetMapping("/check/{itemId}")
    @PreAuthorize("hasAuthority('quantification:appeal:create')")
    @Operation(summary = "检查扣分明细是否可以申诉")
    public Result<String> checkCanAppeal(@PathVariable Long itemId) {
        String reason = appealService.checkCanAppeal(itemId);
        if (reason != null) {
            return Result.error(reason);
        }
        return Result.success("可以申诉");
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('quantification:appeal:statistics')")
    @Operation(summary = "获取申诉统计数据")
    public Result<AppealStatisticsVO> getAppealStatistics(
            @Parameter(description = "统计范围") @RequestParam String scope,
            @Parameter(description = "范围ID") @RequestParam(required = false) Long scopeId,
            @Parameter(description = "统计周期") @RequestParam String period) {
        AppealStatisticsVO statistics = appealService.getAppealStatistics(scope, scopeId, period);
        return Result.success(statistics);
    }

    @PostMapping("/{appealId}/effective")
    @PreAuthorize("hasAuthority('quantification:appeal:manage')")
    @Operation(summary = "手动触发申诉生效")
    public Result<RankingChangeVO> processEffective(@PathVariable Long appealId) {
        log.info("手动触发申诉生效: appealId={}", appealId);
        RankingChangeVO result = appealService.processEffective(appealId);
        return Result.success(result);
    }

    // ========== 私有辅助方法 ==========

    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        return SecurityUtils.getCurrentUserId();
    }
}
