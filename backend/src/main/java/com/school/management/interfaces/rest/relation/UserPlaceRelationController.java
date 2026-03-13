package com.school.management.interfaces.rest.relation;

import com.school.management.application.relation.UserSpaceRelationApplicationService;
import com.school.management.application.relation.UserSpaceRelationApplicationService.AddRelationCommand;
import com.school.management.application.relation.UserSpaceRelationApplicationService.UpdateRelationCommand;
import com.school.management.domain.relation.model.entity.UserSpaceRelation;
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
 * 用户-场所关系控制器
 */
@Tag(name = "用户场所关系", description = "用户场所分配管理")
@RestController
@RequestMapping("/v6/user-space-relations")
public class UserSpaceRelationController {

    private final UserSpaceRelationApplicationService service;

    public UserSpaceRelationController(UserSpaceRelationApplicationService service) {
        this.service = service;
    }

    @Operation(summary = "获取用户的所有场所关系")
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAuthority('user:read') or @securityService.isSelf(#userId)")
    public ResponseEntity<List<UserSpaceRelationResponse>> getUserRelations(@PathVariable Long userId) {
        List<UserSpaceRelation> relations = service.getUserRelations(userId);
        return ResponseEntity.ok(relations.stream()
                .map(this::toResponse)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "获取用户的有效场所关系")
    @GetMapping("/user/{userId}/active")
    @PreAuthorize("hasAuthority('user:read') or @securityService.isSelf(#userId)")
    public ResponseEntity<List<UserSpaceRelationResponse>> getUserActiveRelations(@PathVariable Long userId) {
        List<UserSpaceRelation> relations = service.getUserActiveRelations(userId);
        return ResponseEntity.ok(relations.stream()
                .map(this::toResponse)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "获取用户的主要场所")
    @GetMapping("/user/{userId}/primary")
    @PreAuthorize("hasAuthority('user:read') or @securityService.isSelf(#userId)")
    public ResponseEntity<UserSpaceRelationResponse> getUserPrimaryRelation(@PathVariable Long userId) {
        UserSpaceRelation relation = service.getUserPrimaryRelation(userId);
        if (relation == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toResponse(relation));
    }

    @Operation(summary = "获取用户未缴费的场所")
    @GetMapping("/user/{userId}/unpaid")
    @PreAuthorize("hasAuthority('user:read') or @securityService.isSelf(#userId)")
    public ResponseEntity<List<UserSpaceRelationResponse>> getUnpaidRelations(@PathVariable Long userId) {
        List<UserSpaceRelation> relations = service.getUnpaidRelations(userId);
        return ResponseEntity.ok(relations.stream()
                .map(this::toResponse)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "获取场所的用户关系")
    @GetMapping("/space/{spaceId}")
    @PreAuthorize("hasAuthority('space:read')")
    public ResponseEntity<List<UserSpaceRelationResponse>> getSpaceUsers(@PathVariable Long spaceId) {
        List<UserSpaceRelation> relations = service.getSpaceUsers(spaceId);
        return ResponseEntity.ok(relations.stream()
                .map(this::toResponse)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "获取场所的分配用户")
    @GetMapping("/space/{spaceId}/assigned")
    @PreAuthorize("hasAuthority('space:read')")
    public ResponseEntity<List<UserSpaceRelationResponse>> getSpaceAssignedUsers(@PathVariable Long spaceId) {
        List<UserSpaceRelation> relations = service.getSpaceAssignedUsers(spaceId);
        return ResponseEntity.ok(relations.stream()
                .map(this::toResponse)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "获取场所已分配位置数")
    @GetMapping("/space/{spaceId}/assigned-count")
    @PreAuthorize("hasAuthority('space:read')")
    public ResponseEntity<Integer> getAssignedPositionCount(@PathVariable Long spaceId) {
        return ResponseEntity.ok(service.getAssignedPositionCount(spaceId));
    }

    @Operation(summary = "获取关系详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('user:read')")
    public ResponseEntity<UserSpaceRelationResponse> getRelation(@PathVariable Long id) {
        UserSpaceRelation relation = service.getRelation(id);
        return ResponseEntity.ok(toResponse(relation));
    }

    @Operation(summary = "添加用户场所关系")
    @PostMapping
    @PreAuthorize("hasAuthority('space:write')")
    public ResponseEntity<UserSpaceRelationResponse> addRelation(@RequestBody AddRelationRequest request) {
        AddRelationCommand command = new AddRelationCommand();
        command.setUserId(request.getUserId());
        command.setSpaceId(request.getSpaceId());
        command.setRelationType(UserSpaceRelation.RelationType.valueOf(request.getRelationType()));
        command.setPositionCode(request.getPositionCode());
        command.setPositionName(request.getPositionName());
        command.setPrimary(request.isPrimary());
        command.setCanUse(request.isCanUse());
        command.setCanManage(request.isCanManage());
        command.setStartDate(request.getStartDate());
        command.setEndDate(request.getEndDate());
        command.setFeeAmount(request.getFeeAmount());
        command.setFeePaid(request.isFeePaid());
        command.setSortOrder(request.getSortOrder());
        command.setRemark(request.getRemark());
        // TODO: 从SecurityContext获取当前用户ID
        command.setOperatorId(1L);

        UserSpaceRelation relation = service.addRelation(command);
        return ResponseEntity.ok(toResponse(relation));
    }

    @Operation(summary = "更新用户场所关系")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('space:write')")
    public ResponseEntity<UserSpaceRelationResponse> updateRelation(
            @PathVariable Long id,
            @RequestBody UpdateRelationRequest request) {
        UpdateRelationCommand command = new UpdateRelationCommand();
        command.setPositionCode(request.getPositionCode());
        command.setPositionName(request.getPositionName());
        command.setCanUse(request.isCanUse());
        command.setCanManage(request.isCanManage());
        command.setStartDate(request.getStartDate());
        command.setEndDate(request.getEndDate());
        command.setFeeAmount(request.getFeeAmount());
        command.setFeePaid(request.isFeePaid());
        command.setSortOrder(request.getSortOrder());
        command.setRemark(request.getRemark());

        UserSpaceRelation relation = service.updateRelation(id, command);
        return ResponseEntity.ok(toResponse(relation));
    }

    @Operation(summary = "设置主要场所")
    @PutMapping("/user/{userId}/primary/{relationId}")
    @PreAuthorize("hasAuthority('space:write')")
    public ResponseEntity<UserSpaceRelationResponse> setPrimary(
            @PathVariable Long userId,
            @PathVariable Long relationId) {
        UserSpaceRelation relation = service.setPrimary(userId, relationId);
        return ResponseEntity.ok(toResponse(relation));
    }

    @Operation(summary = "标记已缴费")
    @PutMapping("/{id}/paid")
    @PreAuthorize("hasAuthority('space:write')")
    public ResponseEntity<UserSpaceRelationResponse> markAsPaid(@PathVariable Long id) {
        UserSpaceRelation relation = service.markAsPaid(id);
        return ResponseEntity.ok(toResponse(relation));
    }

    @Operation(summary = "删除用户场所关系")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('space:write')")
    public ResponseEntity<Void> deleteRelation(@PathVariable Long id) {
        service.deleteRelation(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== 转换方法 ====================

    private UserSpaceRelationResponse toResponse(UserSpaceRelation relation) {
        UserSpaceRelationResponse response = new UserSpaceRelationResponse();
        response.setId(relation.getId());
        response.setUserId(relation.getUserId());
        response.setSpaceId(relation.getSpaceId());
        response.setRelationType(relation.getRelationType().name());
        response.setRelationTypeLabel(relation.getRelationType().getLabel());
        response.setPositionCode(relation.getPositionCode());
        response.setPositionName(relation.getPositionName());
        response.setPrimary(relation.isPrimary());
        response.setCanUse(relation.isCanUse());
        response.setCanManage(relation.isCanManage());
        response.setStartDate(relation.getStartDate());
        response.setEndDate(relation.getEndDate());
        response.setFeeAmount(relation.getFeeAmount());
        response.setFeePaid(relation.isFeePaid());
        response.setSortOrder(relation.getSortOrder());
        response.setRemark(relation.getRemark());
        response.setActive(relation.isActive());
        response.setExpired(relation.isExpired());
        response.setExpiringSoon(relation.isExpiringSoon(7));
        response.setNeedsPayment(relation.needsPayment());
        return response;
    }

    // ==================== DTO类 ====================

    public static class AddRelationRequest {
        private Long userId;
        private Long spaceId;
        private String relationType;
        private String positionCode;
        private String positionName;
        private boolean isPrimary;
        private boolean canUse = true;
        private boolean canManage;
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal feeAmount;
        private boolean feePaid;
        private Integer sortOrder;
        private String remark;

        // Getters and Setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public Long getSpaceId() { return spaceId; }
        public void setSpaceId(Long spaceId) { this.spaceId = spaceId; }
        public String getRelationType() { return relationType; }
        public void setRelationType(String relationType) { this.relationType = relationType; }
        public String getPositionCode() { return positionCode; }
        public void setPositionCode(String positionCode) { this.positionCode = positionCode; }
        public String getPositionName() { return positionName; }
        public void setPositionName(String positionName) { this.positionName = positionName; }
        public boolean isPrimary() { return isPrimary; }
        public void setPrimary(boolean primary) { isPrimary = primary; }
        public boolean isCanUse() { return canUse; }
        public void setCanUse(boolean canUse) { this.canUse = canUse; }
        public boolean isCanManage() { return canManage; }
        public void setCanManage(boolean canManage) { this.canManage = canManage; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        public BigDecimal getFeeAmount() { return feeAmount; }
        public void setFeeAmount(BigDecimal feeAmount) { this.feeAmount = feeAmount; }
        public boolean isFeePaid() { return feePaid; }
        public void setFeePaid(boolean feePaid) { this.feePaid = feePaid; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }

    public static class UpdateRelationRequest {
        private String positionCode;
        private String positionName;
        private boolean canUse = true;
        private boolean canManage;
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal feeAmount;
        private boolean feePaid;
        private Integer sortOrder;
        private String remark;

        // Getters and Setters
        public String getPositionCode() { return positionCode; }
        public void setPositionCode(String positionCode) { this.positionCode = positionCode; }
        public String getPositionName() { return positionName; }
        public void setPositionName(String positionName) { this.positionName = positionName; }
        public boolean isCanUse() { return canUse; }
        public void setCanUse(boolean canUse) { this.canUse = canUse; }
        public boolean isCanManage() { return canManage; }
        public void setCanManage(boolean canManage) { this.canManage = canManage; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        public BigDecimal getFeeAmount() { return feeAmount; }
        public void setFeeAmount(BigDecimal feeAmount) { this.feeAmount = feeAmount; }
        public boolean isFeePaid() { return feePaid; }
        public void setFeePaid(boolean feePaid) { this.feePaid = feePaid; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }

    public static class UserSpaceRelationResponse {
        private Long id;
        private Long userId;
        private Long spaceId;
        private String relationType;
        private String relationTypeLabel;
        private String positionCode;
        private String positionName;
        private boolean isPrimary;
        private boolean canUse;
        private boolean canManage;
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal feeAmount;
        private boolean feePaid;
        private Integer sortOrder;
        private String remark;
        private boolean isActive;
        private boolean isExpired;
        private boolean isExpiringSoon;
        private boolean needsPayment;

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public Long getSpaceId() { return spaceId; }
        public void setSpaceId(Long spaceId) { this.spaceId = spaceId; }
        public String getRelationType() { return relationType; }
        public void setRelationType(String relationType) { this.relationType = relationType; }
        public String getRelationTypeLabel() { return relationTypeLabel; }
        public void setRelationTypeLabel(String relationTypeLabel) { this.relationTypeLabel = relationTypeLabel; }
        public String getPositionCode() { return positionCode; }
        public void setPositionCode(String positionCode) { this.positionCode = positionCode; }
        public String getPositionName() { return positionName; }
        public void setPositionName(String positionName) { this.positionName = positionName; }
        public boolean isPrimary() { return isPrimary; }
        public void setPrimary(boolean primary) { isPrimary = primary; }
        public boolean isCanUse() { return canUse; }
        public void setCanUse(boolean canUse) { this.canUse = canUse; }
        public boolean isCanManage() { return canManage; }
        public void setCanManage(boolean canManage) { this.canManage = canManage; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        public BigDecimal getFeeAmount() { return feeAmount; }
        public void setFeeAmount(BigDecimal feeAmount) { this.feeAmount = feeAmount; }
        public boolean isFeePaid() { return feePaid; }
        public void setFeePaid(boolean feePaid) { this.feePaid = feePaid; }
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
        public boolean isNeedsPayment() { return needsPayment; }
        public void setNeedsPayment(boolean needsPayment) { this.needsPayment = needsPayment; }
    }
}
