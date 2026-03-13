package com.school.management.infrastructure.persistence.relation;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 用户-场所关系Mapper
 */
@Mapper
public interface UserSpaceRelationMapper extends BaseMapper<UserSpaceRelationPO> {

    /**
     * 查询用户的所有场所关系
     */
    @Select("SELECT * FROM user_space_relations WHERE user_id = #{userId} AND deleted = 0 ORDER BY is_primary DESC, sort_order")
    List<UserSpaceRelationPO> findByUserId(@Param("userId") Long userId);

    /**
     * 查询用户的主要场所
     */
    @Select("SELECT * FROM user_space_relations WHERE user_id = #{userId} AND is_primary = 1 AND deleted = 0 LIMIT 1")
    UserSpaceRelationPO findPrimaryByUserId(@Param("userId") Long userId);

    /**
     * 查询场所的所有用户关系
     */
    @Select("SELECT * FROM user_space_relations WHERE space_id = #{spaceId} AND deleted = 0 ORDER BY position_code, sort_order")
    List<UserSpaceRelationPO> findBySpaceId(@Param("spaceId") Long spaceId);

    /**
     * 查询场所的分配用户
     */
    @Select("SELECT * FROM user_space_relations WHERE space_id = #{spaceId} AND relation_type = 'ASSIGNED' AND deleted = 0 ORDER BY position_code")
    List<UserSpaceRelationPO> findAssignedBySpaceId(@Param("spaceId") Long spaceId);

    /**
     * 查询用户在指定场所的关系
     */
    @Select("SELECT * FROM user_space_relations WHERE user_id = #{userId} AND space_id = #{spaceId} AND deleted = 0")
    List<UserSpaceRelationPO> findByUserAndSpace(@Param("userId") Long userId, @Param("spaceId") Long spaceId);

    /**
     * 查询用户的有效场所关系
     */
    @Select("SELECT * FROM user_space_relations WHERE user_id = #{userId} AND deleted = 0 " +
            "AND (end_date IS NULL OR end_date >= CURDATE()) " +
            "ORDER BY is_primary DESC, sort_order")
    List<UserSpaceRelationPO> findActiveByUserId(@Param("userId") Long userId);

    /**
     * 根据位置编码查询
     */
    @Select("SELECT * FROM user_space_relations WHERE space_id = #{spaceId} AND position_code = #{positionCode} AND deleted = 0 LIMIT 1")
    UserSpaceRelationPO findBySpaceAndPosition(@Param("spaceId") Long spaceId, @Param("positionCode") String positionCode);

    /**
     * 查询未缴费的关系
     */
    @Select("SELECT * FROM user_space_relations WHERE user_id = #{userId} AND fee_paid = 0 AND fee_amount > 0 AND deleted = 0")
    List<UserSpaceRelationPO> findUnpaidByUserId(@Param("userId") Long userId);

    /**
     * 清除用户的主要场所标记
     */
    @Update("UPDATE user_space_relations SET is_primary = 0 WHERE user_id = #{userId} AND deleted = 0")
    int clearPrimaryByUserId(@Param("userId") Long userId);

    /**
     * 检查用户是否已有主要场所
     */
    @Select("SELECT COUNT(*) > 0 FROM user_space_relations WHERE user_id = #{userId} AND is_primary = 1 AND deleted = 0")
    boolean existsPrimaryByUserId(@Param("userId") Long userId);

    /**
     * 检查关系是否存在
     */
    @Select("SELECT COUNT(*) > 0 FROM user_space_relations WHERE user_id = #{userId} AND space_id = #{spaceId} AND relation_type = #{relationType} AND deleted = 0")
    boolean existsRelation(@Param("userId") Long userId, @Param("spaceId") Long spaceId, @Param("relationType") String relationType);

    /**
     * 检查位置是否已被占用
     */
    @Select("SELECT COUNT(*) > 0 FROM user_space_relations WHERE space_id = #{spaceId} AND position_code = #{positionCode} AND deleted = 0")
    boolean existsPosition(@Param("spaceId") Long spaceId, @Param("positionCode") String positionCode);

    /**
     * 统计场所的用户数
     */
    @Select("SELECT COUNT(*) FROM user_space_relations WHERE space_id = #{spaceId} AND deleted = 0")
    int countBySpaceId(@Param("spaceId") Long spaceId);

    /**
     * 统计用户的场所数
     */
    @Select("SELECT COUNT(*) FROM user_space_relations WHERE user_id = #{userId} AND deleted = 0")
    int countByUserId(@Param("userId") Long userId);

    /**
     * 统计场所已分配的位置数
     */
    @Select("SELECT COUNT(*) FROM user_space_relations WHERE space_id = #{spaceId} AND position_code IS NOT NULL AND deleted = 0")
    int countAssignedPositions(@Param("spaceId") Long spaceId);
}
