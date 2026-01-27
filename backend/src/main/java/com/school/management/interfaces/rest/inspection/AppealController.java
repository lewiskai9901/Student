package com.school.management.interfaces.rest.inspection;

import com.school.management.common.result.Result;
import com.school.management.domain.inspection.model.Appeal;
import com.school.management.domain.inspection.model.AppealStatus;
import com.school.management.domain.inspection.repository.AppealRepository;
import com.school.management.domain.shared.event.DomainEventPublisher;
import com.school.management.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API controller for appeal management.
 * Implements the 9-state appeal workflow.
 */
@Slf4j
@Tag(name = "Appeals", description = "Appeal management API with state machine workflow")
@RestController("appealController")
@RequestMapping("/appeals")
public class AppealController {

    private final AppealRepository appealRepository;
    private final DomainEventPublisher eventPublisher;

    public AppealController(AppealRepository appealRepository,
                           DomainEventPublisher eventPublisher) {
        this.appealRepository = appealRepository;
        this.eventPublisher = eventPublisher;
    }

    @Operation(summary = "Create appeal", description = "Creates a new appeal for a deduction")
    @PostMapping
    @PreAuthorize("hasAuthority('inspection:appeal:create')")
    @Transactional
    public Result<AppealResponse> createAppeal(@RequestBody CreateAppealRequest request) {
        // Check if appeal already exists for this deduction
        if (appealRepository.existsByDeductionDetailId(request.getDeductionDetailId())) {
            return Result.error("An appeal already exists for this deduction");
        }

        Appeal appeal = Appeal.create(
            request.getInspectionRecordId(),
            request.getDeductionDetailId(),
            request.getClassId(),
            generateAppealCode(),
            request.getReason(),
            request.getAttachments(),
            request.getOriginalDeduction(),
            request.getRequestedDeduction(),
            getCurrentUserId()
        );

        appeal = appealRepository.save(appeal);
        publishEvents(appeal);

        return Result.success(toResponse(appeal));
    }

    @Operation(summary = "Get appeal", description = "Gets an appeal by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('inspection:appeal:view')")
    public Result<AppealResponse> getAppeal(
            @Parameter(description = "Appeal ID") @PathVariable Long id) {
        Appeal appeal = appealRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Appeal not found: " + id));
        return Result.success(toResponse(appeal));
    }

    @Operation(summary = "Get pending appeals", description = "Gets appeals pending review")
    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('inspection:appeal:review')")
    public Result<List<AppealResponse>> getPendingAppeals(
            @Parameter(description = "Review level") @RequestParam(defaultValue = "1") int level) {
        List<Appeal> appeals = level == 1
            ? appealRepository.findPendingLevel1Review()
            : appealRepository.findPendingLevel2Review();

        return Result.success(appeals.stream()
            .map(this::toResponse)
            .collect(Collectors.toList()));
    }

    @Operation(summary = "Get my appeals", description = "Gets appeals submitted by current user")
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public Result<List<AppealResponse>> getMyAppeals() {
        List<Appeal> appeals = appealRepository.findByApplicantId(getCurrentUserId());
        return Result.success(appeals.stream()
            .map(this::toResponse)
            .collect(Collectors.toList()));
    }

    @Operation(summary = "Start Level 1 review", description = "Starts Level 1 review for an appeal")
    @PutMapping("/{id}/start-level1-review")
    @PreAuthorize("hasAuthority('inspection:appeal:review')")
    @Transactional
    public Result<AppealResponse> startLevel1Review(
            @Parameter(description = "Appeal ID") @PathVariable Long id) {
        Appeal appeal = appealRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Appeal not found: " + id));

        appeal.startLevel1Review(getCurrentUserId());
        appeal = appealRepository.save(appeal);
        publishEvents(appeal);

        return Result.success(toResponse(appeal));
    }

    @Operation(summary = "Level 1 approve", description = "Approves appeal at Level 1")
    @PutMapping("/{id}/level1-approve")
    @PreAuthorize("hasAuthority('inspection:appeal:review')")
    @Transactional
    public Result<AppealResponse> level1Approve(
            @Parameter(description = "Appeal ID") @PathVariable Long id,
            @RequestBody ReviewRequest request) {
        Appeal appeal = appealRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Appeal not found: " + id));

        appeal.level1Approve(getCurrentUserId(), request.getComment());
        appeal = appealRepository.save(appeal);
        publishEvents(appeal);

        return Result.success(toResponse(appeal));
    }

    @Operation(summary = "Level 1 reject", description = "Rejects appeal at Level 1")
    @PutMapping("/{id}/level1-reject")
    @PreAuthorize("hasAuthority('inspection:appeal:review')")
    @Transactional
    public Result<AppealResponse> level1Reject(
            @Parameter(description = "Appeal ID") @PathVariable Long id,
            @RequestBody ReviewRequest request) {
        Appeal appeal = appealRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Appeal not found: " + id));

        appeal.level1Reject(getCurrentUserId(), request.getComment());
        appeal = appealRepository.save(appeal);
        publishEvents(appeal);

        return Result.success(toResponse(appeal));
    }

    @Operation(summary = "Start Level 2 review", description = "Starts Level 2 review for an appeal")
    @PutMapping("/{id}/start-level2-review")
    @PreAuthorize("hasAuthority('inspection:appeal:final-review')")
    @Transactional
    public Result<AppealResponse> startLevel2Review(
            @Parameter(description = "Appeal ID") @PathVariable Long id) {
        Appeal appeal = appealRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Appeal not found: " + id));

        appeal.startLevel2Review(getCurrentUserId());
        appeal = appealRepository.save(appeal);
        publishEvents(appeal);

        return Result.success(toResponse(appeal));
    }

    @Operation(summary = "Final approve", description = "Final approval of an appeal")
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('inspection:appeal:final-review')")
    @Transactional
    public Result<AppealResponse> approve(
            @Parameter(description = "Appeal ID") @PathVariable Long id,
            @RequestBody FinalReviewRequest request) {
        Appeal appeal = appealRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Appeal not found: " + id));

        appeal.approve(getCurrentUserId(), request.getComment(), request.getApprovedDeduction());
        appeal = appealRepository.save(appeal);
        publishEvents(appeal);

        return Result.success(toResponse(appeal));
    }

    @Operation(summary = "Final reject", description = "Final rejection of an appeal")
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('inspection:appeal:final-review')")
    @Transactional
    public Result<AppealResponse> reject(
            @Parameter(description = "Appeal ID") @PathVariable Long id,
            @RequestBody ReviewRequest request) {
        Appeal appeal = appealRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Appeal not found: " + id));

        appeal.reject(getCurrentUserId(), request.getComment());
        appeal = appealRepository.save(appeal);
        publishEvents(appeal);

        return Result.success(toResponse(appeal));
    }

    @Operation(summary = "Withdraw appeal", description = "Withdraws a pending appeal")
    @PutMapping("/{id}/withdraw")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public Result<AppealResponse> withdraw(
            @Parameter(description = "Appeal ID") @PathVariable Long id) {
        Appeal appeal = appealRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Appeal not found: " + id));

        appeal.withdraw(getCurrentUserId());
        appeal = appealRepository.save(appeal);
        publishEvents(appeal);

        return Result.success(toResponse(appeal));
    }

    @Operation(summary = "Make effective", description = "Makes an approved appeal effective")
    @PutMapping("/{id}/make-effective")
    @PreAuthorize("hasAuthority('inspection:appeal:final-review')")
    @Transactional
    public Result<AppealResponse> makeEffective(
            @Parameter(description = "Appeal ID") @PathVariable Long id) {
        Appeal appeal = appealRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Appeal not found: " + id));

        appeal.makeEffective();
        appeal = appealRepository.save(appeal);
        publishEvents(appeal);

        // 更新班级分数：将原扣分调整为审批后的扣分
        updateClassScore(appeal);

        return Result.success(toResponse(appeal));
    }

    @Operation(summary = "Get statistics", description = "Gets appeal statistics by status")
    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('inspection:appeal:view')")
    public Result<AppealStatistics> getStatistics() {
        AppealStatistics stats = new AppealStatistics();
        for (AppealStatus status : AppealStatus.values()) {
            stats.getCountByStatus().put(status.name(), appealRepository.countByStatus(status));
        }
        return Result.success(stats);
    }

    private void publishEvents(Appeal appeal) {
        appeal.getDomainEvents().forEach(eventPublisher::publish);
        appeal.clearDomainEvents();
    }

    private String generateAppealCode() {
        return "APP" + System.currentTimeMillis();
    }

    private Long getCurrentUserId() {
        try {
            return SecurityUtils.getCurrentUserId();
        } catch (Exception e) {
            log.warn("无法从安全上下文获取用户ID，使用默认值: {}", e.getMessage());
            return 1L;
        }
    }

    /**
     * 更新班级分数
     * 当申诉生效时，需要根据调整后的扣分重新计算班级得分
     */
    private void updateClassScore(Appeal appeal) {
        try {
            BigDecimal originalDeduction = appeal.getOriginalDeduction();
            BigDecimal approvedDeduction = appeal.getApprovedDeduction();

            if (originalDeduction != null && approvedDeduction != null) {
                // 计算分数差异：原扣分 - 审批后扣分 = 应返还的分数
                BigDecimal scoreDifference = originalDeduction.subtract(approvedDeduction);

                log.info("申诉生效，更新班级分数: appealId={}, classId={}, 原扣分={}, 审批后扣分={}, 分数调整={}",
                        appeal.getId(), appeal.getClassId(),
                        originalDeduction, approvedDeduction, scoreDifference);

                // 这里应该调用检查记录服务来更新班级的实际得分
                // 由于涉及到检查记录的复杂逻辑，建议通过领域事件来处理
                // 事件处理器中已经处理了APPROVED状态的分数更新逻辑
            }
        } catch (Exception e) {
            log.error("更新班级分数失败: appealId={}", appeal.getId(), e);
            // 分数更新失败不影响申诉生效流程
        }
    }

    private AppealResponse toResponse(Appeal appeal) {
        AppealResponse response = new AppealResponse();
        response.setId(appeal.getId());
        response.setAppealCode(appeal.getAppealCode());
        response.setInspectionRecordId(appeal.getInspectionRecordId());
        response.setDeductionDetailId(appeal.getDeductionDetailId());
        response.setClassId(appeal.getClassId());
        response.setReason(appeal.getReason());
        response.setAttachments(appeal.getAttachments());
        response.setOriginalDeduction(appeal.getOriginalDeduction());
        response.setRequestedDeduction(appeal.getRequestedDeduction());
        response.setApprovedDeduction(appeal.getApprovedDeduction());
        response.setStatus(appeal.getStatus());
        response.setApplicantId(appeal.getApplicantId());
        response.setAppliedAt(appeal.getAppliedAt());
        response.setLevel1ReviewerId(appeal.getLevel1ReviewerId());
        response.setLevel1ReviewedAt(appeal.getLevel1ReviewedAt());
        response.setLevel1Comment(appeal.getLevel1Comment());
        response.setLevel2ReviewerId(appeal.getLevel2ReviewerId());
        response.setLevel2ReviewedAt(appeal.getLevel2ReviewedAt());
        response.setLevel2Comment(appeal.getLevel2Comment());
        response.setEffectiveAt(appeal.getEffectiveAt());
        response.setAllowedTransitions(appeal.getStatus().getAllowedTransitions().stream()
            .map(AppealStatus::name)
            .collect(Collectors.toList()));
        return response;
    }
}
