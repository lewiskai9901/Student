package com.school.management.interfaces.rest.organization;

import com.school.management.application.organization.UserPositionApplicationService;
import com.school.management.application.organization.command.AppointUserCommand;
import com.school.management.application.organization.command.EndAppointmentCommand;
import com.school.management.application.organization.command.TransferUserCommand;
import com.school.management.application.organization.query.OrgMemberDTO;
import com.school.management.application.organization.query.OrgStatisticsDTO;
import com.school.management.application.organization.query.UserPositionDTO;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.infrastructure.casbin.CasbinAccess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "User Positions", description = "人岗关系管理")
@RestController
@RequestMapping("/user-positions")
@RequiredArgsConstructor
public class UserPositionController {

    private final UserPositionApplicationService service;

    @Operation(summary = "任命")
    @PostMapping("/appoint")
    @CasbinAccess(resource = "system:org", action = "update")
    public Result<UserPositionDTO> appoint(@RequestBody AppointRequest request) {
        AppointUserCommand cmd = AppointUserCommand.builder()
            .userId(request.getUserId())
            .positionId(request.getPositionId())
            .primary(request.isPrimary())
            .appointmentType(request.getAppointmentType())
            .startDate(request.getStartDate() != null ? LocalDate.parse(request.getStartDate()) : LocalDate.now())
            .reason(request.getReason())
            .createdBy(SecurityUtils.requireCurrentUserId())
            .build();
        return Result.success(service.appointUser(cmd));
    }

    @Operation(summary = "离任")
    @PutMapping("/{id}/end")
    @CasbinAccess(resource = "system:org", action = "update")
    public Result<Void> endAppointment(@PathVariable Long id, @RequestBody EndRequest request) {
        EndAppointmentCommand cmd = EndAppointmentCommand.builder()
            .endDate(request.getEndDate() != null ? LocalDate.parse(request.getEndDate()) : LocalDate.now())
            .reason(request.getReason())
            .build();
        service.endAppointment(id, cmd);
        return Result.success();
    }

    @Operation(summary = "调岗")
    @PostMapping("/transfer")
    @CasbinAccess(resource = "system:org", action = "update")
    public Result<Void> transfer(@RequestBody TransferRequest request) {
        TransferUserCommand cmd = TransferUserCommand.builder()
            .userId(request.getUserId())
            .fromPositionId(request.getFromPositionId())
            .toPositionId(request.getToPositionId())
            .transferDate(request.getTransferDate() != null ? LocalDate.parse(request.getTransferDate()) : LocalDate.now())
            .reason(request.getReason())
            .operatedBy(SecurityUtils.requireCurrentUserId())
            .build();
        service.transferUser(cmd);
        return Result.success();
    }

    @Operation(summary = "用户岗位列表（含历史）")
    @GetMapping
    @CasbinAccess(resource = "system:org", action = "view")
    public Result<List<UserPositionDTO>> listByUser(@RequestParam Long userId) {
        return Result.success(service.getUserPositions(userId));
    }

    @Operation(summary = "用户主岗")
    @GetMapping("/primary")
    @CasbinAccess(resource = "system:org", action = "view")
    public Result<UserPositionDTO> getPrimary(@RequestParam Long userId) {
        return Result.success(service.getPrimaryPosition(userId));
    }

    @Operation(summary = "组织成员列表（基于岗位）")
    @GetMapping("/org-members")
    @CasbinAccess(resource = "system:org", action = "view")
    public Result<List<OrgMemberDTO>> listOrgMembers(@RequestParam Long orgUnitId) {
        return Result.success(service.getMembersByOrgUnit(orgUnitId));
    }

    @Operation(summary = "组织归属成员列表（primary_org_unit_id）")
    @GetMapping("/belonging-members")
    @CasbinAccess(resource = "system:org", action = "view")
    public Result<List<OrgMemberDTO>> listBelongingMembers(@RequestParam Long orgUnitId) {
        return Result.success(service.getBelongingMembers(orgUnitId));
    }

    @Operation(summary = "组织岗位人员（含所有子组织，递归）")
    @GetMapping("/org-members-recursive")
    @CasbinAccess(resource = "system:org", action = "view")
    public Result<List<OrgMemberDTO>> listOrgMembersRecursive(@RequestParam Long orgUnitId) {
        return Result.success(service.getMembersRecursive(orgUnitId));
    }

    @Operation(summary = "组织统计（多维度成员计数）")
    @GetMapping("/org-statistics")
    @CasbinAccess(resource = "system:org", action = "view")
    public Result<OrgStatisticsDTO> getOrgStatistics(@RequestParam Long orgUnitId) {
        return Result.success(service.getOrgStatistics(orgUnitId));
    }

    @Operation(summary = "添加成员到组织")
    @PostMapping("/add-member")
    @CasbinAccess(resource = "system:org", action = "update")
    public Result<Void> addMember(@RequestBody AddMemberRequest request) {
        service.addMemberToOrg(request.getOrgUnitId(), request.getUserId(), SecurityUtils.requireCurrentUserId());
        return Result.success();
    }

    @Operation(summary = "从组织移除成员")
    @PostMapping("/remove-member")
    @CasbinAccess(resource = "system:org", action = "update")
    public Result<Void> removeMember(@RequestBody RemoveMemberRequest request) {
        service.removeMemberFromOrg(request.getOrgUnitId(), request.getUserId(), SecurityUtils.requireCurrentUserId());
        return Result.success();
    }

    // ==================== Request DTOs ====================

    @Data
    public static class AppointRequest {
        private Long userId;
        private Long positionId;
        private boolean primary;
        private String appointmentType;
        private String startDate;
        private String reason;
    }

    @Data
    public static class EndRequest {
        private String endDate;
        private String reason;
    }

    @Data
    public static class TransferRequest {
        private Long userId;
        private Long fromPositionId;
        private Long toPositionId;
        private String transferDate;
        private String reason;
    }

    @Data
    public static class AddMemberRequest {
        private Long orgUnitId;
        private Long userId;
    }

    @Data
    public static class RemoveMemberRequest {
        private Long orgUnitId;
        private Long userId;
    }
}
