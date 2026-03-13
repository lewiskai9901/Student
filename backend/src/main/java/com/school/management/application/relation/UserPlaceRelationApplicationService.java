package com.school.management.application.relation;

import com.school.management.domain.relation.model.entity.UserSpaceRelation;
import com.school.management.domain.relation.repository.UserSpaceRelationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 用户-场所关系应用服务
 */
@Service
public class UserSpaceRelationApplicationService {

    private final UserSpaceRelationRepository repository;

    public UserSpaceRelationApplicationService(UserSpaceRelationRepository repository) {
        this.repository = repository;
    }

    /**
     * 添加用户场所关系
     */
    @Transactional
    public UserSpaceRelation addRelation(AddRelationCommand command) {
        // 检查关系是否已存在
        if (repository.existsRelation(command.getUserId(), command.getSpaceId(), command.getRelationType())) {
            throw new IllegalArgumentException("该用户在此场所已存在相同类型的关系");
        }

        // 检查位置是否已被占用
        if (command.getPositionCode() != null && !command.getPositionCode().isEmpty()) {
            if (repository.existsPosition(command.getSpaceId(), command.getPositionCode())) {
                throw new IllegalArgumentException("该位置已被占用: " + command.getPositionCode());
            }
        }

        // 如果是主要场所，先清除其他主要场所
        if (command.isPrimary()) {
            repository.clearPrimaryByUserId(command.getUserId());
        }

        UserSpaceRelation relation = UserSpaceRelation.builder()
                .userId(command.getUserId())
                .spaceId(command.getSpaceId())
                .relationType(command.getRelationType())
                .positionCode(command.getPositionCode())
                .positionName(command.getPositionName())
                .isPrimary(command.isPrimary())
                .canUse(command.isCanUse())
                .canManage(command.isCanManage())
                .startDate(command.getStartDate())
                .endDate(command.getEndDate())
                .feeAmount(command.getFeeAmount())
                .feePaid(command.isFeePaid())
                .sortOrder(command.getSortOrder() != null ? command.getSortOrder() : 0)
                .remark(command.getRemark())
                .createdBy(command.getOperatorId())
                .build();

        return repository.save(relation);
    }

    /**
     * 更新用户场所关系
     */
    @Transactional
    public UserSpaceRelation updateRelation(Long id, UpdateRelationCommand command) {
        UserSpaceRelation relation = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("关系不存在: " + id));

        // 如果要更新位置，检查新位置是否被占用
        if (command.getPositionCode() != null && !command.getPositionCode().isEmpty()) {
            if (!command.getPositionCode().equals(relation.getPositionCode())) {
                if (repository.existsPosition(relation.getSpaceId(), command.getPositionCode())) {
                    throw new IllegalArgumentException("该位置已被占用: " + command.getPositionCode());
                }
            }
        }

        // 更新位置
        relation.updatePosition(command.getPositionCode(), command.getPositionName());

        // 更新权限
        relation.updatePermissions(command.isCanUse(), command.isCanManage());

        // 更新有效期
        relation.updatePeriod(command.getStartDate(), command.getEndDate());

        // 更新费用
        relation.updateFee(command.getFeeAmount(), command.isFeePaid());

        // 更新排序
        if (command.getSortOrder() != null) {
            relation.updateSortOrder(command.getSortOrder());
        }

        return repository.save(relation);
    }

    /**
     * 设置主要场所
     */
    @Transactional
    public UserSpaceRelation setPrimary(Long userId, Long relationId) {
        UserSpaceRelation relation = repository.findById(relationId)
                .orElseThrow(() -> new IllegalArgumentException("关系不存在: " + relationId));

        if (!relation.getUserId().equals(userId)) {
            throw new IllegalArgumentException("关系不属于该用户");
        }

        // 清除其他主要场所
        repository.clearPrimaryByUserId(userId);

        // 设置新的主要场所
        relation.setAsPrimary();

        return repository.save(relation);
    }

    /**
     * 标记已缴费
     */
    @Transactional
    public UserSpaceRelation markAsPaid(Long id) {
        UserSpaceRelation relation = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("关系不存在: " + id));

        relation.markAsPaid();

        return repository.save(relation);
    }

    /**
     * 删除用户场所关系
     */
    @Transactional
    public void deleteRelation(Long id) {
        UserSpaceRelation relation = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("关系不存在: " + id));

        // 如果是主要场所，不允许直接删除
        if (relation.isPrimary() && repository.countByUserId(relation.getUserId()) > 1) {
            throw new IllegalArgumentException("请先设置其他场所为主要场所后再删除");
        }

        repository.deleteById(id);
    }

    /**
     * 获取用户的所有场所关系
     */
    public List<UserSpaceRelation> getUserRelations(Long userId) {
        return repository.findByUserId(userId);
    }

    /**
     * 获取用户的有效场所关系
     */
    public List<UserSpaceRelation> getUserActiveRelations(Long userId) {
        return repository.findActiveByUserId(userId);
    }

    /**
     * 获取用户的主要场所
     */
    public UserSpaceRelation getUserPrimaryRelation(Long userId) {
        return repository.findPrimaryByUserId(userId)
                .orElse(null);
    }

    /**
     * 获取场所的用户关系
     */
    public List<UserSpaceRelation> getSpaceUsers(Long spaceId) {
        return repository.findBySpaceId(spaceId);
    }

    /**
     * 获取场所的分配用户
     */
    public List<UserSpaceRelation> getSpaceAssignedUsers(Long spaceId) {
        return repository.findAssignedBySpaceId(spaceId);
    }

    /**
     * 获取用户未缴费的场所
     */
    public List<UserSpaceRelation> getUnpaidRelations(Long userId) {
        return repository.findUnpaidByUserId(userId);
    }

    /**
     * 获取关系详情
     */
    public UserSpaceRelation getRelation(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("关系不存在: " + id));
    }

    /**
     * 获取场所已分配位置数
     */
    public int getAssignedPositionCount(Long spaceId) {
        return repository.countAssignedPositions(spaceId);
    }

    // ==================== 命令对象 ====================

    public static class AddRelationCommand {
        private Long userId;
        private Long spaceId;
        private UserSpaceRelation.RelationType relationType;
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
        private Long operatorId;

        // Getters and Setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public Long getSpaceId() { return spaceId; }
        public void setSpaceId(Long spaceId) { this.spaceId = spaceId; }
        public UserSpaceRelation.RelationType getRelationType() { return relationType; }
        public void setRelationType(UserSpaceRelation.RelationType relationType) { this.relationType = relationType; }
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
        public Long getOperatorId() { return operatorId; }
        public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    }

    public static class UpdateRelationCommand {
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
}
