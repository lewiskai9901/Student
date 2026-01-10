package com.school.management.aspect;

import com.school.management.annotation.DataPermission;
import com.school.management.domain.access.model.DataScope;
import com.school.management.security.CustomUserDetails;
import com.school.management.service.DataPermissionService;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/**
 * 数据权限AOP切面
 * 自动根据当前用户权限修改查询请求中的过滤条件
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DataPermissionAspect {

    private final DataPermissionService dataPermissionService;

    @Around("@annotation(com.school.management.annotation.DataPermission)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        // 获取方法上的注解
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        DataPermission dataPermission = method.getAnnotation(DataPermission.class);

        if (dataPermission == null) {
            return point.proceed();
        }

        String moduleCode = dataPermission.module();
        String classField = dataPermission.classField();
        String deptField = dataPermission.deptField();

        // 获取当前用户
        CustomUserDetails userDetails = SecurityUtils.getCurrentUserDetails();
        if (userDetails == null) {
            log.debug("用户未登录，跳过数据权限过滤");
            return point.proceed();
        }

        // 获取数据范围
        DataScope dataScope = dataPermissionService.getDataScope(moduleCode);
        log.debug("用户 {} 对模块 {} 的数据权限范围: {}", userDetails.getUsername(), moduleCode, dataScope);

        // 如果是全部数据权限，直接执行
        if (dataScope == DataScope.ALL) {
            return point.proceed();
        }

        // 修改方法参数中的QueryRequest
        Object[] args = point.getArgs();
        for (Object arg : args) {
            if (arg != null) {
                applyDataPermission(arg, dataScope, moduleCode, classField, deptField, userDetails);
            }
        }

        return point.proceed(args);
    }

    /**
     * 应用数据权限过滤
     */
    private void applyDataPermission(Object request, DataScope dataScope, String moduleCode,
                                      String classField, String deptField, CustomUserDetails userDetails) {
        try {
            switch (dataScope) {
                case DEPARTMENT:
                    // 本部门数据
                    setFieldValue(request, deptField,
                            userDetails.getDepartmentId() != null
                                ? Collections.singletonList(userDetails.getDepartmentId())
                                : Collections.emptyList());
                    break;

                case GRADE:
                    // 本年级数据 - 通过班级过滤
                    List<Long> gradeClassIds = dataPermissionService.getAccessibleClassIds(moduleCode);
                    if (gradeClassIds != null) {
                        setFieldValue(request, classField, gradeClassIds);
                    }
                    break;

                case CLASS:
                    // 本班级数据
                    List<Long> classIds = dataPermissionService.getAccessibleClassIds(moduleCode);
                    if (classIds != null) {
                        setFieldValue(request, classField, classIds);
                    }
                    break;

                case SELF:
                    // 仅本人数据
                    setFieldValue(request, "selfUserId", userDetails.getUserId());
                    // 如果学生有所属班级，也设置班级过滤
                    if (userDetails.getClassId() != null) {
                        setFieldValue(request, classField, Collections.singletonList(userDetails.getClassId()));
                    }
                    break;

                default:
                    break;
            }
        } catch (Exception e) {
            log.warn("应用数据权限过滤失败: {}", e.getMessage());
        }
    }

    /**
     * 通过反射设置字段值
     */
    private void setFieldValue(Object object, String fieldName, Object value) {
        try {
            Field field = findField(object.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                field.set(object, value);
                log.debug("设置数据权限过滤字段 {} = {}", fieldName, value);
            }
        } catch (Exception e) {
            log.debug("设置字段 {} 失败: {}", fieldName, e.getMessage());
        }
    }

    /**
     * 递归查找字段(包括父类)
     */
    private Field findField(Class<?> clazz, String fieldName) {
        if (clazz == null || clazz == Object.class) {
            return null;
        }
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            return findField(clazz.getSuperclass(), fieldName);
        }
    }
}
