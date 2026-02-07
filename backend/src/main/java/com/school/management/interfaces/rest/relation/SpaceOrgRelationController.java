package com.school.management.interfaces.rest.relation;

import com.school.management.application.relation.SpaceOrgRelationApplicationService;
import com.school.management.application.relation.SpaceOrgRelationApplicationService.AddRelationCommand;
import com.school.management.application.relation.SpaceOrgRelationApplicationService.UpdateRelationCommand;
import com.school.management.domain.relation.model.entity.SpaceOrgRelation;
import com.school.management.common.util.SecurityUtils;
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
 * 场所-组织关系控制器
 */
@Tag(name = "场所组织关系", description = "场所多归属关系管理")
@RestController
@RequestMapping("/v6/space-org-relations")
public class SpaceOrgRelationController {

    private final SpaceOrgRelationApplicationService service;

    public SpaceOrgRelationController(SpaceOrgRelationApplicationService service) {
        this.service = service;
    }

    @Operation(summary = "获取场所的所有组织关系")
    @GetMapping("/space/{spaceId}")
    @PreAuthorize("hasAuthority('space:read')")
    public ResponseEntity<List<SpaceOrgRelationResponse>> getSpaceRelations(@PathVariable Long spaceId) {
        List<SpaceOrgRelation> relations = service.getSpaceRelations(spaceId);
        return ResponseEntity.ok(relations.stream()
                .map(this::toResponse)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "获取场所的有效组织关系")
    @GetMapping("/space/{spaceId}/active")
    @PreAuthorize("hasAuthority('space:read')")
    public ResponseEntity<List<SpaceOrgRelationResponse>> getSpaceActiveRelations(@PathVariable Long spaceId) {
        List<SpaceOrgRelation> relations = service.getSpaceActiveRelations(spaceId);
        return ResponseEntity.ok(relations.stream()
                .map(this::toResponse)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "获取场所的主归属")
    @GetMapping("/space/{spaceId}/primary")
    @PreAuthorize("hasAuthority('space:read')")
    public ResponseEntity<SpaceOrgRelationResponse> getSpacePrimaryRelation(@PathVariable Long spaceId) {
        SpaceOrgRelation relation = service.getSpacePrimaryRelation(spaceId);
        if (relation == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toResponse(relation));
    }

    @Operation(summary = "获取组织管理的场所关系")
    @GetMapping("/org/{orgUnitId}")
    @PreAuthorize("hasAuthority('org_unit:read')")
    public ResponseEntity<List<SpaceOrgRelationResponse>> getOrgSpaces(@PathVariable Long orgUnitId) {
        List<SpaceOrgRelation> relations = service.getOrgSpaces(orgUnitId);
        return ResponseEntity.ok(relations.stream()
                .map(this::toResponse)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "获取组织的主管场所")
    @GetMapping("/org/{orgUnitId}/primary")
    @PreAuthorize("hasAuthority('org_unit:read')")
    public ResponseEntity<List<SpaceOrgRelationResponse>> getOrgPrimarySpaces(@PathVariable Long orgUnitId) {
        List<SpaceOrgRelation> relations = service.getOrgPrimarySpaces(orgUnitId);
        return ResponseEntity.ok(relations.stream()
                .map(this::toResponse)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "获取组织可检查的场所")
    @GetMapping("/org/{orgUnitId}/inspectable")
    @PreAuthorize("hasAuthority('inspection:read')")
    public ResponseEntity<List<SpaceOrgRelationResponse>> getInspectableSpaces(@PathVariable Long orgUnitId) {
        List<SpaceOrgRelation> relations = service.getInspectableSpaces(orgUnitId);
        return ResponseEntity.ok(relations.stream()
                .map(this::toResponse)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "获取共用场所关系")
    @GetMapping("/space/{spaceId}/shared")
    @PreAuthorize("hasAuthority('space:read')")
    public ResponseEntity<List<SpaceOrgRelationResponse>> getSharedRelations(@PathVariable Long spaceId) {
        List<SpaceOrgRelation> relations = service.getSharedRelations(spaceId);
        return ResponseEntity.ok(relations.stream()
                .map(this::toResponse)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "获取关系详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('space:read')")
    public ResponseEntity<SpaceOrgRelationResponse> getRelation(@PathVariable Long id) {
        SpaceOrgRelation relation = service.getRelation(id);
        return ResponseEntity.ok(toResponse(relation));
    }

    @Operation(summary = "添加场所组织关系")
    @PostMapping
    @PreAuthorize("hasAuthority('space:write')")
    public ResponseEntity<SpaceOrgRelationResponse> addRelation(@RequestBody AddRelationRequest request) {
        AddRelationCommand command = new AddRelationCommand();
        command.setSpaceId(request.getSpaceId());
        command.setOrgUnitId(request.getOrgUnitId());
        command.setRelationType(SpaceOrgRelation.RelationType.valueOf(request.getRelationType()));
        command.setPrimary(request.isPrimary());
        command.setPriorityLevel(request.getPriorityLevel());
        command.setCanUse(request.isCanUse());
        command.setCanManage(request.isCanManage());
        command.setCanAssign(request.isCanAssign());
        command.setCanInspect(request.isCanInspect());
        command.setUseSchedule(request.getUseSchedule());
        command.setStartDate(request.getStartDate());
        command.setEndDate(request.getEndDate());
        command.setAllocatedCapacity(request.getAllocatedCapacity());
        command.setWeightRatio(request.getWeightRatio());
        command.setSortOrder(request.getSortOrder());
        command.setRemark(request.getRemark());
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new RuntimeException("无法获取当前用户ID");
        }
        command.setOperatorId(currentUserId);

        SpaceOrgRelation relation = service.addRelation(command);
        return ResponseEntity.ok(toResponse(relation));
    }

    @Operation(summary = "更新场所组织关系")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('space:write')")
    public ResponseEntity<SpaceOrgRelationResponse> updateRelation(
            @PathVariable Long id,
            @RequestBody UpdateRelationRequest request) {
        UpdateRelationCommand command = new UpdateRelationCommand();
        command.setPriorityLevel(request.getPriorityLevel());
        command.setCanUse(request.isCanUse());
        command.setCanManage(request.isCanManage());
        command.setCanAssign(request.isCanAssign());
        command.setCanInspect(request.isCanInspect());
        command.setUseSchedule(request.getUseSchedule());
        command.setStartDate(request.getStartDate());
        command.setEndDate(request.getEndDate());
        command.setAllocatedCapacity(request.getAllocatedCapacity());
        command.setWeightRatio(request.getWeightRatio());
        command.setSortOrder(request.getSortOrder());
        command.setRemark(request.getRemark());

        SpaceOrgRelation relation = service.updateRelation(id, command);
        return ResponseEntity.ok(toResponse(relation));
    }

    @Operation(summary = "设置主归属")
    @PutMapping("/space/{spaceId}/primary/{relationId}")
    @PreAuthorize("hasAuthority('space:write')")
    public ResponseEntity<SpaceOrgRelationResponse> setPrimary(
            @PathVariable Long spaceId,
            @PathVariable Long relationId) {
        SpaceOrgRelation relation = service.setPrimary(spaceId, relationId);
        return ResponseEntity.ok(toResponse(relation));
    }

    @Operation(summary = "删除场所组织关系")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('space:write')")
    public ResponseEntity<Void> deleteRelation(@PathVariable Long id) {
        service.deleteRelation(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== 转换方法 ====================

    private SpaceOrgRelationResponse toResponse(SpaceOrgRelation relation) {
        SpaceOrgRelationResponse response = new SpaceOrgRelationResponse();
        response.setId(relation.getId());
        response.setSpaceId(relation.getSpaceId());
        response.setOrgUnitId(relation.getOrgUnitId());
        response.setRelationType(relation.getRelationType().name());
        response.setRelationTypeLabel(relation.getRelationType().getLabel());
        response.setPrimary(relation.isPrimary());
        response.setPriorityLevel(relation.getPriorityLevel());
        response.setCanUse(relation.isCanUse());
        response.setCanManage(relation.isCanManage());
        response.setCanAssign(relation.isCanAssign());
        response.setCanInspect(relation.isCanInspect());
        response.setUseSchedule(relation.getUseSchedule());
        response.setStartDate(relation.getStartDate());
        response.setEndDate(relation.getEndDate());
        response.setAllocatedCapacity(relation.getAllocatedCapacity());
        response.setWeightRatio(relation.getWeightRatio());
        response.setSortOrder(relation.getSortOrder());
        response.setRemark(relation.getRemark());
        response.setActive(relation.isActive());
        response.setExpired(relation.isExpired());
        response.setExpiringSoon(relation.isExpiringSoon(7));
        response.setHasFullManagementRights(relation.hasFullManagementRights());
        return response;
    }

    // ==================== DTO类 ====================

    public static class AddRelationRequest {
        private Long spaceId;
        private Long orgUnitId;
        private String relationType;
        private boolean isPrimary;
        private Integer priorityLevel;
        private boolean canUse = true;
        private boolean canManage;
        private boolean canAssign;
        private boolean canInspect;
        private String useSchedule;
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer allocatedCapacity;
        private BigDecimal weightRatio;
        private Integer sortOrder;
        private String remark;

        // Getters and Setters
        public Long getSpaceId() { return spaceId; }
        public void setSpaceId(Long spaceId) { this.spaceId = spaceId; }
        public Long getOrgUnitId() { return orgUnitId; }
        public void setOrgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; }
        public String getRelationType() { return relationType; }
        public void setRelationType(String relationType) { this.relationType = relationType; }
        public boolean isPrimary() { return isPrimary; }
        public void setPrimary(boolean primary) { isPrimary = primary; }
        public Integer getPriorityLevel() { return priorityLevel; }
        public void setPriorityLevel(Integer priorityLevel) { this.priorityLevel = priorityLevel; }
        public boolean isCanUse() { return canUse; }
        public void setCanUse(boolean canUse) { this.canUse = canUse; }
        public boolean isCanManage() { return canManage; }
        public void setCanManage(boolean canManage) { this.canManage = canManage; }
        public boolean isCanAssign() { return canAssign; }
        public void setCanAssign(boolean canAssign) { this.canAssign = canAssign; }
        public boolean isCanInspect() { return canInspect; }
        public void setCanInspect(boolean canInspect) { this.canInspect = canInspect; }
        public String getUseSchedule() { return useSchedule; }
        public void setUseSchedule(String useSchedule) { this.useSchedule = useSchedule; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        public Integer getAllocatedCapacity() { return allocatedCapacity; }
        public void setAllocatedCapacity(Integer allocatedCapacity) { this.allocatedCapacity = allocatedCapacity; }
        public BigDecimal getWeightRatio() { return weightRatio; }
        public void setWeightRatio(BigDecimal weightRatio) { this.weightRatio = weightRatio; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }

    public static class UpdateRelationRequest {
        private Integer priorityLevel;
        private boolean canUse = true;
        private boolean canManage;
        private boolean canAssign;
        private boolean canInspect;
        private String useSchedule;
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer allocatedCapacity;
        private BigDecimal weightRatio;
        private Integer sortOrder;
        private String remark;

        // Getters and Setters
        public Integer getPriorityLevel() { return priorityLevel; }
        public void setPriorityLevel(Integer priorityLevel) { this.priorityLevel = priorityLevel; }
        public boolean isCanUse() { return canUse; }
        public void setCanUse(boolean canUse) { this.canUse = canUse; }
        public boolean isCanManage() { return canManage; }
        public void setCanManage(boolean canManage) { this.canManage = canManage; }
        public boolean isCanAssign() { return canAssign; }
        public void setCanAssign(boolean canAssign) { this.canAssign = canAssign; }
        public boolean isCanInspect() { return canInspect; }
        public void setCanInspect(boolean canInspect) { this.canInspect = canInspect; }
        public String getUseSchedule() { return useSchedule; }
        public void setUseSchedule(String useSchedule) { this.useSchedule = useSchedule; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        public Integer getAllocatedCapacity() { return allocatedCapacity; }
        public void setAllocatedCapacity(Integer allocatedCapacity) { this.allocatedCapacity = allocatedCapacity; }
        public BigDecimal getWeightRatio() { return weightRatio; }
        public void setWeightRatio(BigDecimal weightRatio) { this.weightRatio = weightRatio; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }

    public static class SpaceOrgRelationResponse {
        private Long id;
        private Long spaceId;
        private Long orgUnitId;
        private String relationType;
        private String relationTypeLabel;
        private boolean isPrimary;
        private Integer priorityLevel;
        private boolean canUse;
        private boolean canManage;
        private boolean canAssign;
        private boolean canInspect;
        private String useSchedule;
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer allocatedCapacity;
        private BigDecimal weightRatio;
        private Integer sortOrder;
        private String remark;
        private boolean isActive;
        private boolean isExpired;
        private boolean isExpiringSoon;
        private boolean hasFullManagementRights;

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getSpaceId() { return spaceId; }
        public void setSpaceId(Long spaceId) { this.spaceId = spaceId; }
        public Long getOrgUnitId() { return orgUnitId; }
        public void setOrgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; }
        public String getRelationType() { return relationType; }
        public void setRelationType(String relationType) { this.relationType = relationType; }
        public String getRelationTypeLabel() { return relationTypeLabel; }
        public void setRelationTypeLabel(String relationTypeLabel) { this.relationTypeLabel = relationTypeLabel; }
        public boolean isPrimary() { return isPrimary; }
        public void setPrimary(boolean primary) { isPrimary = primary; }
        public Integer getPriorityLevel() { return priorityLevel; }
        public void setPriorityLevel(Integer priorityLevel) { this.priorityLevel = priorityLevel; }
        public boolean isCanUse() { return canUse; }
        public void setCanUse(boolean canUse) { this.canUse = canUse; }
        public boolean isCanManage() { return canManage; }
        public void setCanManage(boolean canManage) { this.canManage = canManage; }
        public boolean isCanAssign() { return canAssign; }
        public void setCanAssign(boolean canAssign) { this.canAssign = canAssign; }
        public boolean isCanInspect() { return canInspect; }
        public void setCanInspect(boolean canInspect) { this.canInspect = canInspect; }
        public String getUseSchedule() { return useSchedule; }
        public void setUseSchedule(String useSchedule) { this.useSchedule = useSchedule; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        public Integer getAllocatedCapacity() { return allocatedCapacity; }
        public void setAllocatedCapacity(Integer allocatedCapacity) { this.allocatedCapacity = allocatedCapacity; }
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
        public boolean isHasFullManagementRights() { return hasFullManagementRights; }
        public void setHasFullManagementRights(boolean hasFullManagementRights) { this.hasFullManagementRights = hasFullManagementRights; }
    }
}
