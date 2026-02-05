package com.school.management.application.relation;

import com.school.management.domain.relation.model.entity.UserOrgRelation;
import com.school.management.domain.relation.repository.UserOrgRelationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 用户-组织关系应用服务
 */
@Service
public class UserOrgRelationApplicationService {

    private final UserOrgRelationRepository repository;

    public UserOrgRelationApplicationService(UserOrgRelationRepository repository) {
        this.repository = repository;
    }

    /**
     * 添加用户组织关系
     */
    @Transactional
    public UserOrgRelation addRelation(AddRelationCommand command) {
        // 检查关系是否已存在
        if (repository.existsRelation(command.getUserId(), command.getOrgUnitId(), command.getRelationType())) {
            throw new IllegalArgumentException("该用户在此组织已存在相同类型的关系");
        }

        // 如果是主归属，先清除其他主归属
        if (command.isPrimary()) {
            repository.clearPrimaryByUserId(command.getUserId());
        }

        UserOrgRelation relation = UserOrgRelation.builder()
                .userId(command.getUserId())
                .orgUnitId(command.getOrgUnitId())
                .relationType(command.getRelationType())
                .positionTitle(command.getPositionTitle())
                .positionLevel(command.getPositionLevel())
                .isPrimary(command.isPrimary())
                .isLeader(command.isLeader())
                .canManage(command.isCanManage())
                .canApprove(command.isCanApprove())
                .startDate(command.getStartDate())
                .endDate(command.getEndDate())
                .weightRatio(command.getWeightRatio() != null ? command.getWeightRatio() : new BigDecimal("100.00"))
                .sortOrder(command.getSortOrder() != null ? command.getSortOrder() : 0)
                .remark(command.getRemark())
                .createdBy(command.getOperatorId())
                .build();

        return repository.save(relation);
    }

    /**
     * 更新用户组织关系
     */
    @Transactional
    public UserOrgRelation updateRelation(Long id, UpdateRelationCommand command) {
        UserOrgRelation relation = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("关系不存在: " + id));

        // 更新职务信息
        relation.updatePosition(command.getPositionTitle(), command.getPositionLevel(), command.isLeader());

        // 更新权限
        relation.updatePermissions(command.isCanManage(), command.isCanApprove());

        // 更新有效期
        relation.updatePeriod(command.getStartDate(), command.getEndDate());

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
    public UserOrgRelation setPrimary(Long userId, Long relationId) {
        UserOrgRelation relation = repository.findById(relationId)
                .orElseThrow(() -> new IllegalArgumentException("关系不存在: " + relationId));

        if (!relation.getUserId().equals(userId)) {
            throw new IllegalArgumentException("关系不属于该用户");
        }

        // 清除其他主归属
        repository.clearPrimaryByUserId(userId);

        // 设置新的主归属
        relation.setAsPrimary();

        return repository.save(relation);
    }

    /**
     * 删除用户组织关系
     */
    @Transactional
    public void deleteRelation(Long id) {
        UserOrgRelation relation = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("关系不存在: " + id));

        // 如果是主归属，不允许直接删除
        if (relation.isPrimary() && repository.countByUserId(relation.getUserId()) > 1) {
            throw new IllegalArgumentException("请先设置其他组织为主归属后再删除");
        }

        repository.deleteById(id);
    }

    /**
     * 获取用户的所有关系
     */
    public List<UserOrgRelation> getUserRelations(Long userId) {
        return repository.findByUserId(userId);
    }

    /**
     * 获取用户的有效关系
     */
    public List<UserOrgRelation> getUserActiveRelations(Long userId) {
        return repository.findActiveByUserId(userId);
    }

    /**
     * 获取用户的主归属
     */
    public UserOrgRelation getUserPrimaryRelation(Long userId) {
        return repository.findPrimaryByUserId(userId)
                .orElse(null);
    }

    /**
     * 获取组织的成员关系
     */
    public List<UserOrgRelation> getOrgMembers(Long orgUnitId) {
        return repository.findByOrgUnitId(orgUnitId);
    }

    /**
     * 获取组织的领导
     */
    public List<UserOrgRelation> getOrgLeaders(Long orgUnitId) {
        return repository.findLeadersByOrgUnitId(orgUnitId);
    }

    /**
     * 获取关系详情
     */
    public UserOrgRelation getRelation(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("关系不存在: " + id));
    }

    // ==================== 命令对象 ====================

    public static class AddRelationCommand {
        private Long userId;
        private Long orgUnitId;
        private UserOrgRelation.RelationType relationType;
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
        private Long operatorId;

        // Getters and Setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public Long getOrgUnitId() { return orgUnitId; }
        public void setOrgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; }
        public UserOrgRelation.RelationType getRelationType() { return relationType; }
        public void setRelationType(UserOrgRelation.RelationType relationType) { this.relationType = relationType; }
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
        public Long getOperatorId() { return operatorId; }
        public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    }

    public static class UpdateRelationCommand {
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
}
