package com.school.management.interfaces.rest.corrective;

import com.school.management.application.corrective.CorrectiveActionApplicationService;
import com.school.management.application.corrective.command.CreateActionCommand;
import com.school.management.application.corrective.command.ResolveActionCommand;
import com.school.management.application.corrective.command.VerifyActionCommand;
import com.school.management.common.result.Result;
import com.school.management.domain.corrective.model.*;
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
@RequestMapping("/v2/corrective-actions")
@Tag(name = "Corrective Actions", description = "整改工单管理")
public class CorrectiveActionController {

    private final CorrectiveActionApplicationService service;
    private final JwtTokenService jwtTokenService;

    public CorrectiveActionController(CorrectiveActionApplicationService service,
                                       JwtTokenService jwtTokenService) {
        this.service = service;
        this.jwtTokenService = jwtTokenService;
    }

    @PostMapping
    @Operation(summary = "创建整改工单")
    @PreAuthorize("hasAuthority('corrective:action:create')")
    public Result<ActionResponse> create(@Valid @RequestBody CreateActionRequest request) {
        CreateActionCommand command = CreateActionCommand.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .source(ActionSource.valueOf(request.getSource()))
                .sourceId(request.getSourceId())
                .severity(ActionSeverity.valueOf(request.getSeverity()))
                .category(ActionCategory.valueOf(request.getCategory()))
                .classId(request.getClassId())
                .assigneeId(request.getAssigneeId())
                .deadline(request.getDeadline())
                .createdBy(jwtTokenService.getCurrentUserId())
                .build();

        CorrectiveAction action = service.createAction(command);
        return Result.success(ActionResponse.fromDomain(action));
    }

    @GetMapping
    @Operation(summary = "查询整改工单列表")
    @PreAuthorize("hasAuthority('corrective:action:view')")
    public Result<List<ActionResponse>> list(@RequestParam(required = false) String status,
                                              @RequestParam(required = false) Long classId,
                                              @RequestParam(required = false) Long assigneeId) {
        List<CorrectiveAction> actions;
        if (status != null) {
            actions = service.listByStatus(ActionStatus.valueOf(status));
        } else if (classId != null) {
            actions = service.listByClassId(classId);
        } else if (assigneeId != null) {
            actions = service.listByAssigneeId(assigneeId);
        } else {
            actions = service.listByStatus(ActionStatus.OPEN);
        }
        return Result.success(actions.stream()
                .map(ActionResponse::fromDomain)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取工单详情")
    @PreAuthorize("hasAuthority('corrective:action:view')")
    public Result<ActionResponse> getById(@PathVariable Long id) {
        return service.getAction(id)
                .map(a -> Result.success(ActionResponse.fromDomain(a)))
                .orElse(Result.error("Action not found"));
    }

    @PutMapping("/{id}/start")
    @Operation(summary = "开始整改")
    @PreAuthorize("hasAuthority('corrective:action:process')")
    public Result<ActionResponse> start(@PathVariable Long id) {
        CorrectiveAction action = service.startProgress(id);
        return Result.success(ActionResponse.fromDomain(action));
    }

    @PutMapping("/{id}/resolve")
    @Operation(summary = "提交整改")
    @PreAuthorize("hasAuthority('corrective:action:process')")
    public Result<ActionResponse> resolve(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String note = (String) body.get("resolutionNote");
        @SuppressWarnings("unchecked")
        List<String> attachments = (List<String>) body.get("attachments");

        ResolveActionCommand command = ResolveActionCommand.builder()
                .actionId(id)
                .resolutionNote(note)
                .attachments(attachments)
                .build();

        CorrectiveAction action = service.resolve(command);
        return Result.success(ActionResponse.fromDomain(action));
    }

    @PutMapping("/{id}/verify")
    @Operation(summary = "验证整改")
    @PreAuthorize("hasAuthority('corrective:action:verify')")
    public Result<ActionResponse> verify(@PathVariable Long id, @RequestBody Map<String, String> body) {
        VerifyActionCommand command = VerifyActionCommand.builder()
                .actionId(id)
                .verifierId(jwtTokenService.getCurrentUserId())
                .result(body.get("result"))
                .comment(body.get("comment"))
                .build();

        CorrectiveAction action = service.verify(command);
        return Result.success(ActionResponse.fromDomain(action));
    }

    @PutMapping("/{id}/escalate")
    @Operation(summary = "升级工单")
    @PreAuthorize("hasAuthority('corrective:action:verify')")
    public Result<ActionResponse> escalate(@PathVariable Long id) {
        CorrectiveAction action = service.escalate(id);
        return Result.success(ActionResponse.fromDomain(action));
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取工单统计")
    @PreAuthorize("hasAuthority('corrective:action:view')")
    public Result<Map<String, Long>> statistics() {
        return Result.success(service.getStatistics());
    }
}
