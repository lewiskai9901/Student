package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.UserDataScope;
import com.school.management.dto.UserDataScopeDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户数据范围Mapper
 *
 * @author system
 * @version 3.0.0
 * @deprecated 用户级数据范围已废弃，请使用角色级数据权限配置
 */
@Deprecated(since = "4.0.0", forRemoval = true)
@Mapper
@SuppressWarnings("deprecation")
public interface UserDataScopeMapper extends BaseMapper<UserDataScope> {

    /**
     * 根据用户ID查询数据范围列表
     *
     * @param userId 用户ID
     * @return 数据范围列表
     */
    List<UserDataScope> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID查询数据范围DTO列表（包含关联信息）
     *
     * @param userId 用户ID
     * @return 数据范围DTO列表
     */
    List<UserDataScopeDTO> selectDTOByUserId(@Param("userId") Long userId);

    /**
     * 根据范围类型和范围ID查询拥有该范围的用户列表
     *
     * @param scopeType 范围类型
     * @param scopeId 范围ID
     * @return 数据范围DTO列表
     */
    List<UserDataScopeDTO> selectByScopeTypeAndId(
            @Param("scopeType") String scopeType,
            @Param("scopeId") Long scopeId
    );

    /**
     * 检查用户是否有指定范围的权限
     *
     * @param userId 用户ID
     * @param scopeType 范围类型
     * @param scopeId 范围ID
     * @return 是否存在
     */
    boolean existsByUserAndScope(
            @Param("userId") Long userId,
            @Param("scopeType") String scopeType,
            @Param("scopeId") Long scopeId
    );

    /**
     * 根据用户ID和范围类型查询范围ID列表
     *
     * @param userId 用户ID
     * @param scopeType 范围类型
     * @return 范围ID列表
     */
    List<Long> selectScopeIdsByUserAndType(
            @Param("userId") Long userId,
            @Param("scopeType") String scopeType
    );

    /**
     * 获取用户所有可访问的部门ID（包含子部门）
     *
     * @param userId 用户ID
     * @return 部门ID列表
     */
    List<Long> selectAccessibleDeptIds(@Param("userId") Long userId);

    /**
     * 获取用户所有可访问的年级ID
     *
     * @param userId 用户ID
     * @return 年级ID列表
     */
    List<Long> selectAccessibleGradeIds(@Param("userId") Long userId);

    /**
     * 获取用户所有可访问的班级ID
     * 包含：直接分配的班级 + 通过年级继承的班级 + 通过部门继承的班级
     *
     * @param userId 用户ID
     * @return 班级ID列表
     */
    List<Long> selectAccessibleClassIds(@Param("userId") Long userId);

    /**
     * 批量删除用户的数据范围
     *
     * @param userId 用户ID
     * @param scopeType 范围类型（可选，为null则删除所有类型）
     * @return 删除数量
     */
    int deleteByUserIdAndType(
            @Param("userId") Long userId,
            @Param("scopeType") String scopeType
    );

    /**
     * 根据范围ID删除所有用户的该范围（用于范围被删除时清理）
     *
     * @param scopeType 范围类型
     * @param scopeId 范围ID
     * @return 删除数量
     */
    int deleteByScopeTypeAndId(
            @Param("scopeType") String scopeType,
            @Param("scopeId") Long scopeId
    );
}
