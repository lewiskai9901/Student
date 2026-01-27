package com.school.management.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.dto.*;
import com.school.management.entity.Department;
import com.school.management.entity.User;
import com.school.management.entity.UserRole;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.ClassMapper;
import com.school.management.mapper.DepartmentMapper;
import com.school.management.mapper.UserMapper;
import com.school.management.mapper.UserRoleMapper;
import com.school.management.security.CustomUserDetails;
import com.school.management.service.UserService;
import com.school.management.util.PasswordValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 *
 * @author system
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserRoleMapper userRoleMapper;
    private final DepartmentMapper departmentMapper;
    private final ClassMapper classMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserResponse createUser(UserCreateRequest request) {
        log.info("创建用户: {}", request.getUsername());

        // 校验用户名唯一性
        if (existsUsername(request.getUsername(), null)) {
            throw new BusinessException("用户名已存在");
        }

        // 校验手机号唯一性
        if (StrUtil.isNotBlank(request.getPhone()) && existsByPhone(request.getPhone(), null)) {
            throw new BusinessException("手机号已存在");
        }

        // 校验邮箱唯一性
        if (StrUtil.isNotBlank(request.getEmail()) && existsByEmail(request.getEmail(), null)) {
            throw new BusinessException("邮箱已存在");
        }

        // 校验密码强度
        PasswordValidator.validate(request.getPassword());

        // 创建用户实体
        User user = new User();
        BeanUtil.copyProperties(request, user);

        // 加密密码
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // 设置创建信息
        Long currentUserId = getCurrentUserId();
        user.setCreatedBy(currentUserId);
        user.setUpdatedBy(currentUserId);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        // 保存用户
        userMapper.insert(user);

        // 分配角色
        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            assignRoles(user.getId(), request.getRoleIds());
        }

        log.info("用户创建成功: {} -> {}", request.getUsername(), user.getId());
        return buildUserResponse(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        log.info("更新用户: {}", id);

        // 查询用户
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 校验手机号唯一性
        if (StrUtil.isNotBlank(request.getPhone()) && existsByPhone(request.getPhone(), id)) {
            throw new BusinessException("手机号已存在");
        }

        // 校验邮箱唯一性
        if (StrUtil.isNotBlank(request.getEmail()) && existsByEmail(request.getEmail(), id)) {
            throw new BusinessException("邮箱已存在");
        }

        // 更新用户信息
        BeanUtil.copyProperties(request, user, "id", "username", "password", "createTime", "createBy");
        user.setUpdatedBy(getCurrentUserId());
        user.setUpdatedAt(LocalDateTime.now());

        // 更新用户
        userMapper.updateById(user);

        // 更新角色
        if (request.getRoleIds() != null) {
            assignRoles(id, request.getRoleIds());
        }

        log.info("用户更新成功: {}", id);
        return buildUserResponse(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        log.info("删除用户: {}", id);

        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 逻辑删除
        userMapper.deleteById(id);

        log.info("用户删除成功: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUsers(List<Long> ids) {
        log.info("批量删除用户: {}", ids);

        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("用户ID列表不能为空");
        }

        // 批量逻辑删除
        userMapper.deleteBatchIds(ids);

        log.info("批量删除用户成功: {}", ids);
    }

    @Override
    public UserResponse getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        return buildUserResponse(user);
    }

    @Override
    public Page<UserResponse> getUserPage(UserQueryRequest request) {
        // 构建查询条件 - 使用 LambdaQueryWrapper 避免字段名问题
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();

        if (StrUtil.isNotBlank(request.getUsername())) {
            queryWrapper.like(User::getUsername, request.getUsername());
        }
        if (StrUtil.isNotBlank(request.getRealName())) {
            queryWrapper.like(User::getRealName, request.getRealName());
        }
        if (StrUtil.isNotBlank(request.getPhone())) {
            queryWrapper.like(User::getPhone, request.getPhone());
        }
        if (request.getGender() != null) {
            queryWrapper.eq(User::getGender, request.getGender());
        }
        if (request.getStatus() != null) {
            queryWrapper.eq(User::getStatus, request.getStatus());
        }
        if (request.getOrgUnitId() != null) {
            queryWrapper.eq(User::getOrgUnitId, request.getOrgUnitId());
        }
        if (request.getClassId() != null) {
            queryWrapper.eq(User::getClassId, request.getClassId());
        }
        if (StrUtil.isNotBlank(request.getStudentId())) {
            queryWrapper.like(User::getEmployeeNo, request.getStudentId());
        }
        if (request.getUserType() != null) {
            queryWrapper.eq(User::getUserType, request.getUserType());
        }

        // 时间范围查询
        if (StrUtil.isNotBlank(request.getCreateTimeStart())) {
            queryWrapper.ge(User::getCreatedAt, request.getCreateTimeStart());
        }
        if (StrUtil.isNotBlank(request.getCreateTimeEnd())) {
            queryWrapper.le(User::getCreatedAt, request.getCreateTimeEnd());
        }

        // 排序 - 默认按创建时间降序
        queryWrapper.orderByDesc(User::getCreatedAt);

        // 分页查询 - 兼容 page/pageNum 和 size/pageSize
        int pageNum = request.getPageNum() != null ? request.getPageNum() :
                      (request.getPage() != null ? request.getPage() : 1);
        int pageSize = request.getPageSize() != null ? request.getPageSize() :
                       (request.getSize() != null ? request.getSize() : 10);

        Page<User> page = new Page<>(pageNum, pageSize);
        IPage<User> userPage = userMapper.selectPage(page, queryWrapper);

        // 转换结果
        Page<UserResponse> responsePage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        List<UserResponse> userResponses = userPage.getRecords().stream()
                .map(this::buildUserResponse)
                .collect(Collectors.toList());
        responsePage.setRecords(userResponses);

        return responsePage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String resetPassword(Long id) {
        log.info("重置用户密码: {}", id);

        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 生成随机强密码 (12位: 包含大小写字母、数字、特殊字符)
        String newPassword = generateSecurePassword();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedBy(getCurrentUserId());
        user.setUpdatedAt(LocalDateTime.now());
        user.setPasswordChangedAt(LocalDateTime.now());

        userMapper.updateById(user);

        log.info("用户密码重置成功: userId={}", id);
        return newPassword;
    }

    /**
     * 生成安全随机密码
     * @return 12位随机密码，包含大小写字母、数字和特殊字符
     */
    private String generateSecurePassword() {
        String upperCase = "ABCDEFGHJKLMNPQRSTUVWXYZ"; // 排除易混淆的I和O
        String lowerCase = "abcdefghjkmnpqrstuvwxyz"; // 排除易混淆的i, l, o
        String digits = "23456789"; // 排除易混淆的0和1
        String special = "@#$%&*!";

        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder password = new StringBuilder();

        // 确保包含每种类型的字符
        password.append(upperCase.charAt(random.nextInt(upperCase.length())));
        password.append(lowerCase.charAt(random.nextInt(lowerCase.length())));
        password.append(digits.charAt(random.nextInt(digits.length())));
        password.append(special.charAt(random.nextInt(special.length())));

        // 填充剩余8位随机字符
        String allChars = upperCase + lowerCase + digits + special;
        for (int i = 0; i < 8; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }

        // 打乱顺序
        char[] chars = password.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }

        return new String(chars);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long id, Integer status) {
        log.info("更新用户状态: {} -> {}", id, status);

        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        user.setStatus(status);
        user.setUpdatedBy(getCurrentUserId());
        user.setUpdatedAt(LocalDateTime.now());

        userMapper.updateById(user);

        log.info("用户状态更新成功: {} -> {}", id, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds) {
        log.info("分配用户角色: {} -> {}", userId, roleIds);

        // 验证用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 删除用户现有角色
        userRoleMapper.deleteByUserId(userId);

        // 插入新的角色关联
        if (roleIds != null && !roleIds.isEmpty()) {
            List<UserRole> userRoles = roleIds.stream()
                    .map(roleId -> {
                        UserRole userRole = new UserRole();
                        userRole.setUserId(userId);
                        userRole.setRoleId(roleId);
                        return userRole;
                    })
                    .collect(Collectors.toList());
            userRoleMapper.insertBatch(userRoles);
        }

        log.info("用户角色分配成功: {} -> {}", userId, roleIds);
    }

    @Override
    public boolean existsUsername(String username, Long excludeId) {
        return userMapper.existsByUsername(username, excludeId);
    }

    @Override
    public boolean existsByPhone(String phone, Long excludeId) {
        return userMapper.existsByPhone(phone, excludeId);
    }

    @Override
    public boolean existsByEmail(String email, Long excludeId) {
        return userMapper.existsByEmail(email, excludeId);
    }

    @Override
    public List<UserResponse> getSimpleUserList(String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getDeleted, 0)
               .eq(User::getStatus, 1); // 只查询启用的用户

        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w
                    .like(User::getRealName, keyword)
                    .or()
                    .like(User::getUsername, keyword)
            );
        }

        wrapper.orderByAsc(User::getRealName)
               .last("LIMIT 50"); // 限制返回数量

        List<User> users = userMapper.selectList(wrapper);
        return users.stream().map(this::buildUserResponse).collect(Collectors.toList());
    }

    /**
     * 构建用户响应对象
     */
    private UserResponse buildUserResponse(User user) {
        UserResponse.UserResponseBuilder builder = UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .gender(user.getGender())
                .status(user.getStatus())
                .orgUnitId(user.getOrgUnitId())
                .classId(user.getClassId())
                .employeeNo(user.getEmployeeNo())
                .userType(user.getUserType())
                .lastLoginTime(user.getLastLoginTime())
                .lastLoginIp(user.getLastLoginIp())
                .createTime(user.getCreatedAt())
                .updateTime(user.getUpdatedAt());

        // 设置性别描述
        if (user.getGender() != null) {
            builder.genderName(user.getGender() == 1 ? "男" : "女");
        }

        // 设置状态描述
        if (user.getStatus() != null) {
            builder.statusName(user.getStatus() == 1 ? "启用" : "禁用");
        }

        // 设置用户类型描述
        if (user.getUserType() != null) {
            String userTypeName = switch (user.getUserType()) {
                case 1 -> "学生";
                case 2 -> "教师";
                case 3 -> "管理员";
                default -> "未知";
            };
            builder.userTypeName(userTypeName);
        }

        // 查询角色名称信息
        List<String> roleNames = userMapper.findRoleNamesByUserId(user.getId());
        builder.roleNames(roleNames);

        // 查询权限信息
        List<String> permissions = userMapper.findPermissionCodesByUserId(user.getId());
        builder.permissions(permissions);

        // 查询组织单元信息
        if (user.getOrgUnitId() != null) {
            Department department = departmentMapper.selectById(user.getOrgUnitId());
            if (department != null) {
                builder.orgUnitName(department.getDeptName());
            }
        }

        // 查询班级信息
        if (user.getClassId() != null) {
            com.school.management.entity.Class classInfo = classMapper.selectById(user.getClassId());
            if (classInfo != null) {
                builder.className(classInfo.getClassName());
            }
        }

        return builder.build();
    }

    /**
     * 获取当前登录用户名
     */
    private String getCurrentUsername() {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext()
                    .getAuthentication().getPrincipal();
            return userDetails.getUsername();
        } catch (Exception e) {
            return "system";
        }
    }

    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext()
                    .getAuthentication().getPrincipal();
            return userDetails.getId();
        } catch (Exception e) {
            return 1L;
        }
    }

    @Override
    public List<Long> getUserRoleIds(Long userId) {
        return userMapper.findRoleIdsByUserId(userId);
    }

    @Override
    public List<UserResponse> getUsersByOrgUnit(Long orgUnitId, Boolean includeChildren, String keyword) {
        List<Long> orgUnitIds = new ArrayList<>();
        orgUnitIds.add(orgUnitId);

        // 如果需要包含下级组织，递归获取所有下级组织ID
        if (Boolean.TRUE.equals(includeChildren)) {
            collectChildOrgUnitIds(orgUnitId, orgUnitIds);
        }

        // 构建查询条件
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getDeleted, 0)
               .eq(User::getStatus, 1) // 只查询启用的用户
               .in(User::getOrgUnitId, orgUnitIds);

        // 关键词搜索
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w
                    .like(User::getRealName, keyword)
                    .or()
                    .like(User::getUsername, keyword)
            );
        }

        wrapper.orderByAsc(User::getRealName);

        List<User> users = userMapper.selectList(wrapper);
        return users.stream().map(this::buildUserResponse).collect(Collectors.toList());
    }

    /**
     * 递归收集下级组织单元ID
     */
    private void collectChildOrgUnitIds(Long parentId, List<Long> result) {
        List<Department> children = departmentMapper.selectList(
                new LambdaQueryWrapper<Department>()
                        .eq(Department::getParentId, parentId)
                        .eq(Department::getDeleted, 0)
                        .eq(Department::getStatus, 1)
        );

        for (Department child : children) {
            result.add(child.getId());
            collectChildOrgUnitIds(child.getId(), result);
        }
    }

    @Override
    public List<UserResponse> getUsersWithOrgUnits(String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getDeleted, 0)
               .eq(User::getStatus, 1); // 只查询启用的用户

        // 关键词搜索
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w
                    .like(User::getRealName, keyword)
                    .or()
                    .like(User::getUsername, keyword)
            );
        }

        wrapper.orderByAsc(User::getOrgUnitId)
               .orderByAsc(User::getRealName)
               .last("LIMIT 200"); // 限制返回数量

        List<User> users = userMapper.selectList(wrapper);
        return users.stream().map(this::buildUserResponse).collect(Collectors.toList());
    }
}
