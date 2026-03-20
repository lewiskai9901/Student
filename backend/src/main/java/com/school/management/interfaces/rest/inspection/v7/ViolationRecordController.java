package com.school.management.interfaces.rest.inspection.v7;

import com.school.management.application.inspection.v7.ViolationRecordApplicationService;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.domain.inspection.model.v7.execution.ViolationRecord;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 违纪记录控制器
 * 管理检查过程中产生的违纪记录。
 */
@Slf4j
@RestController
@RequestMapping("/v7/insp/violation-records")
@RequiredArgsConstructor
public class ViolationRecordController {

    private final ViolationRecordApplicationService violationRecordService;

    @PostMapping
    @CasbinAccess(resource = "insp:violation", action = "create")
    public Result<ViolationRecord> createViolationRecord(@RequestBody CreateViolationRecordRequest request) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        return Result.success(violationRecordService.createViolationRecord(
                request.submissionId(), request.submissionDetailId(),
                request.sectionId(), request.itemId(),
                request.userId(), request.userName(), request.classInfo(),
                request.occurredAt(), request.severity(),
                request.description(), request.evidenceUrls(),
                request.score(), operatorId));
    }

    @GetMapping
    @CasbinAccess(resource = "insp:violation", action = "view")
    public Result<List<ViolationRecord>> listBySubmission(@RequestParam Long submissionId) {
        return Result.success(violationRecordService.listBySubmission(submissionId));
    }

    @GetMapping("/by-user/{userId}")
    @CasbinAccess(resource = "insp:violation", action = "view")
    public Result<List<ViolationRecord>> listByUser(@PathVariable Long userId) {
        return Result.success(violationRecordService.listByUser(userId));
    }

    @GetMapping("/{id}")
    @CasbinAccess(resource = "insp:violation", action = "view")
    public Result<ViolationRecord> getViolationRecord(@PathVariable Long id) {
        return Result.success(violationRecordService.getViolationRecord(id)
                .orElseThrow(() -> new IllegalArgumentException("违纪记录不存在: " + id)));
    }

    @PutMapping("/{id}")
    @CasbinAccess(resource = "insp:violation", action = "edit")
    public Result<ViolationRecord> updateViolationRecord(@PathVariable Long id,
                                                          @RequestBody UpdateViolationRecordRequest request) {
        return Result.success(violationRecordService.updateViolationRecord(id,
                request.description(), request.severity(), request.score(),
                request.evidenceUrls(), request.classInfo()));
    }

    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "insp:violation", action = "delete")
    public Result<Void> deleteViolationRecord(@PathVariable Long id) {
        violationRecordService.deleteViolationRecord(id);
        return Result.success();
    }

    // --- Request DTOs ---

    public record CreateViolationRecordRequest(
            Long submissionId,
            Long submissionDetailId,
            Long sectionId,
            Long itemId,
            Long userId,
            String userName,
            String classInfo,
            LocalDateTime occurredAt,
            String severity,
            String description,
            String evidenceUrls,
            BigDecimal score
    ) {}

    public record UpdateViolationRecordRequest(
            String description,
            String severity,
            BigDecimal score,
            String evidenceUrls,
            String classInfo
    ) {}
}
