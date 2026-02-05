package com.school.management.infrastructure.access;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * 用户上下文 (V5)
 * 存储当前请求用户的权限相关信息
 */
@Data
@Builder
public class UserContext {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户所属组织单元ID
     */
    private Long orgUnitId;

    /**
     * 用户所属组织单元路径（用于层级查询）
     */
    private String orgUnitPath;

    /**
     * 用户拥有的角色ID列表
     */
    private List<Long> roleIds;

    /**
     * 用户拥有的角色代码列表
     */
    private Set<String> roleCodes;

    /**
     * 是否为超级管理员
     */
    @Builder.Default
    private boolean superAdmin = false;

    /**
     * 判断是否拥有指定角色
     */
    public boolean hasRole(String roleCode) {
        return roleCodes != null && roleCodes.contains(roleCode);
    }

    /**
     * 判断是否拥有任一指定角色
     */
    public boolean hasAnyRole(String... roleCodes) {
        if (this.roleCodes == null) {
            return false;
        }
        for (String code : roleCodes) {
            if (this.roleCodes.contains(code)) {
                return true;
            }
        }
        return false;
    }
}
