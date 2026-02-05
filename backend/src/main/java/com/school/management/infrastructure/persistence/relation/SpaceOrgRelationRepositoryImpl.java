package com.school.management.infrastructure.persistence.relation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.relation.model.entity.SpaceOrgRelation;
import com.school.management.domain.relation.repository.SpaceOrgRelationRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 场所-组织关系仓储实现
 */
@Repository
public class SpaceOrgRelationRepositoryImpl implements SpaceOrgRelationRepository {

    private final SpaceOrgRelationMapper mapper;

    public SpaceOrgRelationRepositoryImpl(SpaceOrgRelationMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public SpaceOrgRelation save(SpaceOrgRelation relation) {
        SpaceOrgRelationPO po = toPO(relation);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return toDomain(po);
    }

    @Override
    public Optional<SpaceOrgRelation> findById(Long id) {
        SpaceOrgRelationPO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<SpaceOrgRelation> findBySpaceId(Long spaceId) {
        return mapper.findBySpaceId(spaceId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<SpaceOrgRelation> findActiveBySpaceId(Long spaceId) {
        return mapper.findActiveBySpaceId(spaceId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<SpaceOrgRelation> findPrimaryBySpaceId(Long spaceId) {
        SpaceOrgRelationPO po = mapper.findPrimaryBySpaceId(spaceId);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<SpaceOrgRelation> findByOrgUnitId(Long orgUnitId) {
        return mapper.findByOrgUnitId(orgUnitId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<SpaceOrgRelation> findPrimaryByOrgUnitId(Long orgUnitId) {
        return mapper.findPrimaryByOrgUnitId(orgUnitId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<SpaceOrgRelation> findBySpaceAndOrg(Long spaceId, Long orgUnitId) {
        return mapper.findBySpaceAndOrg(spaceId, orgUnitId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<SpaceOrgRelation> findInspectableBySpaceId(Long spaceId) {
        return mapper.findInspectableBySpaceId(spaceId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<SpaceOrgRelation> findInspectableByOrgUnitId(Long orgUnitId) {
        return mapper.findInspectableByOrgUnitId(orgUnitId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<SpaceOrgRelation> findSharedBySpaceId(Long spaceId) {
        return mapper.findSharedBySpaceId(spaceId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void clearPrimaryBySpaceId(Long spaceId) {
        mapper.clearPrimaryBySpaceId(spaceId);
    }

    @Override
    public boolean existsPrimaryBySpaceId(Long spaceId) {
        return mapper.existsPrimaryBySpaceId(spaceId);
    }

    @Override
    public boolean existsRelation(Long spaceId, Long orgUnitId, SpaceOrgRelation.RelationType relationType) {
        return mapper.existsRelation(spaceId, orgUnitId, relationType.name());
    }

    @Override
    public int countByOrgUnitId(Long orgUnitId) {
        return mapper.countByOrgUnitId(orgUnitId);
    }

    @Override
    public int countBySpaceId(Long spaceId) {
        return mapper.countBySpaceId(spaceId);
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public void deleteBySpaceId(Long spaceId) {
        LambdaQueryWrapper<SpaceOrgRelationPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SpaceOrgRelationPO::getSpaceId, spaceId);
        mapper.delete(wrapper);
    }

    // ==================== 转换方法 ====================

    private SpaceOrgRelationPO toPO(SpaceOrgRelation entity) {
        SpaceOrgRelationPO po = new SpaceOrgRelationPO();
        po.setId(entity.getId());
        po.setSpaceId(entity.getSpaceId());
        po.setOrgUnitId(entity.getOrgUnitId());
        po.setRelationType(entity.getRelationType().name());
        po.setIsPrimary(entity.isPrimary());
        po.setPriorityLevel(entity.getPriorityLevel());
        po.setCanUse(entity.isCanUse());
        po.setCanManage(entity.isCanManage());
        po.setCanAssign(entity.isCanAssign());
        po.setCanInspect(entity.isCanInspect());
        po.setUseSchedule(entity.getUseSchedule());
        po.setStartDate(entity.getStartDate());
        po.setEndDate(entity.getEndDate());
        po.setAllocatedCapacity(entity.getAllocatedCapacity());
        po.setWeightRatio(entity.getWeightRatio());
        po.setSortOrder(entity.getSortOrder());
        po.setRemark(entity.getRemark());
        po.setCreatedBy(entity.getCreatedBy());
        return po;
    }

    private SpaceOrgRelation toDomain(SpaceOrgRelationPO po) {
        return SpaceOrgRelation.builder()
                .id(po.getId())
                .spaceId(po.getSpaceId())
                .orgUnitId(po.getOrgUnitId())
                .relationType(SpaceOrgRelation.RelationType.valueOf(po.getRelationType()))
                .isPrimary(Boolean.TRUE.equals(po.getIsPrimary()))
                .priorityLevel(po.getPriorityLevel())
                .canUse(Boolean.TRUE.equals(po.getCanUse()))
                .canManage(Boolean.TRUE.equals(po.getCanManage()))
                .canAssign(Boolean.TRUE.equals(po.getCanAssign()))
                .canInspect(Boolean.TRUE.equals(po.getCanInspect()))
                .useSchedule(po.getUseSchedule())
                .startDate(po.getStartDate())
                .endDate(po.getEndDate())
                .allocatedCapacity(po.getAllocatedCapacity())
                .weightRatio(po.getWeightRatio() != null ? po.getWeightRatio() : new BigDecimal("100.00"))
                .sortOrder(po.getSortOrder())
                .remark(po.getRemark())
                .createdBy(po.getCreatedBy())
                .build();
    }
}
