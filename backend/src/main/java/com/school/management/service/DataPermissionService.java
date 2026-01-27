package com.school.management.service;

import com.school.management.casbin.model.DataScope;

import java.util.List;

/**
 * 数据权限服务接口
 */
public interface DataPermissionService {

    /**
     * 获取当前用户对指定模块的数据范围
     * @param moduleCode 模块编码
     * @return 数据范围枚举
     */
    DataScope getDataScope(String moduleCode);

    /**
     * 获取当前用户可访问的班级ID列表
     * @param moduleCode 模块编码
     * @return 班级ID列表，null表示不限制
     */
    List<Long> getAccessibleClassIds(String moduleCode);

    /**
     * 获取当前用户可访问的部门ID列表
     * @param moduleCode 模块编码
     * @return 部门ID列表，null表示不限制
     */
    List<Long> getAccessibleDepartmentIds(String moduleCode);

    /**
     * 检查当前用户是否可以访问指定班级
     * @param classId 班级ID
     * @param moduleCode 模块编码
     * @return 是否可访问
     */
    boolean canAccessClass(Long classId, String moduleCode);

    /**
     * 检查当前用户是否可以访问指定学生
     * @param studentId 学生ID
     * @param moduleCode 模块编码
     * @return 是否可访问
     */
    boolean canAccessStudent(Long studentId, String moduleCode);

    /**
     * 获取当前用户对指定模块是否有全部数据权限
     * @param moduleCode 模块编码
     * @return 是否有全部权限
     */
    boolean hasAllDataPermission(String moduleCode);
}
