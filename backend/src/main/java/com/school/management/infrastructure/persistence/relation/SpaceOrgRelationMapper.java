package com.school.management.infrastructure.persistence.relation;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 场所-组织关系Mapper
 */
@Mapper
public interface SpaceOrgRelationMapper extends BaseMapper<SpaceOrgRelationPO> {

    /**
     * 查询场所的所有关系
     */
    @Select("SELECT * FROM space_org_relations WHERE space_id = #{spaceId} AND deleted = 0 ORDER BY is_primary DESC, priority_level, sort_order")
    List<SpaceOrgRelationPO> findBySpaceId(@Param("spaceId") Long spaceId);

    /**
     * 查询场所的主归属
     */
    @Select("SELECT * FROM space_org_relations WHERE space_id = #{spaceId} AND is_primary = 1 AND deleted = 0 LIMIT 1")
    SpaceOrgRelationPO findPrimaryBySpaceId(@Param("spaceId") Long spaceId);

    /**
     * 查询组织的所有场所关系
     */
    @Select("SELECT * FROM space_org_relations WHERE org_unit_id = #{orgUnitId} AND deleted = 0 ORDER BY is_primary DESC, sort_order")
    List<SpaceOrgRelationPO> findByOrgUnitId(@Param("orgUnitId") Long orgUnitId);

    /**
     * 查询组织的主管场所
     */
    @Select("SELECT * FROM space_org_relations WHERE org_unit_id = #{orgUnitId} AND is_primary = 1 AND deleted = 0")
    List<SpaceOrgRelationPO> findPrimaryByOrgUnitId(@Param("orgUnitId") Long orgUnitId);

    /**
     * 查询场所在指定组织的关系
     */
    @Select("SELECT * FROM space_org_relations WHERE space_id = #{spaceId} AND org_unit_id = #{orgUnitId} AND deleted = 0")
    List<SpaceOrgRelationPO> findBySpaceAndOrg(@Param("spaceId") Long spaceId, @Param("orgUnitId") Long orgUnitId);

    /**
     * 查询场所的有效关系（排除过期的）
     */
    @Select("SELECT * FROM space_org_relations WHERE space_id = #{spaceId} AND deleted = 0 " +
            "AND (end_date IS NULL OR end_date >= CURDATE()) " +
            "ORDER BY is_primary DESC, priority_level, sort_order")
    List<SpaceOrgRelationPO> findActiveBySpaceId(@Param("spaceId") Long spaceId);

    /**
     * 查询具有检查权限的关系
     */
    @Select("SELECT * FROM space_org_relations WHERE space_id = #{spaceId} AND can_inspect = 1 AND deleted = 0 " +
            "AND (end_date IS NULL OR end_date >= CURDATE())")
    List<SpaceOrgRelationPO> findInspectableBySpaceId(@Param("spaceId") Long spaceId);

    /**
     * 查询组织可检查的场所
     */
    @Select("SELECT * FROM space_org_relations WHERE org_unit_id = #{orgUnitId} AND can_inspect = 1 AND deleted = 0 " +
            "AND (end_date IS NULL OR end_date >= CURDATE())")
    List<SpaceOrgRelationPO> findInspectableByOrgUnitId(@Param("orgUnitId") Long orgUnitId);

    /**
     * 清除场所的主归属标记
     */
    @Update("UPDATE space_org_relations SET is_primary = 0 WHERE space_id = #{spaceId} AND deleted = 0")
    int clearPrimaryBySpaceId(@Param("spaceId") Long spaceId);

    /**
     * 检查场所是否已有主归属
     */
    @Select("SELECT COUNT(*) > 0 FROM space_org_relations WHERE space_id = #{spaceId} AND is_primary = 1 AND deleted = 0")
    boolean existsPrimaryBySpaceId(@Param("spaceId") Long spaceId);

    /**
     * 检查关系是否存在
     */
    @Select("SELECT COUNT(*) > 0 FROM space_org_relations WHERE space_id = #{spaceId} AND org_unit_id = #{orgUnitId} AND relation_type = #{relationType} AND deleted = 0")
    boolean existsRelation(@Param("spaceId") Long spaceId, @Param("orgUnitId") Long orgUnitId, @Param("relationType") String relationType);

    /**
     * 统计组织管理的场所数
     */
    @Select("SELECT COUNT(*) FROM space_org_relations WHERE org_unit_id = #{orgUnitId} AND deleted = 0")
    int countByOrgUnitId(@Param("orgUnitId") Long orgUnitId);

    /**
     * 统计场所的组织归属数
     */
    @Select("SELECT COUNT(*) FROM space_org_relations WHERE space_id = #{spaceId} AND deleted = 0")
    int countBySpaceId(@Param("spaceId") Long spaceId);

    /**
     * 查询共用场所关系
     */
    @Select("SELECT * FROM space_org_relations WHERE space_id = #{spaceId} AND relation_type = 'SHARED' AND deleted = 0 ORDER BY priority_level")
    List<SpaceOrgRelationPO> findSharedBySpaceId(@Param("spaceId") Long spaceId);
}
