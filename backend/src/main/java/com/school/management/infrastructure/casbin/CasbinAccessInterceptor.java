package com.school.management.infrastructure.casbin;

import com.school.management.domain.access.model.PermissionScope;
import com.school.management.infrastructure.access.UserContext;
import com.school.management.infrastructure.access.UserContextHolder;
import com.school.management.infrastructure.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.casbin.jcasbin.main.Enforcer;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class CasbinAccessInterceptor {

    private final Enforcer enforcer;

    @Around("@annotation(com.school.management.infrastructure.casbin.CasbinAccess)")
    public Object checkAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        CasbinAccess annotation = method.getAnnotation(CasbinAccess.class);

        UserContext userContext = UserContextHolder.getContext();
        if (userContext == null) {
            throw new AccessDeniedException("User context not available");
        }

        // Super admin bypasses all checks
        if (userContext.isSuperAdmin()) {
            return joinPoint.proceed();
        }

        // PUBLIC scope: authentication is enough, no role gate
        if (annotation.scope() == PermissionScope.PUBLIC) {
            return joinPoint.proceed();
        }

        // SELF scope: authentication is enough. Data filtering (user_id = currentUserId())
        // is enforced inside the endpoint via SecurityUtils.requireCurrentUserId();
        // ArchUnitMyEndpointTest guarantees endpoints cannot accept identity params.
        // The permission engine would add no real gate here — a teacher can always see their own data.
        if (annotation.scope() == PermissionScope.SELF) {
            return joinPoint.proceed();
        }

        String userId = String.valueOf(userContext.getUserId());
        String tenantId = String.valueOf(TenantContextHolder.getTenantId());
        String resource = annotation.resource();
        String action = annotation.action();

        boolean allowed = enforcer.enforce(userId, tenantId, resource, action);

        if (!allowed) {
            log.warn("Access denied: user={}, tenant={}, resource={}, action={}, scope={}",
                    userId, tenantId, resource, action, annotation.scope());
            throw new AccessDeniedException(
                    "Access denied for resource '" + resource + "' action '" + action + "'");
        }

        log.debug("Access granted: user={}, tenant={}, resource={}, action={}, scope={}",
                userId, tenantId, resource, action, annotation.scope());
        return joinPoint.proceed();
    }
}
