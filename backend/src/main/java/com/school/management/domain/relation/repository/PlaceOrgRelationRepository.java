package com.school.management.domain.relation.repository;

import com.school.management.domain.relation.model.entity.SpaceOrgRelation;

import java.util.List;
import java.util.Optional;

/**
 * 场所-组织关系仓储接口
 */
public interface SpaceOrgRelationRepository {

    /**
     * 保存关系
     */
    SpaceOrgRelation save(SpaceOrgRelation relation);

    /**
     * 根据ID查询
     */
    Optional<SpaceOrgRelation> findById(Long id);

    /**
     * 查询场所的所有关系
     */
    List<SpaceOrgRelation> findBySpaceId(Long spaceId);

    /**
     * 查询场所的有效关系
     */
    List<SpaceOrgRelation> findActiveBySpaceId(Long spaceId);

    /**
     * 查询场所的主归属
     */
    Optional<SpaceOrgRelation> findPrimaryBySpaceId(Long spaceId);

    /**
     * 查询组织的所有场所关系
     */
    List<SpaceOrgRelation> findByOrgUnitId(Long orgUnitId);

    /**
     * 查询组织的主管场所
     */
    List<SpaceOrgRelation> findPrimaryByOrgUnitId(Long orgUnitId);

    /**
     * 查询场所在指定组织的关系
     */
    List<SpaceOrgRelation> findBySpaceAndOrg(Long spaceId, Long orgUnitId);

    /**
     * 查询具有检查权限的关系
     */
    List<SpaceOrgRelation> findInspectableBySpaceId(Long spaceId);

    /**
     * 查询组织可检查的场所关系
     */
    List<SpaceOrgRelation> findInspectableByOrgUnitId(Long orgUnitId);

    /**
     * 查询共用场所关系
     */
    List<SpaceOrgRelation> findSharedBySpaceId(Long spaceId);

    /**
     * 清除场所的主归属标记
     */
    void clearPrimaryBySpaceId(Long spaceId);

    /**
     * 检查场所是否已有主归属
     */
    boolean existsPrimaryBySpaceId(Long spaceId);

    /**
     * 检查关系是否存在
     */
    boolean existsRelation(Long spaceId, Long orgUnitId, SpaceOrgRelation.RelationType relationType);

    /**
     * 统计组织管理的场所数
     */
    int countByOrgUnitId(Long orgUnitId);

    /**
     * 统计场所的组织归属数
     */
    int countBySpaceId(Long spaceId);

    /**
     * 删除关系
     */
    void deleteById(Long id);

    /**
     * 删除场所的所有关系
     */
    void deleteBySpaceId(Long spaceId);
}
