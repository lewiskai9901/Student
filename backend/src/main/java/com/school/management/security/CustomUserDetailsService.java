package com.school.management.security;

import com.school.management.domain.access.model.ScopeType;
import com.school.management.domain.access.model.UserRole;
import com.school.management.domain.access.model.entity.AccessRelation;
import com.school.management.domain.access.repository.AccessRelationRepository;
import com.school.management.domain.access.repository.UserRoleRepository;
import com.school.management.infrastructure.access.UserContext;
import com.school.management.infrastructure.persistence.user.UserDomainMapper;
import com.school.management.infrastructure.persistence.user.UserPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 自定义用户详情服务 - 使用DDD UserDomainMapper
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserDomainMapper userDomainMapper;
    private final AccessRelationRepository accessRelationRepository;
    private final UserRoleRepository userRoleRepository;
    private final JdbcTemplate jdbcTemplate;

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

        return buildUserDetails(user, roles, permissions);
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

        return buildUserDetails(user, roles, permissions);
    }

    private CustomUserDetails buildUserDetails(UserPO user, List<String> roles, List<String> permissions) {
        // orgUnitId 直接从 users.primary_org_unit_id 获取
        Long orgUnitId = user.getPrimaryOrgUnitId();

        // 查询角色ID列表
        List<Long> roleIds = userDomainMapper.findRoleIdsByUserId(user.getId());

        // 查询组织单元路径
        String orgUnitPath = null;
        if (orgUnitId != null) {
            try {
                orgUnitPath = jdbcTemplate.queryForObject(
                        "SELECT tree_path FROM org_units WHERE id = ? AND deleted = 0",
                        String.class, orgUnitId);
            } catch (Exception e) {
                log.warn("查询组织路径失败, orgUnitId={}: {}", orgUnitId, e.getMessage());
            }
        }

        // 加载带作用域的角色分配
        List<UserContext.ScopedRoleInfo> scopedRoles = loadScopedRoles(user.getId());

        CustomUserDetails details = new CustomUserDetails(
                user.getId(), user.getUsername(), user.getPassword(), user.getRealName(),
                user.getStatus(), orgUnitId, 1L,
                roles, permissions);
        details.setRoleIds(roleIds != null ? roleIds : Collections.emptyList());
        details.setOrgUnitPath(orgUnitPath);
        details.setScopedRoles(scopedRoles);
        return details;
    }

    /**
     * 加载用户的带作用域角色分配列表，预查询 ORG_UNIT scope 的 org path
     */
    private List<UserContext.ScopedRoleInfo> loadScopedRoles(Long userId) {
        List<UserRole> activeRoles = userRoleRepository.findActiveByUserId(userId);
        if (activeRoles.isEmpty()) {
            return Collections.emptyList();
        }

        List<UserContext.ScopedRoleInfo> scopedRoles = new ArrayList<>(activeRoles.size());
        for (UserRole ur : activeRoles) {
            String scopeOrgPath = null;
            if (ScopeType.ORG_UNIT.equals(ur.getScopeType()) && ur.getScopeId() != null && ur.getScopeId() > 0) {
                try {
                    scopeOrgPath = jdbcTemplate.queryForObject(
                            "SELECT tree_path FROM org_units WHERE id = ? AND deleted = 0",
                            String.class, ur.getScopeId());
                } catch (Exception e) {
                    log.warn("查询 scope org path 失败, scopeId={}: {}", ur.getScopeId(), e.getMessage());
                }
            }
            scopedRoles.add(UserContext.ScopedRoleInfo.builder()
                    .roleId(ur.getRoleId())
                    .scopeType(ur.getScopeType())
                    .scopeId(ur.getScopeId())
                    .scopeOrgPath(scopeOrgPath)
                    .build());
        }
        return scopedRoles;
    }
}
