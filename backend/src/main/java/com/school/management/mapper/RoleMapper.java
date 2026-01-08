package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色Mapper接口
 *
 * @author system
 * @since 1.0.0
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    /**
     * 分页查询角色
     *
     * @param page 分页参数
     * @param roleName 角色名称
     * @param roleCode 角色编码
     * @param status 状态
     * @return 角色列表
     */
    IPage<Role> selectRolePage(Page<Role> page,
                               @Param("roleName") String roleName,
                               @Param("roleCode") String roleCode,
                               @Param("status") Integer status);

    /**
     * 根据用户ID获取角色列表
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    List<Role> selectRolesByUserId(@Param("userId") Long userId);

    /**
     * 根据角色编码获取角色
     *
     * @param roleCode 角色编码
     * @return 角色
     */
    Role selectByRoleCode(@Param("roleCode") String roleCode);

    /**
     * 检查角色编码是否存在
     *
     * @param roleCode 角色编码
     * @param excludeId 排除的角色ID
     * @return 数量
     */
    Integer countByRoleCode(@Param("roleCode") String roleCode, @Param("excludeId") Long excludeId);
}