package com.school.management.interfaces.rest.inspection;

import com.school.management.application.inspection.*;
import com.school.management.common.result.Result;
import com.school.management.domain.inspection.model.InspectionRecord;
import com.school.management.domain.inspection.model.RecordStatus;
import com.school.management.security.JwtTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for inspection records.
 */
@RestController
@RequestMapping("/v2/inspection-records")
@Tag(name = "Inspection Records", description = "Inspection record management API")
public class InspectionRecordController {

    private final InspectionApplicationService inspectionService;
    private final JwtTokenService jwtTokenService;

    public InspectionRecordController(
            InspectionApplicationService inspectionService,
            JwtTokenService jwtTokenService) {
        this.inspectionService = inspectionService;
        this.jwtTokenService = jwtTokenService;
    }

    @PostMapping
    @Operation(summary = "Create a new inspection record")
    @PreAuthorize("hasAuthority('inspection:record:create')")
    public Result<RecordResponse> createRecord(@Valid @RequestBody CreateRecordRequest request) {
        CreateRecordCommand command = CreateRecordCommand.builder()
            .templateId(request.getTemplateId())
            .roundId(request.getRoundId())
            .inspectionDate(request.getInspectionDate())
            .inspectionPeriod(request.getInspectionPeriod())
            .inspectorId(getCurrentUserId())
            .inspectorName(getCurrentUserName())
            .createdBy(getCurrentUserId())
            .build();

        InspectionRecord record = inspectionService.createRecord(command);
        return Result.success(toResponse(record));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get record by ID")
    @PreAuthorize("hasAuthority('inspection:record:view')")
    public Result<RecordResponse> getRecord(@PathVariable Long id) {
        return inspectionService.getRecord(id)
            .map(record -> Result.success(toResponse(record)))
            .orElse(Result.error("Record not found"));
    }

    @GetMapping
    @Operation(summary = "List records by date range")
    @PreAuthorize("hasAuthority('inspection:record:view')")
    public Result<List<RecordResponse>> listRecordsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<RecordResponse> records = inspectionService.listRecordsByDateRange(startDate, endDate)
            .stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        return Result.success(records);
    }

    @PostMapping("/{id}/class-scores")
    @Operation(summary = "Add a class score to record")
    @PreAuthorize("hasAuthority('inspection:record:edit')")
    public Result<RecordResponse> addClassScore(
            @PathVariable Long id,
            @Valid @RequestBody AddClassScoreRequest request) {

        InspectionRecord record = inspectionService.addClassScore(
            id,
            request.getClassId(),
            request.getClassName(),
            request.getBaseScore()
        );
        return Result.success(toResponse(record));
    }

    @PostMapping("/{id}/deductions")
    @Operation(summary = "Record a deduction")
    @PreAuthorize("hasAuthority('inspection:record:edit')")
    public Result<RecordResponse> recordDeduction(
            @PathVariable Long id,
            @Valid @RequestBody RecordDeductionRequest request) {

        RecordDeductionCommand command = RecordDeductionCommand.builder()
            .classId(request.getClassId())
            .deductionItemId(request.getDeductionItemId())
            .itemName(request.getItemName())
            .count(request.getCount())
            .deductionAmount(request.getDeductionAmount())
            .remark(request.getRemark())
            .evidenceUrls(request.getEvidenceUrls())
            .build();

        InspectionRecord record = inspectionService.recordDeduction(id, command);
        return Result.success(toResponse(record));
    }

    @PutMapping("/{id}/submit")
    @Operation(summary = "Submit record for review")
    @PreAuthorize("hasAuthority('inspection:record:submit')")
    public Result<RecordResponse> submitRecord(@PathVariable Long id) {
        InspectionRecord record = inspectionService.submitRecord(id);
        return Result.success(toResponse(record));
    }

    @PutMapping("/{id}/approve")
    @Operation(summary = "Approve a submitted record")
    @PreAuthorize("hasAuthority('inspection:record:review')")
    public Result<RecordResponse> approveRecord(@PathVariable Long id) {
        InspectionRecord record = inspectionService.approveRecord(id, getCurrentUserId());
        return Result.success(toResponse(record));
    }

    @PutMapping("/{id}/reject")
    @Operation(summary = "Reject a submitted record")
    @PreAuthorize("hasAuthority('inspection:record:review')")
    public Result<RecordResponse> rejectRecord(
            @PathVariable Long id,
            @RequestBody RejectRecordRequest request) {
        InspectionRecord record = inspectionService.rejectRecord(id, getCurrentUserId(), request.getReason());
        return Result.success(toResponse(record));
    }

    @PutMapping("/{id}/publish")
    @Operation(summary = "Publish an approved record")
    @PreAuthorize("hasAuthority('inspection:record:publish')")
    public Result<RecordResponse> publishRecord(@PathVariable Long id) {
        InspectionRecord record = inspectionService.publishRecord(id);
        return Result.success(toResponse(record));
    }

    private Long getCurrentUserId() {
        return jwtTokenService.getCurrentUserId();
    }

    private String getCurrentUserName() {
        return jwtTokenService.getCurrentUserName();
    }

    private RecordResponse toResponse(InspectionRecord record) {
        RecordResponse response = new RecordResponse();
        response.setId(record.getId());
        response.setRecordCode(record.getRecordCode());
        response.setTemplateId(record.getTemplateId());
        response.setTemplateVersion(record.getTemplateVersion());
        response.setRoundId(record.getRoundId());
        response.setInspectionDate(record.getInspectionDate());
        response.setInspectionPeriod(record.getInspectionPeriod());
        response.setStatus(record.getStatus());
        response.setInspectorId(record.getInspectorId());
        response.setInspectorName(record.getInspectorName());
        response.setInspectedAt(record.getInspectedAt());
        response.setReviewerId(record.getReviewerId());
        response.setReviewedAt(record.getReviewedAt());
        response.setPublishedAt(record.getPublishedAt());
        response.setRemarks(record.getRemarks());
        response.setCreatedAt(record.getCreatedAt());
        response.setClassCount(record.getClassScores().size());
        response.setTotalDeductionCount(record.getTotalDeductionCount());
        response.setAverageScore(record.calculateAverageScore());
        return response;
    }
}
