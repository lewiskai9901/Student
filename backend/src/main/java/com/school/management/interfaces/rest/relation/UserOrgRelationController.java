package com.school.management.interfaces.rest.relation;

import com.school.management.application.relation.UserOrgRelationApplicationService;
import com.school.management.application.relation.UserOrgRelationApplicationService.AddRelationCommand;
import com.school.management.application.relation.UserOrgRelationApplicationService.UpdateRelationCommand;
import com.school.management.domain.relation.model.entity.UserOrgRelation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户-组织关系控制器
 */
@Tag(name = "用户组织关系", description = "用户多归属关系管理")
@RestController
@RequestMapping("/v6/user-org-relations")
public class UserOrgRelationController {

    private final UserOrgRelationApplicationService service;

    public UserOrgRelationController(UserOrgRelationApplicationService service) {
        this.service = service;
    }

    @Operation(summary = "获取用户的所有组织关系")
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAuthority('user:read') or @securityService.isSelf(#userId)")
    public ResponseEntity<List<UserOrgRelationResponse>> getUserRelations(@PathVariable Long userId) {
        List<UserOrgRelation> relations = service.getUserRelations(userId);
        return ResponseEntity.ok(relations.stream()
                .map(this::toResponse)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "获取用户的有效组织关系")
    @GetMapping("/user/{userId}/active")
    @PreAuthorize("hasAuthority('user:read') or @securityService.isSelf(#userId)")
    public ResponseEntity<List<UserOrgRelationResponse>> getUserActiveRelations(@PathVariable Long userId) {
        List<UserOrgRelation> relations = service.getUserActiveRelations(userId);
        return ResponseEntity.ok(relations.stream()
                .map(this::toResponse)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "获取用户的主归属")
    @GetMapping("/user/{userId}/primary")
    @PreAuthorize("hasAuthority('user:read') or @securityService.isSelf(#userId)")
    public ResponseEntity<UserOrgRelationResponse> getUserPrimaryRelation(@PathVariable Long userId) {
        UserOrgRelation relation = service.getUserPrimaryRelation(userId);
        if (relation == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toResponse(relation));
    }

    @Operation(summary = "获取组织的成员关系")
    @GetMapping("/org/{orgUnitId}")
    @PreAuthorize("hasAuthority('org_unit:read')")
    public ResponseEntity<List<UserOrgRelationResponse>> getOrgMembers(@PathVariable Long orgUnitId) {
        List<UserOrgRelation> relations = service.getOrgMembers(orgUnitId);
        return ResponseEntity.ok(relations.stream()
                .map(this::toResponse)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "获取组织的领导")
    @GetMapping("/org/{orgUnitId}/leaders")
    @PreAuthorize("hasAuthority('org_unit:read')")
    public ResponseEntity<List<UserOrgRelationResponse>> getOrgLeaders(@PathVariable Long orgUnitId) {
        List<UserOrgRelation> relations = service.getOrgLeaders(orgUnitId);
        return ResponseEntity.ok(relations.stream()
                .map(this::toResponse)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "获取关系详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('user:read')")
    public ResponseEntity<UserOrgRelationResponse> getRelation(@PathVariable Long id) {
        UserOrgRelation relation = service.getRelation(id);
        return ResponseEntity.ok(toResponse(relation));
    }

    @Operation(summary = "添加用户组织关系")
    @PostMapping
    @PreAuthorize("hasAuthority('user:write')")
    public ResponseEntity<UserOrgRelationResponse> addRelation(@RequestBody AddRelationRequest request) {
        AddRelationCommand command = new AddRelationCommand();
        command.setUserId(request.getUserId());
        command.setOrgUnitId(request.getOrgUnitId());
        command.setRelationType(UserOrgRelation.RelationType.valueOf(request.getRelationType()));
        command.setPositionTitle(request.getPositionTitle());
        command.setPositionLevel(request.getPositionLevel());
        command.setPrimary(request.isPrimary());
        command.setLeader(request.isLeader());
        command.setCanManage(request.isCanManage());
        command.setCanApprove(request.isCanApprove());
        command.setStartDate(request.getStartDate());
        command.setEndDate(request.getEndDate());
        command.setWeightRatio(request.getWeightRatio());
        command.setSortOrder(request.getSortOrder());
        command.setRemark(request.getRemark());
        // TODO: 从SecurityContext获取当前用户ID
        command.setOperatorId(1L);

        UserOrgRelation relation = service.addRelation(command);
        return ResponseEntity.ok(toResponse(relation));
    }

    @Operation(summary = "更新用户组织关系")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('user:write')")
    public ResponseEntity<UserOrgRelationResponse> updateRelation(
            @PathVariable Long id,
            @RequestBody UpdateRelationRequest request) {
        UpdateRelationCommand command = new UpdateRelationCommand();
        command.setPositionTitle(request.getPositionTitle());
        command.setPositionLevel(request.getPositionLevel());
        command.setLeader(request.isLeader());
        command.setCanManage(request.isCanManage());
        command.setCanApprove(request.isCanApprove());
        command.setStartDate(request.getStartDate());
        command.setEndDate(request.getEndDate());
        command.setWeightRatio(request.getWeightRatio());
        command.setSortOrder(request.getSortOrder());
        command.setRemark(request.getRemark());

        UserOrgRelation relation = service.updateRelation(id, command);
        return ResponseEntity.ok(toResponse(relation));
    }

    @Operation(summary = "设置主归属")
    @PutMapping("/user/{userId}/primary/{relationId}")
    @PreAuthorize("hasAuthority('user:write')")
    public ResponseEntity<UserOrgRelationResponse> setPrimary(
            @PathVariable Long userId,
            @PathVariable Long relationId) {
        UserOrgRelation relation = service.setPrimary(userId, relationId);
        return ResponseEntity.ok(toResponse(relation));
    }

    @Operation(summary = "删除用户组织关系")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('user:write')")
    public ResponseEntity<Void> deleteRelation(@PathVariable Long id) {
        service.deleteRelation(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== 请求/响应对象 ====================

    private UserOrgRelationResponse toResponse(UserOrgRelation relation) {
        UserOrgRelationResponse response = new UserOrgRelationResponse();
        response.setId(relation.getId());
        response.setUserId(relation.getUserId());
        response.setOrgUnitId(relation.getOrgUnitId());
        response.setRelationType(relation.getRelationType().name());
        response.setRelationTypeLabel(relation.getRelationType().getLabel());
        response.setPositionTitle(relation.getPositionTitle());
        response.setPositionLevel(relation.getPositionLevel());
        response.setPrimary(relation.isPrimary());
        response.setLeader(relation.isLeader());
        response.setCanManage(relation.isCanManage());
        response.setCanApprove(relation.isCanApprove());
        response.setStartDate(relation.getStartDate());
        response.setEndDate(relation.getEndDate());
        response.setWeightRatio(relation.getWeightRatio());
        response.setSortOrder(relation.getSortOrder());
        response.setRemark(relation.getRemark());
        response.setActive(relation.isActive());
        response.setExpired(relation.isExpired());
        response.setExpiringSoon(relation.isExpiringSoon(7));
        return response;
    }

    // ==================== DTO类 ====================

    public static class AddRelationRequest {
        private Long userId;
        private Long orgUnitId;
        private String relationType;
        private String positionTitle;
        private Integer positionLevel;
        private boolean isPrimary;
        private boolean isLeader;
        private boolean canManage;
        private boolean canApprove;
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal weightRatio;
        private Integer sortOrder;
        private String remark;

        // Getters and Setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public Long getOrgUnitId() { return orgUnitId; }
        public void setOrgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; }
        public String getRelationType() { return relationType; }
        public void setRelationType(String relationType) { this.relationType = relationType; }
        public String getPositionTitle() { return positionTitle; }
        public void setPositionTitle(String positionTitle) { this.positionTitle = positionTitle; }
        public Integer getPositionLevel() { return positionLevel; }
        public void setPositionLevel(Integer positionLevel) { this.positionLevel = positionLevel; }
        public boolean isPrimary() { return isPrimary; }
        public void setPrimary(boolean primary) { isPrimary = primary; }
        public boolean isLeader() { return isLeader; }
        public void setLeader(boolean leader) { isLeader = leader; }
        public boolean isCanManage() { return canManage; }
        public void setCanManage(boolean canManage) { this.canManage = canManage; }
        public boolean isCanApprove() { return canApprove; }
        public void setCanApprove(boolean canApprove) { this.canApprove = canApprove; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        public BigDecimal getWeightRatio() { return weightRatio; }
        public void setWeightRatio(BigDecimal weightRatio) { this.weightRatio = weightRatio; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }

    public static class UpdateRelationRequest {
        private String positionTitle;
        private Integer positionLevel;
        private boolean isLeader;
        private boolean canManage;
        private boolean canApprove;
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal weightRatio;
        private Integer sortOrder;
        private String remark;

        // Getters and Setters
        public String getPositionTitle() { return positionTitle; }
        public void setPositionTitle(String positionTitle) { this.positionTitle = positionTitle; }
        public Integer getPositionLevel() { return positionLevel; }
        public void setPositionLevel(Integer positionLevel) { this.positionLevel = positionLevel; }
        public boolean isLeader() { return isLeader; }
        public void setLeader(boolean leader) { isLeader = leader; }
        public boolean isCanManage() { return canManage; }
        public void setCanManage(boolean canManage) { this.canManage = canManage; }
        public boolean isCanApprove() { return canApprove; }
        public void setCanApprove(boolean canApprove) { this.canApprove = canApprove; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        public BigDecimal getWeightRatio() { return weightRatio; }
        public void setWeightRatio(BigDecimal weightRatio) { this.weightRatio = weightRatio; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }

    public static class UserOrgRelationResponse {
        private Long id;
        private Long userId;
        private Long orgUnitId;
        private String relationType;
        private String relationTypeLabel;
        private String positionTitle;
        private Integer positionLevel;
        private boolean isPrimary;
        private boolean isLeader;
        private boolean canManage;
        private boolean canApprove;
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal weightRatio;
        private Integer sortOrder;
        private String remark;
        private boolean isActive;
        private boolean isExpired;
        private boolean isExpiringSoon;

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public Long getOrgUnitId() { return orgUnitId; }
        public void setOrgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; }
        public String getRelationType() { return relationType; }
        public void setRelationType(String relationType) { this.relationType = relationType; }
        public String getRelationTypeLabel() { return relationTypeLabel; }
        public void setRelationTypeLabel(String relationTypeLabel) { this.relationTypeLabel = relationTypeLabel; }
        public String getPositionTitle() { return positionTitle; }
        public void setPositionTitle(String positionTitle) { this.positionTitle = positionTitle; }
        public Integer getPositionLevel() { return positionLevel; }
        public void setPositionLevel(Integer positionLevel) { this.positionLevel = positionLevel; }
        public boolean isPrimary() { return isPrimary; }
        public void setPrimary(boolean primary) { isPrimary = primary; }
        public boolean isLeader() { return isLeader; }
        public void setLeader(boolean leader) { isLeader = leader; }
        public boolean isCanManage() { return canManage; }
        public void setCanManage(boolean canManage) { this.canManage = canManage; }
        public boolean isCanApprove() { return canApprove; }
        public void setCanApprove(boolean canApprove) { this.canApprove = canApprove; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        public BigDecimal getWeightRatio() { return weightRatio; }
        public void setWeightRatio(BigDecimal weightRatio) { this.weightRatio = weightRatio; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
        public boolean isActive() { return isActive; }
        public void setActive(boolean active) { isActive = active; }
        public boolean isExpired() { return isExpired; }
        public void setExpired(boolean expired) { isExpired = expired; }
        public boolean isExpiringSoon() { return isExpiringSoon; }
        public void setExpiringSoon(boolean expiringSoon) { isExpiringSoon = expiringSoon; }
    }
}
