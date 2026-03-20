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
}
