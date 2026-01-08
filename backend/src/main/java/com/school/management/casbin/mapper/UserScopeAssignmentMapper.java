package com.school.management.casbin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.casbin.dto.ScopeAssignmentDTO;
import com.school.management.casbin.entity.UserScopeAssignment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户范围分配Mapper接口
 *
 * @author system
 * @version 1.0.0
 */
@Mapper
public interface UserScopeAssignmentMapper extends BaseMapper<UserScopeAssignment> {

    /**
     * 根据用户ID查询范围分配（包含用户信息）
     *
     * @param userId 用户ID
     * @return 范围分配列表
     */
    List<ScopeAssignmentDTO> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据范围表达式查询用户分配
     *
     * @param scopeExpression 范围表达式
     * @return 用户范围分配列表
     */
    List<ScopeAssignmentDTO> selectByScopeExpression(@Param("scopeExpression") String scopeExpression);

    /**
     * 批量删除用户范围（软删除）
     *
     * @param userId 用户ID
     * @param scopes 范围表达式列表
     * @return 影响行数
     */
    int batchSoftDelete(@Param("userId") Long userId, @Param("scopes") List<String> scopes);
}
