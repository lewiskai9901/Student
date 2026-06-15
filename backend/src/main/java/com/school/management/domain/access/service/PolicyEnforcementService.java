package com.school.management.domain.access.service;

import java.util.List;

/**
 * Domain service interface for policy enforcement.
 * Infrastructure layer (Casbin) provides the implementation.
 */
public interface PolicyEnforcementService {

    void assignRole(Long userId, String roleCode, Long tenantId);

    void removeRole(Long userId, String roleCode, Long tenantId);

    void grantPermission(String roleCode, Long tenantId, String resource, String action);

    void revokePermission(String roleCode, Long tenantId, String resource, String action);

    boolean checkAccess(Long userId, Long tenantId, String resource, String action);

    List<List<String>> getUserPermissions(Long userId, Long tenantId);

    List<String> getUserRoles(Long userId, Long tenantId);

    void syncFromDatabase();

    /**
     * 热同步单个用户的 grouping policy（g 策略）到内存 enforcer。
     *
     * <p>实现应先清掉该用户现存全部 g 策略，再按 {@code user_roles} 当前行重建 ——
     * 一次性覆盖新增/移除/替换，幂等。用于"新建用户带角色 / 改角色 / 删用户"后
     * 让 {@code @CasbinAccess} 鉴权立即生效，无需重启。
     */
    void syncUserRoles(Long userId);
}
