package com.school.management.infrastructure.persistence.access;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色数据权限 DDD Mapper接口
 */
@Mapper
public interface RoleDataPermissionDomainMapper extends BaseMapper<RoleDataPermissionPO> {

    @Select("SELECT * FROM role_data_permissions WHERE role_id = #{roleId}")
    List<RoleDataPermissionPO> selectByRoleId(@Param("roleId") Long roleId);

    @Delete("DELETE FROM role_data_permissions WHERE role_id = #{roleId}")
    int physicalDeleteByRoleId(@Param("roleId") Long roleId);
}
