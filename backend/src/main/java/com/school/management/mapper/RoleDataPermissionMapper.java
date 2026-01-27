package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.RoleDataPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色数据权限Mapper接口
 */
@Mapper
public interface RoleDataPermissionMapper extends BaseMapper<RoleDataPermission> {

    /**
     * 根据角色ID和模块编码查询数据权限
     */
    RoleDataPermission selectByRoleAndModule(@Param("roleId") Long roleId, @Param("moduleCode") String moduleCode);

    /**
     * 根据角色ID列表和模块编码查询数据权限（取最高权限）
     */
    List<RoleDataPermission> selectByRoleIdsAndModule(@Param("roleIds") List<Long> roleIds, @Param("moduleCode") String moduleCode);

    /**
     * 根据角色ID查询所有数据权限配置
     */
    List<RoleDataPermission> selectByRoleId(@Param("roleId") Long roleId);
    /**
     * 根据角色ID物理删除数据权限配置（绕过逻辑删除）
     */
    @org.apache.ibatis.annotations.Delete("DELETE FROM role_data_permissions WHERE role_id = #{roleId}")
    int physicalDeleteByRoleId(@Param("roleId") Long roleId);

}
