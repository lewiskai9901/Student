package com.school.management.casbin.service;

import com.school.management.casbin.dto.ScopeAssignmentDTO;
import com.school.management.entity.Student;
import com.school.management.enums.DataScope;
import com.school.management.mapper.StudentMapper;
import com.school.management.security.CustomUserDetails;
import com.school.management.service.DataPermissionService;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于 Casbin 的数据权限服务实现
 *
 * 使用 Casbin 策略引擎管理数据权限:
 * - 范围表达式: scope:*, scope:dept:{id}, scope:grade:{id}, scope:class:{id}, scope:self
 * - 支持层级继承: 部门包含年级,年级包含班级
 *
 * @author system
 * @version 4.0.0
 */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
@ConditionalOnProperty(name = "casbin.enabled", havingValue = "true", matchIfMissing = false)
public class CasbinDataPermissionService implements DataPermissionService {

    private final CasbinEnforcerService enforcerService;
    private final CasbinScopeService scopeService;
    private final ScopeHierarchyCache hierarchyCache;
    private final StudentMapper studentMapper;

    @Override
    public DataScope getDataScope(String moduleCode) {
        CustomUserDetails userDetails = SecurityUtils.getCurrentUserDetails();
        if (userDetails == null) {
            return DataScope.SELF;
        }

        Long userId = userDetails.getUserId();
        List<ScopeAssignmentDTO> scopes = scopeService.getUserScopes(userId);

        if (scopes == null || scopes.isEmpty()) {
            return DataScope.SELF;
        }

        // 检查是否有全局权限
        boolean hasAllScope = scopes.stream()
                .anyMatch(s -> "scope:*".equals(s.getScopeExpression()));
        if (hasAllScope) {
            return DataScope.ALL;
        }

        // 检查范围类型
        for (ScopeAssignmentDTO scope : scopes) {
            String type = scope.getScopeType();
            if ("ALL".equals(type)) {
                return DataScope.ALL;
            }
        }

        // 有其他范围配置
        return DataScope.DEPARTMENT;
    }

    @Override
    public List<Long> getAccessibleClassIds(String moduleCode) {
        CustomUserDetails userDetails = SecurityUtils.getCurrentUserDetails();
        if (userDetails == null) {
            return Collections.emptyList();
        }

        Long userId = userDetails.getUserId();

        // 使用 Casbin EnforcerService 获取可访问的班级
        Set<Long> classIds = enforcerService.getAccessibleClassIds(userId);

        if (classIds == null) {
            return null; // null 表示不限制(全部权限)
        }

        if (classIds.isEmpty()) {
            // 没有配置数据范围,检查用户自己的班级
            if (userDetails.getClassId() != null) {
                return Collections.singletonList(userDetails.getClassId());
            }
            return Collections.emptyList();
        }

        return new ArrayList<>(classIds);
    }

    @Override
    public List<Long> getAccessibleDepartmentIds(String moduleCode) {
        CustomUserDetails userDetails = SecurityUtils.getCurrentUserDetails();
        if (userDetails == null) {
            return Collections.emptyList();
        }

        Long userId = userDetails.getUserId();
        List<ScopeAssignmentDTO> scopes = scopeService.getUserScopes(userId);

        if (scopes == null || scopes.isEmpty()) {
            // 没有配置数据范围,返回用户所属部门
            if (userDetails.getDepartmentId() != null) {
                return Collections.singletonList(userDetails.getDepartmentId());
            }
            return Collections.emptyList();
        }

        // 检查是否有全局权限
        boolean hasAllScope = scopes.stream()
                .anyMatch(s -> "scope:*".equals(s.getScopeExpression()) || "ALL".equals(s.getScopeType()));
        if (hasAllScope) {
            return null; // null 表示不限制
        }

        // 收集部门ID
        Set<Long> deptIds = new HashSet<>();
        for (ScopeAssignmentDTO scope : scopes) {
            String expr = scope.getScopeExpression();
            if (expr.startsWith("scope:dept:")) {
                try {
                    Long deptId = Long.parseLong(expr.replace("scope:dept:", ""));
                    deptIds.add(deptId);
                } catch (NumberFormatException e) {
                    log.warn("Invalid department scope expression: {}", expr);
                }
            } else if (expr.startsWith("scope:dept_grade:")) {
                // 提取部门ID (格式: scope:dept_grade:{deptId}:{gradeId})
                String[] parts = expr.replace("scope:dept_grade:", "").split(":");
                if (parts.length >= 1) {
                    try {
                        deptIds.add(Long.parseLong(parts[0]));
                    } catch (NumberFormatException e) {
                        log.warn("Invalid dept_grade scope expression: {}", expr);
                    }
                }
            }
        }

        if (deptIds.isEmpty()) {
            if (userDetails.getDepartmentId() != null) {
                return Collections.singletonList(userDetails.getDepartmentId());
            }
            return Collections.emptyList();
        }

        return new ArrayList<>(deptIds);
    }

    @Override
    public boolean canAccessClass(Long classId, String moduleCode) {
        if (classId == null) {
            return true;
        }

        CustomUserDetails userDetails = SecurityUtils.getCurrentUserDetails();
        if (userDetails == null) {
            return false;
        }

        Long userId = userDetails.getUserId();

        // 使用 Casbin 检查权限
        String scopeExpression = "scope:class:" + classId;
        return enforcerService.checkPermission(userId, scopeExpression, "class", "read");
    }

    @Override
    public boolean canAccessStudent(Long studentId, String moduleCode) {
        if (studentId == null) {
            return true;
        }

        CustomUserDetails userDetails = SecurityUtils.getCurrentUserDetails();
        if (userDetails == null) {
            return false;
        }

        // 获取学生信息
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            return false;
        }

        // 检查是否是学生本人
        if (student.getUserId() != null && student.getUserId().equals(userDetails.getUserId())) {
            return true;
        }

        // 通过班级判断
        return canAccessClass(student.getClassId(), moduleCode);
    }

    @Override
    public boolean hasAllDataPermission(String moduleCode) {
        CustomUserDetails userDetails = SecurityUtils.getCurrentUserDetails();
        if (userDetails == null) {
            return false;
        }

        Long userId = userDetails.getUserId();
        List<ScopeAssignmentDTO> scopes = scopeService.getUserScopes(userId);

        if (scopes == null || scopes.isEmpty()) {
            return false;
        }

        // 检查是否有全局权限
        return scopes.stream()
                .anyMatch(s -> "scope:*".equals(s.getScopeExpression()) || "ALL".equals(s.getScopeType()));
    }
}
