package com.school.management.interfaces.rest.behavior;

import com.school.management.application.behavior.BehaviorApplicationService;
import com.school.management.application.behavior.command.CreateBehaviorRecordCommand;
import com.school.management.application.behavior.query.BehaviorProfileDTO;
import com.school.management.common.result.Result;
import com.school.management.domain.behavior.model.*;
import com.school.management.security.JwtTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v2/behavior-records")
@Tag(name = "Behavior Records", description = "学生行为记录管理")
public class BehaviorRecordController {

    private final BehaviorApplicationService service;
    private final JwtTokenService jwtTokenService;

    public BehaviorRecordController(BehaviorApplicationService service,
                                     JwtTokenService jwtTokenService) {
        this.service = service;
        this.jwtTokenService = jwtTokenService;
    }

    @PostMapping
    @Operation(summary = "创建行为记录")
    @PreAuthorize("hasAuthority('behavior:record:create')")
    public Result<BehaviorRecordResponse> create(@Valid @RequestBody CreateBehaviorRecordRequest request) {
        CreateBehaviorRecordCommand command = CreateBehaviorRecordCommand.builder()
                .studentId(request.getStudentId())
                .classId(request.getClassId())
                .behaviorType(BehaviorType.valueOf(request.getBehaviorType()))
                .source(BehaviorSource.valueOf(request.getSource()))
                .sourceId(request.getSourceId())
                .category(BehaviorCategory.valueOf(request.getCategory()))
                .title(request.getTitle())
                .detail(request.getDetail())
                .deductionAmount(request.getDeductionAmount())
                .recordedBy(jwtTokenService.getCurrentUserId())
                .build();

        BehaviorRecord record = service.createRecord(command);
        return Result.success(BehaviorRecordResponse.fromDomain(record));
    }

    @GetMapping("/class/{classId}")
    @Operation(summary = "获取班级行为记录")
    @PreAuthorize("hasAuthority('behavior:record:view')")
    public Result<List<BehaviorRecordResponse>> listByClass(@PathVariable Long classId) {
        return Result.success(service.listByClassId(classId).stream()
                .map(BehaviorRecordResponse::fromDomain)
                .collect(Collectors.toList()));
    }

    @GetMapping("/student/{studentId}")
    @Operation(summary = "获取学生行为记录")
    @PreAuthorize("hasAuthority('behavior:record:view')")
    public Result<List<BehaviorRecordResponse>> listByStudent(@PathVariable Long studentId) {
        return Result.success(service.listByStudentId(studentId).stream()
                .map(BehaviorRecordResponse::fromDomain)
                .collect(Collectors.toList()));
    }

    @GetMapping("/student/{studentId}/profile")
    @Operation(summary = "获取学生行为画像")
    @PreAuthorize("hasAuthority('behavior:record:view')")
    public Result<BehaviorProfileDTO> getProfile(@PathVariable Long studentId) {
        return Result.success(service.getStudentProfile(studentId));
    }

    @PutMapping("/{id}/acknowledge")
    @Operation(summary = "确认行为记录")
    @PreAuthorize("hasAuthority('behavior:record:resolve')")
    public Result<BehaviorRecordResponse> acknowledge(@PathVariable Long id) {
        BehaviorRecord record = service.acknowledge(id);
        return Result.success(BehaviorRecordResponse.fromDomain(record));
    }

    @PutMapping("/{id}/resolve")
    @Operation(summary = "处理行为记录")
    @PreAuthorize("hasAuthority('behavior:record:resolve')")
    public Result<BehaviorRecordResponse> resolve(@PathVariable Long id, @RequestBody Map<String, String> body) {
        BehaviorRecord record = service.resolve(id, body.get("resolutionNote"));
        return Result.success(BehaviorRecordResponse.fromDomain(record));
    }
}
