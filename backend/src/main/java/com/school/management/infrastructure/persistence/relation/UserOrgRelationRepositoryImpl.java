package com.school.management.infrastructure.persistence.relation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.relation.model.entity.UserOrgRelation;
import com.school.management.domain.relation.repository.UserOrgRelationRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 用户-组织关系仓储实现
 */
@Repository
public class UserOrgRelationRepositoryImpl implements UserOrgRelationRepository {

    private final UserOrgRelationMapper mapper;

    public UserOrgRelationRepositoryImpl(UserOrgRelationMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public UserOrgRelation save(UserOrgRelation relation) {
        UserOrgRelationPO po = toPO(relation);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return toDomain(po);
    }

    @Override
    public Optional<UserOrgRelation> findById(Long id) {
        UserOrgRelationPO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<UserOrgRelation> findByUserId(Long userId) {
        return mapper.findByUserId(userId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserOrgRelation> findActiveByUserId(Long userId) {
        return mapper.findActiveByUserId(userId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<UserOrgRelation> findPrimaryByUserId(Long userId) {
        UserOrgRelationPO po = mapper.findPrimaryByUserId(userId);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<UserOrgRelation> findByOrgUnitId(Long orgUnitId) {
        return mapper.findByOrgUnitId(orgUnitId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserOrgRelation> findLeadersByOrgUnitId(Long orgUnitId) {
        return mapper.findLeadersByOrgUnitId(orgUnitId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserOrgRelation> findByUserAndOrg(Long userId, Long orgUnitId) {
        return mapper.findByUserAndOrg(userId, orgUnitId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void clearPrimaryByUserId(Long userId) {
        mapper.clearPrimaryByUserId(userId);
    }

    @Override
    public boolean existsPrimaryByUserId(Long userId) {
        return mapper.existsPrimaryByUserId(userId);
    }

    @Override
    public boolean existsRelation(Long userId, Long orgUnitId, UserOrgRelation.RelationType relationType) {
        return mapper.existsRelation(userId, orgUnitId, relationType.name());
    }

    @Override
    public int countByOrgUnitId(Long orgUnitId) {
        return mapper.countByOrgUnitId(orgUnitId);
    }

    @Override
    public int countByUserId(Long userId) {
        return mapper.countByUserId(userId);
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public void deleteByUserId(Long userId) {
        LambdaQueryWrapper<UserOrgRelationPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserOrgRelationPO::getUserId, userId);
        mapper.delete(wrapper);
    }

    // ==================== 转换方法 ====================

    private UserOrgRelationPO toPO(UserOrgRelation entity) {
        UserOrgRelationPO po = new UserOrgRelationPO();
        po.setId(entity.getId());
        po.setUserId(entity.getUserId());
        po.setOrgUnitId(entity.getOrgUnitId());
        po.setRelationType(entity.getRelationType().name());
        po.setPositionTitle(entity.getPositionTitle());
        po.setPositionLevel(entity.getPositionLevel());
        po.setIsPrimary(entity.isPrimary());
        po.setIsLeader(entity.isLeader());
        po.setCanManage(entity.isCanManage());
        po.setCanApprove(entity.isCanApprove());
        po.setStartDate(entity.getStartDate());
        po.setEndDate(entity.getEndDate());
        po.setWeightRatio(entity.getWeightRatio());
        po.setSortOrder(entity.getSortOrder());
        po.setRemark(entity.getRemark());
        po.setCreatedBy(entity.getCreatedBy());
        return po;
    }

    private UserOrgRelation toDomain(UserOrgRelationPO po) {
        return UserOrgRelation.builder()
                .id(po.getId())
                .userId(po.getUserId())
                .orgUnitId(po.getOrgUnitId())
                .relationType(UserOrgRelation.RelationType.valueOf(po.getRelationType()))
                .positionTitle(po.getPositionTitle())
                .positionLevel(po.getPositionLevel())
                .isPrimary(Boolean.TRUE.equals(po.getIsPrimary()))
                .isLeader(Boolean.TRUE.equals(po.getIsLeader()))
                .canManage(Boolean.TRUE.equals(po.getCanManage()))
                .canApprove(Boolean.TRUE.equals(po.getCanApprove()))
                .startDate(po.getStartDate())
                .endDate(po.getEndDate())
                .weightRatio(po.getWeightRatio() != null ? po.getWeightRatio() : new BigDecimal("100.00"))
                .sortOrder(po.getSortOrder())
                .remark(po.getRemark())
                .createdBy(po.getCreatedBy())
                .build();
    }
}
