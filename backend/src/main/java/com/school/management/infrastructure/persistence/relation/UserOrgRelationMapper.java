package com.school.management.infrastructure.persistence.relation;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 用户-组织关系Mapper
 */
@Mapper
public interface UserOrgRelationMapper extends BaseMapper<UserOrgRelationPO> {

    /**
     * 查询用户的所有关系
     */
    @Select("SELECT * FROM user_org_relations WHERE user_id = #{userId} AND deleted = 0 ORDER BY is_primary DESC, sort_order")
    List<UserOrgRelationPO> findByUserId(@Param("userId") Long userId);

    /**
     * 查询用户的主归属
     */
    @Select("SELECT * FROM user_org_relations WHERE user_id = #{userId} AND is_primary = 1 AND deleted = 0 LIMIT 1")
    UserOrgRelationPO findPrimaryByUserId(@Param("userId") Long userId);

    /**
     * 查询组织的所有成员关系
     */
    @Select("SELECT * FROM user_org_relations WHERE org_unit_id = #{orgUnitId} AND deleted = 0 ORDER BY is_leader DESC, sort_order")
    List<UserOrgRelationPO> findByOrgUnitId(@Param("orgUnitId") Long orgUnitId);

    /**
     * 查询组织的领导
     */
    @Select("SELECT * FROM user_org_relations WHERE org_unit_id = #{orgUnitId} AND is_leader = 1 AND deleted = 0 ORDER BY position_level, sort_order")
    List<UserOrgRelationPO> findLeadersByOrgUnitId(@Param("orgUnitId") Long orgUnitId);

    /**
     * 查询用户在指定组织的关系
     */
    @Select("SELECT * FROM user_org_relations WHERE user_id = #{userId} AND org_unit_id = #{orgUnitId} AND deleted = 0")
    List<UserOrgRelationPO> findByUserAndOrg(@Param("userId") Long userId, @Param("orgUnitId") Long orgUnitId);

    /**
     * 查询用户的有效关系（排除过期的临时归属）
     */
    @Select("SELECT * FROM user_org_relations WHERE user_id = #{userId} AND deleted = 0 " +
            "AND (relation_type != 'TEMPORARY' OR (end_date IS NULL OR end_date >= CURDATE())) " +
            "ORDER BY is_primary DESC, sort_order")
    List<UserOrgRelationPO> findActiveByUserId(@Param("userId") Long userId);

    /**
     * 清除用户的主归属标记
     */
    @Update("UPDATE user_org_relations SET is_primary = 0 WHERE user_id = #{userId} AND deleted = 0")
    int clearPrimaryByUserId(@Param("userId") Long userId);

    /**
     * 检查用户是否已有主归属
     */
    @Select("SELECT COUNT(*) > 0 FROM user_org_relations WHERE user_id = #{userId} AND is_primary = 1 AND deleted = 0")
    boolean existsPrimaryByUserId(@Param("userId") Long userId);

    /**
     * 检查关系是否存在
     */
    @Select("SELECT COUNT(*) > 0 FROM user_org_relations WHERE user_id = #{userId} AND org_unit_id = #{orgUnitId} AND relation_type = #{relationType} AND deleted = 0")
    boolean existsRelation(@Param("userId") Long userId, @Param("orgUnitId") Long orgUnitId, @Param("relationType") String relationType);

    /**
     * 统计组织成员数
     */
    @Select("SELECT COUNT(*) FROM user_org_relations WHERE org_unit_id = #{orgUnitId} AND deleted = 0")
    int countByOrgUnitId(@Param("orgUnitId") Long orgUnitId);

    /**
     * 统计用户归属数
     */
    @Select("SELECT COUNT(*) FROM user_org_relations WHERE user_id = #{userId} AND deleted = 0")
    int countByUserId(@Param("userId") Long userId);
}
