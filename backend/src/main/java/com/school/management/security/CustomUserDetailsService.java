package com.school.management.security;

import com.school.management.infrastructure.persistence.user.UserDomainMapper;
import com.school.management.infrastructure.persistence.user.UserPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 自定义用户详情服务 - 使用DDD UserDomainMapper
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserDomainMapper userDomainMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("正在加载用户: {}", username);

        UserPO user = userDomainMapper.findByUsername(username);
        if (user == null) {
            log.warn("用户不存在: {}", username);
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        List<String> roles = userDomainMapper.findRoleCodesByUserId(user.getId());
        // SUPER_ADMIN角色自动获取系统所有权限
        List<String> permissions;
        if (roles.contains("SUPER_ADMIN")) {
            permissions = userDomainMapper.findAllPermissionCodes();
            log.debug("用户 {} 是超级管理员，加载全部 {} 个权限", username, permissions.size());
        } else {
            permissions = userDomainMapper.findPermissionCodesByUserId(user.getId());
        }

        log.debug("用户 {} 拥有角色: {}, 权限数: {}", username, roles, permissions.size());

        return new CustomUserDetails(
                user.getId(), user.getUsername(), user.getPassword(), user.getRealName(),
                user.getStatus(), user.getOrgUnitId(), user.getClassId(), user.getUserType(),
                roles, permissions);
    }

    public CustomUserDetails loadUserByUserId(Long userId) {
        UserPO user = userDomainMapper.selectById(userId);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + userId);
        }

        List<String> roles = userDomainMapper.findRoleCodesByUserId(user.getId());
        // SUPER_ADMIN角色自动获取系统所有权限
        List<String> permissions;
        if (roles.contains("SUPER_ADMIN")) {
            permissions = userDomainMapper.findAllPermissionCodes();
        } else {
            permissions = userDomainMapper.findPermissionCodesByUserId(user.getId());
        }

        return new CustomUserDetails(
                user.getId(), user.getUsername(), user.getPassword(), user.getRealName(),
                user.getStatus(), user.getOrgUnitId(), user.getClassId(), user.getUserType(),
                roles, permissions);
    }
}
