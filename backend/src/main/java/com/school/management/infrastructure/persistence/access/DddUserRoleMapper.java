package com.school.management.infrastructure.persistence.access;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * MyBatis mapper for user-role mappings (DDD infrastructure).
 */
@Mapper
public interface DddUserRoleMapper extends BaseMapper<UserRolePO> {

    @Select("SELECT * FROM user_roles WHERE user_id = #{userId}")
    List<UserRolePO> findByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM user_roles WHERE user_id = #{userId} AND is_active = 1 " +
            "AND (expires_at IS NULL OR expires_at > NOW())")
    List<UserRolePO> findActiveByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM user_roles WHERE role_id = #{roleId}")
    List<UserRolePO> findByRoleId(@Param("roleId") Long roleId);

    @Select("SELECT * FROM user_roles WHERE user_id = #{userId} AND org_unit_id = #{orgUnitId}")
    List<UserRolePO> findByUserIdAndOrgUnitId(@Param("userId") Long userId, @Param("orgUnitId") Long orgUnitId);

    @Select("SELECT COUNT(*) > 0 FROM user_roles WHERE user_id = #{userId} AND role_id = #{roleId}")
    boolean existsByUserIdAndRoleId(@Param("userId") Long userId, @Param("roleId") Long roleId);

    @Delete("DELETE FROM user_roles WHERE user_id = #{userId}")
    void deleteByUserId(@Param("userId") Long userId);

    @Delete("DELETE FROM user_roles WHERE user_id = #{userId} AND role_id = #{roleId}")
    void deleteByUserIdAndRoleId(@Param("userId") Long userId, @Param("roleId") Long roleId);
}
