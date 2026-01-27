package com.school.management.interfaces.rest.inspection;

import com.school.management.application.inspection.InspectionSessionApplicationService;
import com.school.management.application.inspection.command.*;
import com.school.management.common.result.Result;
import com.school.management.domain.inspection.model.*;
import com.school.management.interfaces.rest.inspection.dto.*;
import com.school.management.security.JwtTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for V4 inspection sessions.
 */
@RestController
@RequestMapping("/v2/inspection/sessions")
@Tag(name = "Inspection Sessions V4", description = "V4 inspection session management API")
public class InspectionSessionController {

    private final InspectionSessionApplicationService sessionService;
    private final JwtTokenService jwtTokenService;

    public InspectionSessionController(
            InspectionSessionApplicationService sessionService,
            JwtTokenService jwtTokenService) {
        this.sessionService = sessionService;
        this.jwtTokenService = jwtTokenService;
    }

    @PostMapping
    @Operation(summary = "Create a new inspection session")
    @PreAuthorize("hasAuthority('inspection:record:create')")
    public Result<SessionResponse> createSession(@Valid @RequestBody CreateSessionRequest request) {
        CreateSessionCommand command = CreateSessionCommand.builder()
            .templateId(request.getTemplateId())
            .inspectionDate(request.getInspectionDate())
            .inspectionPeriod(request.getInspectionPeriod())
            .inputMode(request.getInputMode() != null ? InputMode.valueOf(request.getInputMode()) : null)
            .scoringMode(request.getScoringMode() != null ? ScoringMode.valueOf(request.getScoringMode()) : null)
            .baseScore(request.getBaseScore())
            .inspectorId(getCurrentUserId())
            .inspectorName(getCurrentUserName())
            .createdBy(getCurrentUserId())
            .build();

        InspectionSession session = sessionService.createSession(command);
        return Result.success(SessionResponse.fromDomain(session));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get session by ID")
    @PreAuthorize("hasAuthority('inspection:record:view')")
    public Result<SessionResponse> getSession(@PathVariable Long id) {
        return sessionService.getSession(id)
            .map(s -> Result.success(SessionResponse.fromDomain(s)))
            .orElse(Result.error("Session not found"));
    }

    @GetMapping
    @Operation(summary = "List sessions by date range")
    @PreAuthorize("hasAuthority('inspection:record:view')")
    public Result<List<SessionResponse>> listSessions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<SessionResponse> sessions = sessionService.listSessionsByDateRange(startDate, endDate)
            .stream()
            .map(SessionResponse::fromDomain)
            .collect(Collectors.toList());
        return Result.success(sessions);
    }

    @GetMapping("/{id}/class-records")
    @Operation(summary = "Get class records for a session")
    @PreAuthorize("hasAuthority('inspection:record:view')")
    public Result<List<ClassRecordResponse>> getClassRecords(@PathVariable Long id) {
        List<ClassRecordResponse> records = sessionService.getSessionClassRecords(id)
            .stream()
            .map(ClassRecordResponse::fromDomain)
            .collect(Collectors.toList());
        return Result.success(records);
    }

    @PostMapping("/{id}/space-deductions")
    @Operation(summary = "Record a space-based deduction")
    @PreAuthorize("hasAuthority('inspection:record:edit')")
    public Result<List<ClassRecordResponse>> recordSpaceDeduction(
            @PathVariable Long id,
            @Valid @RequestBody SpaceDeductionRequest request) {

        SpaceDeductionCommand command = SpaceDeductionCommand.builder()
            .sessionId(id)
            .spaceType(SpaceType.valueOf(request.getSpaceType()))
            .spaceId(request.getSpaceId())
            .spaceName(request.getSpaceName())
            .deductionItemId(request.getDeductionItemId())
            .itemName(request.getItemName())
            .categoryName(request.getCategoryName())
            .deductionAmount(request.getDeductionAmount())
            .personCount(request.getPersonCount())
            .studentIds(request.getStudentIds())
            .studentNames(request.getStudentNames())
            .remark(request.getRemark())
            .evidenceUrls(request.getEvidenceUrls())
            .build();

        List<ClassInspectionRecord> records = sessionService.recordSpaceDeduction(command);
        return Result.success(records.stream()
            .map(ClassRecordResponse::fromDomain)
            .collect(Collectors.toList()));
    }

    @PostMapping("/{id}/person-deductions")
    @Operation(summary = "Record a person-based deduction")
    @PreAuthorize("hasAuthority('inspection:record:edit')")
    public Result<List<ClassRecordResponse>> recordPersonDeduction(
            @PathVariable Long id,
            @Valid @RequestBody PersonDeductionRequest request) {

        PersonDeductionCommand command = PersonDeductionCommand.builder()
            .sessionId(id)
            .studentIds(request.getStudentIds())
            .studentNames(request.getStudentNames())
            .deductionItemId(request.getDeductionItemId())
            .itemName(request.getItemName())
            .categoryName(request.getCategoryName())
            .deductionAmount(request.getDeductionAmount())
            .remark(request.getRemark())
            .evidenceUrls(request.getEvidenceUrls())
            .build();

        List<ClassInspectionRecord> records = sessionService.recordPersonDeduction(command);
        return Result.success(records.stream()
            .map(ClassRecordResponse::fromDomain)
            .collect(Collectors.toList()));
    }

    @PostMapping("/{id}/checklist-responses")
    @Operation(summary = "Submit checklist responses for a class")
    @PreAuthorize("hasAuthority('inspection:record:edit')")
    public Result<ClassRecordResponse> submitChecklistResponses(
            @PathVariable Long id,
            @Valid @RequestBody BatchChecklistRequest request) {

        BatchChecklistResponseCommand command = BatchChecklistResponseCommand.builder()
            .sessionId(id)
            .classId(request.getClassId())
            .items(request.getItems().stream()
                .map(item -> BatchChecklistResponseCommand.ChecklistItem.builder()
                    .checklistItemId(item.getChecklistItemId())
                    .itemName(item.getItemName())
                    .categoryName(item.getCategoryName())
                    .result(ChecklistResult.valueOf(item.getResult()))
                    .deductionWhenFail(item.getDeductionWhenFail())
                    .inspectorNote(item.getInspectorNote())
                    .build())
                .collect(java.util.stream.Collectors.toList()))
            .build();

        ClassInspectionRecord record = sessionService.submitChecklistResponses(command);
        return Result.success(ClassRecordResponse.fromDomain(record));
    }

    @GetMapping("/{id}/checklist-progress")
    @Operation(summary = "Get checklist progress for a session")
    @PreAuthorize("hasAuthority('inspection:record:view')")
    public Result<ChecklistProgressResponse> getChecklistProgress(@PathVariable Long id) {
        Map<String, Object> progress = sessionService.getChecklistProgress(id);
        return Result.success(ChecklistProgressResponse.from(progress));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Submit or publish a session")
    @PreAuthorize("hasAuthority('inspection:record:submit')")
    public Result<SessionResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam String action) {

        InspectionSession session;
        if ("submit".equalsIgnoreCase(action)) {
            session = sessionService.submitSession(id);
        } else if ("publish".equalsIgnoreCase(action)) {
            session = sessionService.publishSession(id);
        } else {
            return Result.error("Invalid action: " + action + ". Use 'submit' or 'publish'.");
        }
        return Result.success(SessionResponse.fromDomain(session));
    }

    private Long getCurrentUserId() {
        return jwtTokenService.getCurrentUserId();
    }

    private String getCurrentUserName() {
        return jwtTokenService.getCurrentUserName();
    }
}
