package com.school.management.infrastructure.persistence.relation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.relation.model.entity.UserSpaceRelation;
import com.school.management.domain.relation.repository.UserSpaceRelationRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 用户-场所关系仓储实现
 */
@Repository
public class UserSpaceRelationRepositoryImpl implements UserSpaceRelationRepository {

    private final UserSpaceRelationMapper mapper;

    public UserSpaceRelationRepositoryImpl(UserSpaceRelationMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public UserSpaceRelation save(UserSpaceRelation relation) {
        UserSpaceRelationPO po = toPO(relation);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return toDomain(po);
    }

    @Override
    public Optional<UserSpaceRelation> findById(Long id) {
        UserSpaceRelationPO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<UserSpaceRelation> findByUserId(Long userId) {
        return mapper.findByUserId(userId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserSpaceRelation> findActiveByUserId(Long userId) {
        return mapper.findActiveByUserId(userId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<UserSpaceRelation> findPrimaryByUserId(Long userId) {
        UserSpaceRelationPO po = mapper.findPrimaryByUserId(userId);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<UserSpaceRelation> findBySpaceId(Long spaceId) {
        return mapper.findBySpaceId(spaceId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserSpaceRelation> findAssignedBySpaceId(Long spaceId) {
        return mapper.findAssignedBySpaceId(spaceId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserSpaceRelation> findByUserAndSpace(Long userId, Long spaceId) {
        return mapper.findByUserAndSpace(userId, spaceId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<UserSpaceRelation> findBySpaceAndPosition(Long spaceId, String positionCode) {
        UserSpaceRelationPO po = mapper.findBySpaceAndPosition(spaceId, positionCode);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<UserSpaceRelation> findUnpaidByUserId(Long userId) {
        return mapper.findUnpaidByUserId(userId).stream()
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
    public boolean existsRelation(Long userId, Long spaceId, UserSpaceRelation.RelationType relationType) {
        return mapper.existsRelation(userId, spaceId, relationType.name());
    }

    @Override
    public boolean existsPosition(Long spaceId, String positionCode) {
        return mapper.existsPosition(spaceId, positionCode);
    }

    @Override
    public int countBySpaceId(Long spaceId) {
        return mapper.countBySpaceId(spaceId);
    }

    @Override
    public int countByUserId(Long userId) {
        return mapper.countByUserId(userId);
    }

    @Override
    public int countAssignedPositions(Long spaceId) {
        return mapper.countAssignedPositions(spaceId);
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public void deleteByUserId(Long userId) {
        LambdaQueryWrapper<UserSpaceRelationPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserSpaceRelationPO::getUserId, userId);
        mapper.delete(wrapper);
    }

    // ==================== 转换方法 ====================

    private UserSpaceRelationPO toPO(UserSpaceRelation entity) {
        UserSpaceRelationPO po = new UserSpaceRelationPO();
        po.setId(entity.getId());
        po.setUserId(entity.getUserId());
        po.setSpaceId(entity.getSpaceId());
        po.setRelationType(entity.getRelationType().name());
        po.setPositionCode(entity.getPositionCode());
        po.setPositionName(entity.getPositionName());
        po.setIsPrimary(entity.isPrimary());
        po.setCanUse(entity.isCanUse());
        po.setCanManage(entity.isCanManage());
        po.setStartDate(entity.getStartDate());
        po.setEndDate(entity.getEndDate());
        po.setFeeAmount(entity.getFeeAmount());
        po.setFeePaid(entity.isFeePaid());
        po.setSortOrder(entity.getSortOrder());
        po.setRemark(entity.getRemark());
        po.setCreatedBy(entity.getCreatedBy());
        return po;
    }

    private UserSpaceRelation toDomain(UserSpaceRelationPO po) {
        return UserSpaceRelation.builder()
                .id(po.getId())
                .userId(po.getUserId())
                .spaceId(po.getSpaceId())
                .relationType(UserSpaceRelation.RelationType.valueOf(po.getRelationType()))
                .positionCode(po.getPositionCode())
                .positionName(po.getPositionName())
                .isPrimary(Boolean.TRUE.equals(po.getIsPrimary()))
                .canUse(Boolean.TRUE.equals(po.getCanUse()))
                .canManage(Boolean.TRUE.equals(po.getCanManage()))
                .startDate(po.getStartDate())
                .endDate(po.getEndDate())
                .feeAmount(po.getFeeAmount())
                .feePaid(Boolean.TRUE.equals(po.getFeePaid()))
                .sortOrder(po.getSortOrder())
                .remark(po.getRemark())
                .createdBy(po.getCreatedBy())
                .build();
    }
}
