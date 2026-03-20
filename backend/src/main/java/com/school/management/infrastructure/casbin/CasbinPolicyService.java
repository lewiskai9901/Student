package com.school.management.infrastructure.casbin;

import com.school.management.domain.access.service.PolicyEnforcementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.casbin.jcasbin.main.Enforcer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Casbin-based implementation of PolicyEnforcementService.
 * Lives in infrastructure layer as it depends on third-party Casbin library.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CasbinPolicyService implements PolicyEnforcementService {

    private final Enforcer enforcer;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void assignRole(Long userId, String roleCode, Long tenantId) {
        String sub = String.valueOf(userId);
        String dom = String.valueOf(tenantId);
        enforcer.addGroupingPolicy(sub, roleCode, dom);
        enforcer.savePolicy();
        log.info("Assigned role {} to user {} in tenant {}", roleCode, userId, tenantId);
    }

    @Override
    public void removeRole(Long userId, String roleCode, Long tenantId) {
        String sub = String.valueOf(userId);
        String dom = String.valueOf(tenantId);
        enforcer.removeGroupingPolicy(sub, roleCode, dom);
        enforcer.savePolicy();
        log.info("Removed role {} from user {} in tenant {}", roleCode, userId, tenantId);
    }

    @Override
    public void grantPermission(String roleCode, Long tenantId, String resource, String action) {
        String dom = String.valueOf(tenantId);
        enforcer.addPolicy(roleCode, dom, resource, action);
        enforcer.savePolicy();
        log.info("Granted {}.{} to role {} in tenant {}", resource, action, roleCode, tenantId);
    }

    @Override
    public void revokePermission(String roleCode, Long tenantId, String resource, String action) {
        String dom = String.valueOf(tenantId);
        enforcer.removePolicy(roleCode, dom, resource, action);
        enforcer.savePolicy();
        log.info("Revoked {}.{} from role {} in tenant {}", resource, action, roleCode, tenantId);
    }

    @Override
    @Transactional
    public void syncFromDatabase() {
        log.info("Starting Casbin policy sync from database...");

        enforcer.clearPolicy();

        // 1. Sync role-permission mappings (p policies)
        String rpSql = "SELECT r.role_code, r.tenant_id, p.permission_code " +
                "FROM role_permissions rp " +
                "JOIN roles r ON r.id = rp.role_id " +
                "JOIN permissions p ON p.id = rp.permission_id " +
                "WHERE r.status = 1 AND r.deleted = 0";

        List<Map<String, Object>> rpRows = jdbcTemplate.queryForList(rpSql);
        int pCount = 0;
        for (Map<String, Object> row : rpRows) {
            String roleCode = (String) row.get("role_code");
            String tenantIdStr = String.valueOf(row.get("tenant_id"));
            String permCode = (String) row.get("permission_code");

            String resource;
            String action;
            if (permCode.contains(":")) {
                String[] parts = permCode.split(":", 2);
                resource = parts[0];
                action = parts[1];
            } else {
                resource = permCode;
                action = "*";
            }

            enforcer.addPolicy(roleCode, tenantIdStr, resource, action);
            pCount++;
        }

        // 2. Sync user-role mappings (g policies)
        String urSql = "SELECT ur.user_id, r.role_code, r.tenant_id " +
                "FROM user_roles ur " +
                "JOIN roles r ON r.id = ur.role_id " +
                "WHERE r.status = 1 AND r.deleted = 0";

        List<Map<String, Object>> urRows = jdbcTemplate.queryForList(urSql);
        int gCount = 0;
        for (Map<String, Object> row : urRows) {
            String userIdStr = String.valueOf(row.get("user_id"));
            String roleCode = (String) row.get("role_code");
            String tenantIdStr = String.valueOf(row.get("tenant_id"));

            enforcer.addGroupingPolicy(userIdStr, roleCode, tenantIdStr);
            gCount++;
        }

        enforcer.savePolicy();
        log.info("Casbin policy sync complete: {} p-policies, {} g-policies", pCount, gCount);
    }

    @Override
    public boolean checkAccess(Long userId, Long tenantId, String resource, String action) {
        return enforcer.enforce(
                String.valueOf(userId),
                String.valueOf(tenantId),
                resource,
                action
        );
    }

    @Override
    public List<List<String>> getUserPermissions(Long userId, Long tenantId) {
        return enforcer.getPermissionsForUserInDomain(
                String.valueOf(userId),
                String.valueOf(tenantId)
        );
    }

    @Override
    public List<String> getUserRoles(Long userId, Long tenantId) {
        return enforcer.getRolesForUserInDomain(
                String.valueOf(userId),
                String.valueOf(tenantId)
        );
    }
}
