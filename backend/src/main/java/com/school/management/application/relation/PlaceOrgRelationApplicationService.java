package com.school.management.application.relation;

import com.school.management.domain.relation.model.entity.SpaceOrgRelation;
import com.school.management.domain.relation.repository.SpaceOrgRelationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 场所-组织关系应用服务
 */
@Service
public class SpaceOrgRelationApplicationService {

    private final SpaceOrgRelationRepository repository;

    public SpaceOrgRelationApplicationService(SpaceOrgRelationRepository repository) {
        this.repository = repository;
    }

    /**
     * 添加场所组织关系
     */
    @Transactional
    public SpaceOrgRelation addRelation(AddRelationCommand command) {
        // 检查关系是否已存在
        if (repository.existsRelation(command.getSpaceId(), command.getOrgUnitId(), command.getRelationType())) {
            throw new IllegalArgumentException("该场所在此组织已存在相同类型的关系");
        }

        // 如果是主归属，先清除其他主归属
        if (command.isPrimary()) {
            repository.clearPrimaryBySpaceId(command.getSpaceId());
        }

        SpaceOrgRelation relation = SpaceOrgRelation.builder()
                .spaceId(command.getSpaceId())
                .orgUnitId(command.getOrgUnitId())
                .relationType(command.getRelationType())
                .isPrimary(command.isPrimary())
                .priorityLevel(command.getPriorityLevel() != null ? command.getPriorityLevel() : 1)
                .canUse(command.isCanUse())
                .canManage(command.isCanManage())
                .canAssign(command.isCanAssign())
                .canInspect(command.isCanInspect())
                .useSchedule(command.getUseSchedule())
                .startDate(command.getStartDate())
                .endDate(command.getEndDate())
                .allocatedCapacity(command.getAllocatedCapacity())
                .weightRatio(command.getWeightRatio() != null ? command.getWeightRatio() : new BigDecimal("100.00"))
                .sortOrder(command.getSortOrder() != null ? command.getSortOrder() : 0)
                .remark(command.getRemark())
                .createdBy(command.getOperatorId())
                .build();

        return repository.save(relation);
    }

    /**
     * 更新场所组织关系
     */
    @Transactional
    public SpaceOrgRelation updateRelation(Long id, UpdateRelationCommand command) {
        SpaceOrgRelation relation = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("关系不存在: " + id));

        // 更新权限
        relation.updatePermissions(command.isCanUse(), command.isCanManage(),
                command.isCanAssign(), command.isCanInspect());

        // 更新优先级
        if (command.getPriorityLevel() != null) {
            relation.updatePriority(command.getPriorityLevel());
        }

        // 更新时间安排
        relation.updateSchedule(command.getUseSchedule());

        // 更新有效期
        relation.updatePeriod(command.getStartDate(), command.getEndDate());

        // 更新容量
        if (command.getAllocatedCapacity() != null) {
            relation.updateCapacity(command.getAllocatedCapacity());
        }

        // 更新权重
        if (command.getWeightRatio() != null) {
            relation.updateWeight(command.getWeightRatio());
        }

        // 更新排序
        if (command.getSortOrder() != null) {
            relation.updateSortOrder(command.getSortOrder());
        }

        return repository.save(relation);
    }

    /**
     * 设置主归属
     */
    @Transactional
    public SpaceOrgRelation setPrimary(Long spaceId, Long relationId) {
        SpaceOrgRelation relation = repository.findById(relationId)
                .orElseThrow(() -> new IllegalArgumentException("关系不存在: " + relationId));

        if (!relation.getSpaceId().equals(spaceId)) {
            throw new IllegalArgumentException("关系不属于该场所");
        }

        // 清除其他主归属
        repository.clearPrimaryBySpaceId(spaceId);

        // 设置新的主归属
        relation.setAsPrimary();

        return repository.save(relation);
    }

    /**
     * 删除场所组织关系
     */
    @Transactional
    public void deleteRelation(Long id) {
        SpaceOrgRelation relation = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("关系不存在: " + id));

        // 如果是主归属，不允许直接删除
        if (relation.isPrimary() && repository.countBySpaceId(relation.getSpaceId()) > 1) {
            throw new IllegalArgumentException("请先设置其他组织为主归属后再删除");
        }

        repository.deleteById(id);
    }

    /**
     * 获取场所的所有关系
     */
    public List<SpaceOrgRelation> getSpaceRelations(Long spaceId) {
        return repository.findBySpaceId(spaceId);
    }

    /**
     * 获取场所的有效关系
     */
    public List<SpaceOrgRelation> getSpaceActiveRelations(Long spaceId) {
        return repository.findActiveBySpaceId(spaceId);
    }

    /**
     * 获取场所的主归属
     */
    public SpaceOrgRelation getSpacePrimaryRelation(Long spaceId) {
        return repository.findPrimaryBySpaceId(spaceId)
                .orElse(null);
    }

    /**
     * 获取组织管理的场所关系
     */
    public List<SpaceOrgRelation> getOrgSpaces(Long orgUnitId) {
        return repository.findByOrgUnitId(orgUnitId);
    }

    /**
     * 获取组织的主管场所
     */
    public List<SpaceOrgRelation> getOrgPrimarySpaces(Long orgUnitId) {
        return repository.findPrimaryByOrgUnitId(orgUnitId);
    }

    /**
     * 获取可检查的场所关系
     */
    public List<SpaceOrgRelation> getInspectableSpaces(Long orgUnitId) {
        return repository.findInspectableByOrgUnitId(orgUnitId);
    }

    /**
     * 获取共用场所关系
     */
    public List<SpaceOrgRelation> getSharedRelations(Long spaceId) {
        return repository.findSharedBySpaceId(spaceId);
    }

    /**
     * 获取关系详情
     */
    public SpaceOrgRelation getRelation(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("关系不存在: " + id));
    }

    // ==================== 命令对象 ====================

    public static class AddRelationCommand {
        private Long spaceId;
        private Long orgUnitId;
        private SpaceOrgRelation.RelationType relationType;
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
        private Long operatorId;

        // Getters and Setters
        public Long getSpaceId() { return spaceId; }
        public void setSpaceId(Long spaceId) { this.spaceId = spaceId; }
        public Long getOrgUnitId() { return orgUnitId; }
        public void setOrgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; }
        public SpaceOrgRelation.RelationType getRelationType() { return relationType; }
        public void setRelationType(SpaceOrgRelation.RelationType relationType) { this.relationType = relationType; }
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
        public Long getOperatorId() { return operatorId; }
        public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    }

    public static class UpdateRelationCommand {
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
}
