package com.school.management.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.dto.*;

import java.util.List;

/**
 * 用户服务接口
 *
 * @author system
 * @since 1.0.0
 */
public interface UserService {

    /**
     * 创建用户
     *
     * @param request 创建请求
     * @return 用户响应
     */
    UserResponse createUser(UserCreateRequest request);

    /**
     * 更新用户
     *
     * @param id      用户ID
     * @param request 更新请求
     * @return 用户响应
     */
    UserResponse updateUser(Long id, UserUpdateRequest request);

    /**
     * 删除用户
     *
     * @param id 用户ID
     */
    void deleteUser(Long id);

    /**
     * 批量删除用户
     *
     * @param ids 用户ID列表
     */
    void deleteUsers(List<Long> ids);

    /**
     * 根据ID获取用户详情
     *
     * @param id 用户ID
     * @return 用户响应
     */
    UserResponse getUserById(Long id);

    /**
     * 分页查询用户
     *
     * @param request 查询请求
     * @return 分页结果
     */
    Page<UserResponse> getUserPage(UserQueryRequest request);

    /**
     * 重置用户密码
     *
     * @param id 用户ID
     * @return 新生成的随机密码
     */
    String resetPassword(Long id);

    /**
     * 修改用户状态
     *
     * @param id     用户ID
     * @param status 状态
     */
    void updateUserStatus(Long id, Integer status);

    /**
     * 分配角色
     *
     * @param userId  用户ID
     * @param roleIds 角色ID列表
     */
    void assignRoles(Long userId, List<Long> roleIds);

    /**
     * 获取用户的角色ID列表
     *
     * @param userId 用户ID
     * @return 角色ID列表
     */
    List<Long> getUserRoleIds(Long userId);

    /**
     * 检查用户名是否存在
     *
     * @param username  用户名
     * @param excludeId 排除的用户ID
     * @return 是否存在
     */
    boolean existsUsername(String username, Long excludeId);

    /**
     * 检查手机号是否存在
     *
     * @param phone     手机号
     * @param excludeId 排除的用户ID
     * @return 是否存在
     */
    boolean existsByPhone(String phone, Long excludeId);

    /**
     * 检查邮箱是否存在
     *
     * @param email     邮箱
     * @param excludeId 排除的用户ID
     * @return 是否存在
     */
    boolean existsByEmail(String email, Long excludeId);

    /**
     * 获取简单用户列表（用于选择器）
     *
     * @param keyword 搜索关键词（姓名或账号）
     * @return 用户列表
     */
    List<UserResponse> getSimpleUserList(String keyword);

    /**
     * 按部门获取用户列表
     *
     * @param departmentId    部门ID
     * @param includeChildren 是否包含子部门
     * @param keyword         搜索关键词
     * @return 用户列表
     */
    List<UserResponse> getUsersByDepartment(Long departmentId, Boolean includeChildren, String keyword);

    /**
     * 获取带部门信息的用户列表
     *
     * @param keyword 搜索关键词
     * @return 用户列表
     */
    List<UserResponse> getUsersWithDepartments(String keyword);
}
