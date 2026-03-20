package com.school.management.infrastructure.persistence.organization;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

@Mapper
public interface UserPositionMapper extends BaseMapper<UserPositionPO> {
    @Select("SELECT * FROM user_positions WHERE user_id = #{userId} ORDER BY start_date DESC")
    List<UserPositionPO> findByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM user_positions WHERE user_id = #{userId} AND end_date IS NULL ORDER BY is_primary DESC")
    List<UserPositionPO> findCurrentByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM user_positions WHERE position_id = #{positionId} ORDER BY start_date DESC")
    List<UserPositionPO> findByPositionId(@Param("positionId") Long positionId);

    @Select("SELECT * FROM user_positions WHERE position_id = #{positionId} AND end_date IS NULL")
    List<UserPositionPO> findCurrentByPositionId(@Param("positionId") Long positionId);

    @Select("SELECT * FROM user_positions WHERE user_id = #{userId} AND is_primary = 1 AND end_date IS NULL LIMIT 1")
    UserPositionPO findPrimaryByUserId(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM user_positions WHERE position_id = #{positionId} AND end_date IS NULL")
    int countCurrentByPositionId(@Param("positionId") Long positionId);

    @Select("SELECT up.* FROM user_positions up " +
            "JOIN positions p ON p.id = up.position_id AND p.deleted = 0 " +
            "WHERE p.org_unit_id = #{orgUnitId} AND up.end_date IS NULL " +
            "ORDER BY up.is_primary DESC, up.start_date")
    List<UserPositionPO> findCurrentByOrgUnitId(@Param("orgUnitId") Long orgUnitId);

    /**
     * 结束某组织下所有在任岗位（设置 end_date = NOW()）
     */
    @Update("UPDATE user_positions up " +
            "JOIN positions p ON p.id = up.position_id " +
            "SET up.end_date = NOW(), up.departure_reason = #{reason} " +
            "WHERE p.org_unit_id = #{orgUnitId} AND up.end_date IS NULL")
    int endAllByOrgUnitId(@Param("orgUnitId") Long orgUnitId, @Param("reason") String reason);
}
