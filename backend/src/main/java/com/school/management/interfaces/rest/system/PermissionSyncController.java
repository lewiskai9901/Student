package com.school.management.interfaces.rest.system;

import com.school.management.common.result.Result;
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
}
