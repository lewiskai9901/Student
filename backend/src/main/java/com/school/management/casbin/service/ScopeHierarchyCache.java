package com.school.management.casbin.service;

import com.school.management.casbin.enums.ScopeType;
import com.school.management.entity.Class;
import com.school.management.mapper.ClassMapper;
import com.school.management.mapper.DepartmentMapper;
import com.school.management.mapper.GradeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.casbin.jcasbin.main.Enforcer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 范围层级缓存
 * 用于缓存班级-年级-部门的层级关系，加速权限判断
 *
 * @author system
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "casbin.enabled", havingValue = "true", matchIfMissing = false)
public class ScopeHierarchyCache {

    private final ClassMapper classMapper;
    private final GradeMapper gradeMapper;
    private final DepartmentMapper departmentMapper;

    @Lazy
    @Autowired
    private Enforcer enforcer;

    /**
     * 范围层级关系缓存
     * key: 子范围表达式 (如 scope:class:101)
     * value: 父范围表达式集合 (如 [scope:grade:24, scope:dept:1, scope:dept_grade:1:24])
     */
    private final Map<String, Set<String>> hierarchyCache = new ConcurrentHashMap<>();

    /**
     * 用户范围分配缓存
     * key: userId
     * value: 用户被分配的范围表达式集合
     */
    private final Map<String, Set<String>> userScopeCache = new ConcurrentHashMap<>();

    /**
     * 范围包含关系缓存
     * key: 范围表达式
     * value: 该范围包含的所有子范围集合
     */
    private final Map<String, Set<String>> scopeContainsCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("初始化范围层级缓存...");
        refreshHierarchy();
        log.info("范围层级缓存初始化完成");
    }

    /**
     * 定时刷新缓存 (每5分钟)
     */
    @Scheduled(fixedRate = 300000)
    public void scheduledRefresh() {
        log.debug("定时刷新范围层级缓存...");
        refreshHierarchy();
    }

    /**
     * 刷新层级关系缓存
     */
    public synchronized void refreshHierarchy() {
        try {
            Map<String, Set<String>> newHierarchy = new ConcurrentHashMap<>();
            Map<String, Set<String>> newContains = new ConcurrentHashMap<>();

            // 查询所有班级及其关联的年级和部门
            List<Class> classes = classMapper.selectList(null);
            for (Class cls : classes) {
                if (cls.getId() == null || cls.getDeleted() != null && cls.getDeleted() == 1) {
                    continue;
                }

                String classScope = ScopeType.CLASS.buildExpression(cls.getId());
                Set<String> parents = new HashSet<>();

                // 添加年级父范围
                if (cls.getGradeId() != null) {
                    String gradeScope = ScopeType.GRADE.buildExpression(cls.getGradeId());
                    parents.add(gradeScope);

                    // 更新年级的包含关系
                    scopeContainsCache.computeIfAbsent(gradeScope, k -> ConcurrentHashMap.newKeySet())
                            .add(classScope);
                }

                // 添加部门父范围
                if (cls.getOrgUnitId() != null) {
                    String deptScope = ScopeType.DEPT.buildExpression(cls.getOrgUnitId());
                    parents.add(deptScope);

                    // 更新部门的包含关系
                    scopeContainsCache.computeIfAbsent(deptScope, k -> ConcurrentHashMap.newKeySet())
                            .add(classScope);
                }

                // 添加部门+年级交叉范围
                if (cls.getOrgUnitId() != null && cls.getGradeId() != null) {
                    String deptGradeScope = ScopeType.DEPT_GRADE.buildExpression(
                            cls.getOrgUnitId(), cls.getGradeId());
                    parents.add(deptGradeScope);

                    // 更新部门+年级的包含关系
                    scopeContainsCache.computeIfAbsent(deptGradeScope, k -> ConcurrentHashMap.newKeySet())
                            .add(classScope);
                }

                // 全局范围包含所有
                parents.add("scope:*");

                newHierarchy.put(classScope, parents);
            }

            // 更新缓存
            hierarchyCache.clear();
            hierarchyCache.putAll(newHierarchy);

            // 刷新用户范围缓存
            refreshUserScopes();

            log.info("范围层级缓存刷新完成，共 {} 个班级", classes.size());
        } catch (Exception e) {
            log.error("刷新范围层级缓存失败", e);
        }
    }

    /**
     * 刷新用户范围分配缓存
     */
    private void refreshUserScopes() {
        if (enforcer == null) {
            return;
        }

        try {
            Map<String, Set<String>> newUserScopes = new ConcurrentHashMap<>();

            // 从 Casbin g2 规则中获取用户范围分配
            List<List<String>> g2Rules = enforcer.getNamedGroupingPolicy("g2");
            for (List<String> rule : g2Rules) {
                if (rule.size() >= 2) {
                    String userKey = rule.get(0); // user:1
                    String scope = rule.get(1);   // scope:class:101

                    newUserScopes.computeIfAbsent(userKey, k -> ConcurrentHashMap.newKeySet())
                            .add(scope);
                }
            }

            userScopeCache.clear();
            userScopeCache.putAll(newUserScopes);

            log.debug("用户范围缓存刷新完成，共 {} 个用户", newUserScopes.size());
        } catch (Exception e) {
            log.error("刷新用户范围缓存失败", e);
        }
    }

    /**
     * 判断子范围是否属于父范围
     *
     * @param child  子范围表达式
     * @param parent 父范围表达式
     * @return 是否属于
     */
    public boolean isChildOf(String child, String parent) {
        if (child == null || parent == null) {
            return false;
        }
        if ("scope:*".equals(parent)) {
            return true;
        }
        if (child.equals(parent)) {
            return true;
        }

        Set<String> parents = hierarchyCache.get(child);
        return parents != null && parents.contains(parent);
    }

    /**
     * 获取范围的所有父范围
     *
     * @param scope 范围表达式
     * @return 父范围集合
     */
    public Set<String> getParents(String scope) {
        return hierarchyCache.getOrDefault(scope, Collections.emptySet());
    }

    /**
     * 检查用户是否拥有指定范围
     *
     * @param userId 用户标识 (如 "user:1")
     * @param scope  范围表达式
     * @return 是否拥有
     */
    public boolean hasUserScope(String userId, String scope) {
        Set<String> scopes = userScopeCache.get(userId);
        return scopes != null && scopes.contains(scope);
    }

    /**
     * 获取用户所有分配的范围
     *
     * @param userId 用户标识
     * @return 范围集合
     */
    public Set<String> getUserScopes(String userId) {
        return userScopeCache.getOrDefault(userId, Collections.emptySet());
    }

    /**
     * 判断一个范围是否包含另一个范围
     *
     * @param container 容器范围
     * @param contained 被包含范围
     * @return 是否包含
     */
    public boolean isScopeContained(String container, String contained) {
        if (container == null || contained == null) {
            return false;
        }
        if ("scope:*".equals(container)) {
            return true;
        }
        if (container.equals(contained)) {
            return true;
        }

        Set<String> children = scopeContainsCache.get(container);
        return children != null && children.contains(contained);
    }

    /**
     * 获取范围包含的所有子范围
     *
     * @param scope 范围表达式
     * @return 子范围集合
     */
    public Set<String> getContainedScopes(String scope) {
        return scopeContainsCache.getOrDefault(scope, Collections.emptySet());
    }

    /**
     * 手动刷新指定用户的范围缓存
     *
     * @param userId 用户ID
     */
    public void refreshUserScope(Long userId) {
        if (enforcer == null || userId == null) {
            return;
        }

        String userKey = "user:" + userId;
        Set<String> scopes = ConcurrentHashMap.newKeySet();

        List<List<String>> g2Rules = enforcer.getFilteredNamedGroupingPolicy("g2", 0, userKey);
        for (List<String> rule : g2Rules) {
            if (rule.size() >= 2) {
                scopes.add(rule.get(1));
            }
        }

        userScopeCache.put(userKey, scopes);
        log.debug("刷新用户 {} 范围缓存完成", userId);
    }

    /**
     * 清除指定用户的范围缓存
     *
     * @param userId 用户ID
     */
    public void evictUserScope(Long userId) {
        String userKey = "user:" + userId;
        userScopeCache.remove(userKey);
        log.debug("清除用户 {} 范围缓存", userId);
    }
}
