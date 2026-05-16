package com.school.management.interfaces.rest.system;

import com.school.management.common.result.Result;
import com.school.management.domain.access.service.PolicyEnforcementService;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Permission sync check utility.
 * Compares @CasbinAccess annotations in code with permission_code entries in the permissions table.
 */
@RestController
@RequestMapping("/system/permission-sync")
@RequiredArgsConstructor
public class PermissionSyncController {

    private final JdbcTemplate jdbcTemplate;
    private final RequestMappingHandlerMapping requestMappingHandlerMapping;
    private final PolicyEnforcementService policyEnforcementService;

    /**
     * Scan all @CasbinAccess annotations at runtime and compare with the permissions table.
     * Returns which resource:action pairs exist in code but not in DB, and vice versa.
     */
    @GetMapping("/check")
    @CasbinAccess(resource = "system:permission", action = "view")
    public Result<Map<String, Object>> checkSync() {
        // 1. Scan all @CasbinAccess annotations from registered controller methods
        Set<String> codePairs = new TreeSet<>();
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = requestMappingHandlerMapping.getHandlerMethods();
        for (HandlerMethod handlerMethod : handlerMethods.values()) {
            CasbinAccess annotation = handlerMethod.getMethodAnnotation(CasbinAccess.class);
            if (annotation != null) {
                codePairs.add(annotation.resource() + ":" + annotation.action());
            }
        }

        // 2. Get all permission_code values from DB
        List<String> dbCodes = jdbcTemplate.queryForList(
                "SELECT permission_code FROM permissions WHERE deleted = 0", String.class);
        Set<String> dbCodeSet = new TreeSet<>(dbCodes);

        // 3. Find resource:action pairs in code but not matching any DB permission_code
        List<String> missingInDb = codePairs.stream()
                .filter(code -> !dbCodeSet.contains(code))
                .sorted()
                .collect(Collectors.toList());

        // 4. Find DB permission_codes that no code annotation references
        //    (compare DB codes against the set of resource prefixes and exact codes from annotations)
        List<String> potentiallyObsolete = dbCodeSet.stream()
                .filter(dbCode -> codePairs.stream().noneMatch(cp ->
                        cp.equals(dbCode) || cp.startsWith(dbCode + ":")))
                .sorted()
                .collect(Collectors.toList());

        // 5. Build grouped view of code pairs by resource
        Map<String, List<String>> codeByResource = new TreeMap<>();
        for (String pair : codePairs) {
            int lastColon = pair.lastIndexOf(':');
            String resource = pair.substring(0, lastColon);
            String action = pair.substring(lastColon + 1);
            codeByResource.computeIfAbsent(resource, k -> new ArrayList<>()).add(action);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("codeAnnotationCount", codePairs.size());
        result.put("dbPermissionCount", dbCodes.size());
        result.put("missingInDb", missingInDb);
        result.put("missingInDbCount", missingInDb.size());
        result.put("potentiallyObsoleteInDb", potentiallyObsolete);
        result.put("potentiallyObsoleteCount", potentiallyObsolete.size());
        result.put("codeResourceActions", codeByResource);
        return Result.success(result);
    }

    /**
     * 手动触发 Casbin enforcer 重载 role_permissions + user_roles → 内存策略.
     *
     * <p>使用场景:
     * <ul>
     *   <li>SQL 直接修改 role_permissions / user_roles 后(如 migration), 需要让
     *       内存 enforcer 与 DB 一致</li>
     *   <li>通过 admin UI 改了大量角色绑定后, 一次性刷新, 比逐条 in-memory 调用快</li>
     *   <li>排障: 怀疑 enforcer 状态漂了, 强制 reset</li>
     * </ul>
     *
     * <p>注:正常业务路径走 PermissionsRefreshedEvent + AccessApplicationService 的
     * 单条 in-memory 调用, 不需要走此端点. 此端点是"管理员保险栓".
     */
    @PostMapping("/refresh-casbin")
    @CasbinAccess(resource = "system:permission", action = "manage")
    public Result<Map<String, Object>> refreshCasbin() {
        policyEnforcementService.syncFromDatabase();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "ok");
        result.put("message", "Casbin enforcer reloaded from role_permissions + user_roles");
        return Result.success(result);
    }
}
