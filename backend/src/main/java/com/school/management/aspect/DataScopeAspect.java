package com.school.management.aspect;

import com.school.management.annotation.DataScope;
import com.school.management.security.CustomUserDetails;
import com.school.management.service.DataPermissionService;
import com.school.management.util.DataPermissionContextHolder;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据权限切面
 * 拦截带有 @DataScope 注解的方法,自动计算并设置数据范围过滤条件
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DataScopeAspect {

    private final DataPermissionService dataPermissionService;

    @Before("@annotation(dataScope)")
    public void doBefore(JoinPoint point, DataScope dataScope) {
        // 清除之前的上下文
        DataPermissionContextHolder.clear();

        CustomUserDetails user = SecurityUtils.getCurrentUserDetails();
        if (user == null) {
            log.debug("No authenticated user, skip data scope");
            return;
        }

        // 获取数据范围 - 使用完全限定名避免与注解冲突
        com.school.management.domain.access.model.DataScope scope = dataPermissionService.getDataScope(dataScope.module());
        log.debug("User {} data scope for module {}: {}", user.getUsername(), dataScope.module(), scope);

        // 全部数据权限,不需要过滤
        if (scope == com.school.management.domain.access.model.DataScope.ALL) {
            return;
        }

        // 构建SQL条件
        StringBuilder sql = new StringBuilder();

        switch (scope) {
            case DEPARTMENT:
                if (StringUtils.hasText(dataScope.deptAlias()) && user.getDepartmentId() != null) {
                    sql.append(String.format(" AND %s.id = %d", dataScope.deptAlias(), user.getDepartmentId()));
                }
                break;

            case GRADE:
                // 本年级:通过班级表过滤
                if (StringUtils.hasText(dataScope.classAlias())) {
                    List<Long> classIds = dataPermissionService.getAccessibleClassIds(dataScope.module());
                    if (classIds != null && !classIds.isEmpty()) {
                        String ids = classIds.stream().map(String::valueOf).collect(Collectors.joining(","));
                        sql.append(String.format(" AND %s.id IN (%s)", dataScope.classAlias(), ids));
                    } else {
                        // 没有可访问的班级,返回空结果
                        sql.append(" AND 1=0");
                    }
                }
                break;

            case CLASS:
                if (StringUtils.hasText(dataScope.classAlias())) {
                    List<Long> classIds = dataPermissionService.getAccessibleClassIds(dataScope.module());
                    if (classIds != null && !classIds.isEmpty()) {
                        String ids = classIds.stream().map(String::valueOf).collect(Collectors.joining(","));
                        sql.append(String.format(" AND %s.id IN (%s)", dataScope.classAlias(), ids));
                    } else {
                        sql.append(" AND 1=0");
                    }
                }
                break;

            case SELF:
                if (StringUtils.hasText(dataScope.userAlias())) {
                    sql.append(String.format(" AND %s.id = %d", dataScope.userAlias(), user.getUserId()));
                } else if (StringUtils.hasText(dataScope.classAlias()) && user.getClassId() != null) {
                    sql.append(String.format(" AND %s.id = %d", dataScope.classAlias(), user.getClassId()));
                }
                break;

            default:
                break;
        }

        if (sql.length() > 0) {
            DataPermissionContextHolder.setDataScopeSql(sql.toString());
            DataPermissionContextHolder.setEnabled(true);
            log.debug("Data scope SQL: {}", sql);
        }
    }

    @After("@annotation(dataScope)")
    public void doAfter(JoinPoint point, DataScope dataScope) {
        // 清除上下文,防止内存泄漏
        DataPermissionContextHolder.clear();
    }
}
