package com.school.management.casbin.service;

import com.school.management.casbin.enums.ScopeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.casbin.jcasbin.main.Enforcer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Casbin 权限执行服务
 *
 * @author system
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "casbin.enabled", havingValue = "true", matchIfMissing = false)
public class CasbinEnforcerService {

    private final Enforcer enforcer;
    private final ScopeHierarchyCache hierarchyCache;

    // ==================== 权限检查 ====================

    /**
     * 检查用户是否有指定权限
     *
     * @param userId   用户ID
     * @param scope    范围表达式
     * @param resource 资源
     * @param action   操作
     * @return 是否有权限
     */
    public boolean checkPermission(Long userId, String scope, String resource, String action) {
        String sub = "user:" + userId;
        boolean allowed = enforcer.enforce(sub, scope, resource, action);
        log.debug("权限检查: user={}, scope={}, resource={}, action={}, result={}",
                userId, scope, resource, action, allowed);
        return allowed;
    }

    /**
     * 检查用户是否有指定权限（使用默认范围）
     *
     * @param userId   用户ID
     * @param resource 资源
     * @param action   操作
     * @return 是否有权限
     */
    public boolean checkPermission(Long userId, String resource, String action) {
        // 获取用户的所有范围，逐一检查
        Set<String> scopes = getUserScopes(userId);
        for (String scope : scopes) {
            if (checkPermission(userId, scope, resource, action)) {
                return true;
            }
        }
        return false;
    }

    // ==================== 用户范围管理 ====================

    /**
     * 获取用户所有分配的范围
     *
     * @param userId 用户ID
     * @return 范围表达式集合
     */
    public Set<String> getUserScopes(Long userId) {
        String sub = "user:" + userId;
        Set<String> scopes = new HashSet<>();

        // 从 g2 关系中获取用户直接分配的范围
        List<List<String>> g2Rules = enforcer.getFilteredNamedGroupingPolicy("g2", 0, sub);
        for (List<String> rule : g2Rules) {
            if (rule.size() >= 2) {
                scopes.add(rule.get(1));
            }
        }

        return scopes;
    }

    /**
     * 获取用户可访问的班级ID列表
     *
     * @param userId 用户ID
     * @return 班级ID集合，null表示可访问全部
     */
    public Set<Long> getAccessibleClassIds(Long userId) {
        Set<String> scopes = getUserScopes(userId);

        // 如果有全局范围，返回null表示全部
        if (scopes.contains("scope:*")) {
            return null;
        }

        Set<Long> classIds = new HashSet<>();

        for (String scope : scopes) {
            ScopeType type = ScopeType.fromExpression(scope);
            if (type == null) {
                continue;
            }

            switch (type) {
                case CLASS:
                    // 直接解析班级ID
                    String idPart = scope.substring(ScopeType.CLASS.getPrefix().length());
                    Arrays.stream(idPart.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .map(Long::parseLong)
                            .forEach(classIds::add);
                    break;
                case DEPT:
                case GRADE:
                case DEPT_GRADE:
                    // 从缓存中获取包含的班级
                    Set<String> contained = hierarchyCache.getContainedScopes(scope);
                    for (String childScope : contained) {
                        if (childScope.startsWith(ScopeType.CLASS.getPrefix())) {
                            String classIdStr = childScope.substring(ScopeType.CLASS.getPrefix().length());
                            classIds.add(Long.parseLong(classIdStr));
                        }
                    }
                    break;
                case ALL:
                    return null; // 全部权限
                default:
                    break;
            }
        }

        return classIds;
    }

    /**
     * 获取用户可访问的年级ID列表
     *
     * @param userId 用户ID
     * @return 年级ID集合，null表示可访问全部
     */
    public Set<Long> getAccessibleGradeIds(Long userId) {
        Set<String> scopes = getUserScopes(userId);

        if (scopes.contains("scope:*")) {
            return null;
        }

        Set<Long> gradeIds = new HashSet<>();

        for (String scope : scopes) {
            ScopeType type = ScopeType.fromExpression(scope);
            if (type == null) {
                continue;
            }

            switch (type) {
                case GRADE:
                    String idPart = scope.substring(ScopeType.GRADE.getPrefix().length());
                    gradeIds.add(Long.parseLong(idPart));
                    break;
                case DEPT_GRADE:
                    // scope:dept_grade:1:24 -> 24
                    String[] parts = scope.substring(ScopeType.DEPT_GRADE.getPrefix().length()).split(":");
                    if (parts.length >= 2) {
                        gradeIds.add(Long.parseLong(parts[1]));
                    }
                    break;
                case ALL:
                    return null;
                default:
                    break;
            }
        }

        return gradeIds;
    }

    /**
     * 获取用户可访问的部门ID列表
     *
     * @param userId 用户ID
     * @return 部门ID集合，null表示可访问全部
     */
    public Set<Long> getAccessibleDeptIds(Long userId) {
        Set<String> scopes = getUserScopes(userId);

        if (scopes.contains("scope:*")) {
            return null;
        }

        Set<Long> deptIds = new HashSet<>();

        for (String scope : scopes) {
            ScopeType type = ScopeType.fromExpression(scope);
            if (type == null) {
                continue;
            }

            switch (type) {
                case DEPT:
                    String idPart = scope.substring(ScopeType.DEPT.getPrefix().length());
                    deptIds.add(Long.parseLong(idPart));
                    break;
                case DEPT_GRADE:
                    // scope:dept_grade:1:24 -> 1
                    String[] parts = scope.substring(ScopeType.DEPT_GRADE.getPrefix().length()).split(":");
                    if (parts.length >= 1) {
                        deptIds.add(Long.parseLong(parts[0]));
                    }
                    break;
                case ALL:
                    return null;
                default:
                    break;
            }
        }

        return deptIds;
    }

    // ==================== 用户范围分配 ====================

    /**
     * 分配用户范围
     *
     * @param userId 用户ID
     * @param scope  范围表达式
     */
    public void assignUserScope(Long userId, String scope) {
        String sub = "user:" + userId;
        enforcer.addNamedGroupingPolicy("g2", sub, scope);
        hierarchyCache.refreshUserScope(userId);
        log.info("分配用户范围: userId={}, scope={}", userId, scope);
    }

    /**
     * 批量分配用户范围
     *
     * @param userId 用户ID
     * @param scopes 范围表达式列表
     */
    public void assignUserScopes(Long userId, List<String> scopes) {
        String sub = "user:" + userId;
        for (String scope : scopes) {
            enforcer.addNamedGroupingPolicy("g2", sub, scope);
        }
        hierarchyCache.refreshUserScope(userId);
        log.info("批量分配用户范围: userId={}, count={}", userId, scopes.size());
    }

    /**
     * 移除用户范围
     *
     * @param userId 用户ID
     * @param scope  范围表达式
     */
    public void removeUserScope(Long userId, String scope) {
        String sub = "user:" + userId;
        enforcer.removeNamedGroupingPolicy("g2", sub, scope);
        hierarchyCache.refreshUserScope(userId);
        log.info("移除用户范围: userId={}, scope={}", userId, scope);
    }

    /**
     * 移除用户所有范围
     *
     * @param userId 用户ID
     */
    public void removeAllUserScopes(Long userId) {
        String sub = "user:" + userId;
        enforcer.removeFilteredNamedGroupingPolicy("g2", 0, sub);
        hierarchyCache.evictUserScope(userId);
        log.info("移除用户所有范围: userId={}", userId);
    }

    // ==================== 用户角色管理 ====================

    /**
     * 获取用户角色列表
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    public List<String> getUserRoles(Long userId) {
        String sub = "user:" + userId;
        return enforcer.getRolesForUser(sub);
    }

    /**
     * 分配用户角色
     *
     * @param userId 用户ID
     * @param role   角色
     */
    public void assignUserRole(Long userId, String role) {
        String sub = "user:" + userId;
        enforcer.addRoleForUser(sub, role);
        log.info("分配用户角色: userId={}, role={}", userId, role);
    }

    /**
     * 移除用户角色
     *
     * @param userId 用户ID
     * @param role   角色
     */
    public void removeUserRole(Long userId, String role) {
        String sub = "user:" + userId;
        enforcer.deleteRoleForUser(sub, role);
        log.info("移除用户角色: userId={}, role={}", userId, role);
    }

    // ==================== 策略管理 ====================

    /**
     * 添加权限策略
     *
     * @param role     角色
     * @param scope    范围模式
     * @param resource 资源
     * @param action   操作
     */
    public void addPolicy(String role, String scope, String resource, String action) {
        enforcer.addPolicy(role, scope, resource, action, "allow");
        log.info("添加策略: role={}, scope={}, resource={}, action={}", role, scope, resource, action);
    }

    /**
     * 添加拒绝策略
     *
     * @param role     角色
     * @param scope    范围模式
     * @param resource 资源
     * @param action   操作
     */
    public void addDenyPolicy(String role, String scope, String resource, String action) {
        enforcer.addPolicy(role, scope, resource, action, "deny");
        log.info("添加拒绝策略: role={}, scope={}, resource={}, action={}", role, scope, resource, action);
    }

    /**
     * 移除策略
     *
     * @param role     角色
     * @param scope    范围模式
     * @param resource 资源
     * @param action   操作
     */
    public void removePolicy(String role, String scope, String resource, String action) {
        enforcer.removePolicy(role, scope, resource, action, "allow");
        log.info("移除策略: role={}, scope={}, resource={}, action={}", role, scope, resource, action);
    }

    /**
     * 获取所有策略
     *
     * @return 策略列表
     */
    public List<List<String>> getAllPolicies() {
        return enforcer.getPolicy();
    }

    /**
     * 获取角色策略
     *
     * @param role 角色
     * @return 策略列表
     */
    public List<List<String>> getPoliciesForRole(String role) {
        return enforcer.getFilteredPolicy(0, role);
    }

    // ==================== 工具方法 ====================

    /**
     * 重新加载策略
     */
    public void reloadPolicy() {
        enforcer.loadPolicy();
        hierarchyCache.refreshHierarchy();
        log.info("策略已重新加载");
    }

    /**
     * 保存策略到数据库
     */
    public void savePolicy() {
        enforcer.savePolicy();
        log.info("策略已保存到数据库");
    }
}
