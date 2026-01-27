package com.school.management.security;

import com.school.management.entity.User;
import com.school.management.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 自定义用户详情服务
 *
 * 注意：数据权限相关的信息（如管理的班级、年级等）不在此处预加载，
 * 而是在 DataPermissionServiceImpl 中根据用户职位动态查询。
 * 这样设计更加灵活，且能正确处理部门层级继承等复杂场景。
 *
 * @author system
 * @version 2.0.0
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("正在加载用户: {}", username);

        // 查询用户信息
        User user = userMapper.findByUsername(username);
        if (user == null) {
            log.warn("用户不存在: {}", username);
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        // 查询用户角色
        List<String> roles = userMapper.findRoleCodesByUserId(user.getId());

        // 查询用户权限
        List<String> permissions = userMapper.findPermissionCodesByUserId(user.getId());

        log.debug("用户 {} 拥有角色: {}, 权限: {}", username, roles, permissions);

        // 注意：managedClassIds 和 gradeLevel 不再预加载
        // 数据权限通过 DataPermissionServiceImpl 动态查询
        return new CustomUserDetails(user, roles, permissions);
    }

    /**
     * 根据用户ID加载用户详情
     */
    public CustomUserDetails loadUserByUserId(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + userId);
        }

        List<String> roles = userMapper.findRoleCodesByUserId(user.getId());
        List<String> permissions = userMapper.findPermissionCodesByUserId(user.getId());

        // 注意：managedClassIds 和 gradeLevel 不再预加载
        // 数据权限通过 DataPermissionServiceImpl 动态查询
        return new CustomUserDetails(user, roles, permissions);
    }
}
