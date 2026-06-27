package com.school.management.security;

import com.school.management.domain.access.model.RoleAssignmentScope;
import com.school.management.domain.access.model.UserRole;
import com.school.management.domain.access.model.entity.AccessRelation;
import com.school.management.application.organization.MembershipResolver;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

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
    private final MembershipResolver membershipResolver;
    private final JdbcTemplate jdbcTemplate;

    /**
     * P0-A SQL 性能优化: userDetails 短期缓存 (TTL 30s).
     * 痛点: JwtAuthenticationFilter 在每个 HTTP 请求里都调用 loadUserByUserId,
     * 内部至少 5 条 SQL (user / roles / permissions / orgPath / scopedRoles).
     * 高频请求场景下重复跑这 5 条 SQL 是巨大浪费.
     * 30s TTL 平衡: 角色权限变更最多 30s 内生效, 避免实时重查.
     * 主动失效: invalidateUserCache(userId) — 角色/权限变更时调用.
     */
    private static final long CACHE_TTL_MS = 30_000L;
    private final ConcurrentHashMap<Long, CachedDetails> userDetailsCache = new ConcurrentHashMap<>();
    private static final AtomicLong cacheHits = new AtomicLong(0);
    private static final AtomicLong cacheMisses = new AtomicLong(0);

    private record CachedDetails(CustomUserDetails details, long expiresAt) {
        boolean isFresh() { return System.currentTimeMillis() < expiresAt; }
    }

    /** 角色 / 权限 / 用户信息变更时调用, 强制刷新. */
    public void invalidateUserCache(Long userId) {
        if (userId != null) {
            userDetailsCache.remove(userId);
            log.debug("UserDetails cache invalidated for userId={}", userId);
        }
    }
    /** 全清, 用于批量权限变更后. */
    public void invalidateAllUserCache() {
        userDetailsCache.clear();
        log.info("UserDetails cache fully cleared");
    }
    public long getCacheHits() { return cacheHits.get(); }
    public long getCacheMisses() { return cacheMisses.get(); }

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
            permissions = new java.util.ArrayList<>(userDomainMapper.findAllPermissionCodes());
            permissions.add("*"); // 通配: 超管可见所有菜单/功能, 不受"某权限码在 permissions 表无对应行"影响
            log.debug("用户 {} 是超级管理员，加载全部 {} 个权限(含通配 *)", username, permissions.size());
        } else {
            permissions = userDomainMapper.findPermissionCodesByUserId(user.getId());
        }

        log.debug("用户 {} 拥有角色: {}, 权限数: {}", username, roles, permissions.size());

        return buildUserDetails(user, roles, permissions);
    }

    public CustomUserDetails loadUserByUserId(Long userId) {
        // P0-A: 命中缓存直接返回, 节省 5+ 条 SQL
        CachedDetails cached = userDetailsCache.get(userId);
        if (cached != null && cached.isFresh()) {
            cacheHits.incrementAndGet();
            return cached.details();
        }
        cacheMisses.incrementAndGet();

        UserPO user = userDomainMapper.selectById(userId);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + userId);
        }

        List<String> roles = userDomainMapper.findRoleCodesByUserId(user.getId());
        // SUPER_ADMIN角色自动获取系统所有权限
        List<String> permissions;
        if (roles.contains("SUPER_ADMIN")) {
            permissions = new java.util.ArrayList<>(userDomainMapper.findAllPermissionCodes());
            permissions.add("*"); // 通配: 超管可见所有菜单/功能
        } else {
            permissions = userDomainMapper.findPermissionCodesByUserId(user.getId());
        }

        CustomUserDetails details = buildUserDetails(user, roles, permissions);
        userDetailsCache.put(userId, new CachedDetails(details, System.currentTimeMillis() + CACHE_TTL_MS));
        // 简单 TTL 清理: 缓存超过 1000 条时整体清一次 (开发期单租户场景足够)
        if (userDetailsCache.size() > 1000) {
            userDetailsCache.entrySet().removeIf(e -> !e.getValue().isFresh());
        }
        return details;
    }

    /**
     * 用户类型是否允许登录: 类型 features 显式 {@code canLogin:false} (如访客 GUEST) → false;
     * 其它一律 true (无类型 / 类型未声明该键 / 解析失败) —— 绝不因元数据缺失或异常把用户锁在门外。
     * 与 users.status 一起在 {@link CustomUserDetails#isEnabled()} 里 AND 成最终登录闸。
     */
    private boolean resolveTypeCanLogin(String userTypeCode) {
        if (userTypeCode == null || userTypeCode.isBlank()) {
            return true;
        }
        try {
            String features = jdbcTemplate.queryForObject(
                    "SELECT features FROM entity_type_configs WHERE entity_type='USER' AND type_code=? AND deleted=0",
                    String.class, userTypeCode);
            if (features == null) {
                return true;
            }
            return !features.replaceAll("\\s", "").contains("\"canLogin\":false");
        } catch (Exception e) {
            log.warn("解析用户类型 {} 的 canLogin 失败, 放行登录: {}", userTypeCode, e.getMessage());
            return true;
        }
    }

    private CustomUserDetails buildUserDetails(UserPO user, List<String> roles, List<String> permissions) {
        // orgUnitId 走统一归属入口 (access_relations member 关系), 不再读 users.primary_org_unit_id
        Long orgUnitId = membershipResolver.orgOf(user.getId()).orElse(null);

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
        details.setCanLogin(resolveTypeCanLogin(user.getUserTypeCode()));
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
            if (RoleAssignmentScope.ORG_UNIT.equals(ur.getScopeType()) && ur.getScopeId() != null && ur.getScopeId() > 0) {
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
