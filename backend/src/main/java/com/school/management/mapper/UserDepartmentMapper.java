package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.UserDepartment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户部门关联Mapper
 *
 * @author system
 * @since 1.0.0
 */
@Mapper
public interface UserDepartmentMapper extends BaseMapper<UserDepartment> {

    /**
     * 根据用户ID查询组织单元ID列表
     *
     * @param userId 用户ID
     * @return 组织单元ID列表
     */
    @Select("SELECT org_unit_id FROM user_departments WHERE user_id = #{userId}")
    List<Long> selectOrgUnitIdsByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID查询用户组织单元关联信息
     *
     * @param userId 用户ID
     * @return 用户组织单元关联列表
     */
    @Select("SELECT ud.*, ou.name as orgUnitName " +
            "FROM user_departments ud " +
            "LEFT JOIN org_units ou ON ud.org_unit_id = ou.id " +
            "WHERE ud.user_id = #{userId}")
    List<UserDepartment> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据组织单元ID查询用户ID列表
     *
     * @param orgUnitId 组织单元ID
     * @return 用户ID列表
     */
    @Select("SELECT user_id FROM user_departments WHERE org_unit_id = #{orgUnitId}")
    List<Long> selectUserIdsByOrgUnitId(@Param("orgUnitId") Long orgUnitId);
}
