package com.school.management.infrastructure.casbin;

import com.school.management.domain.access.model.PermissionScope;
import com.school.management.infrastructure.access.UserContext;
import com.school.management.infrastructure.access.UserContextHolder;
import com.school.management.infrastructure.tenant.TenantContextHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.casbin.jcasbin.main.Enforcer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 {@link CasbinAccessInterceptor} 的 scope 短路语义。
 *
 * <p>关键不变量:
 * <ul>
 *   <li>super-admin 绕过全部检查 (包括 Casbin.enforce)</li>
 *   <li>PUBLIC / SELF 短路,不调用 Enforcer — 数据隔离由端点层保证</li>
 *   <li>MANAGEMENT 正常走 Enforcer.enforce,拒绝时抛 {@link AccessDeniedException}</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class CasbinAccessInterceptorTest {

    @Mock
    private Enforcer enforcer;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature signature;

    private CasbinAccessInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new CasbinAccessInterceptor(enforcer);
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("无 UserContext → AccessDeniedException")
    void noUserContext_throws() throws Exception {
        setUpAnnotation("system:user", "view", PermissionScope.MANAGEMENT);
        UserContextHolder.clear();
        assertThatThrownBy(() -> interceptor.checkAccess(joinPoint))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("super-admin 绕过 — 不调用 enforcer,直接 proceed")
    void superAdmin_bypassesWithoutEnforce() throws Throwable {
        setUpAnnotation("system:user", "view", PermissionScope.MANAGEMENT);
        UserContextHolder.setContext(UserContext.builder()
                .userId(1L).superAdmin(true).build());
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = interceptor.checkAccess(joinPoint);

        assertThat(result).isEqualTo("ok");
        verify(enforcer, never()).enforce(any(), any(), any(), any());
    }

    @Test
    @DisplayName("PUBLIC scope 短路 — 已认证即放行,不走 enforcer")
    void publicScope_shortCircuits() throws Throwable {
        setUpAnnotation("my", "ping", PermissionScope.PUBLIC);
        UserContextHolder.setContext(UserContext.builder()
                .userId(100L).superAdmin(false).build());
        when(joinPoint.proceed()).thenReturn("public-ok");

        Object result = interceptor.checkAccess(joinPoint);

        assertThat(result).isEqualTo("public-ok");
        verify(enforcer, never()).enforce(any(), any(), any(), any());
    }

    @Test
    @DisplayName("SELF scope 短路 — 数据隔离由端点层保证,不走 enforcer")
    void selfScope_shortCircuits() throws Throwable {
        setUpAnnotation("my:schedule", "view", PermissionScope.SELF);
        UserContextHolder.setContext(UserContext.builder()
                .userId(100L).superAdmin(false).build());
        when(joinPoint.proceed()).thenReturn("self-ok");

        Object result = interceptor.checkAccess(joinPoint);

        assertThat(result).isEqualTo("self-ok");
        verify(enforcer, never()).enforce(any(), any(), any(), any());
    }

    @Test
    @DisplayName("MANAGEMENT scope + enforcer 放行 → proceed")
    void managementScope_enforcerAllows_proceeds() throws Throwable {
        setUpAnnotation("system:user", "view", PermissionScope.MANAGEMENT);
        UserContextHolder.setContext(UserContext.builder()
                .userId(100L).superAdmin(false).build());
        when(enforcer.enforce("100", "1", "system:user", "view")).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("mgmt-ok");

        Object result = interceptor.checkAccess(joinPoint);

        assertThat(result).isEqualTo("mgmt-ok");
        verify(enforcer).enforce("100", "1", "system:user", "view");
    }

    @Test
    @DisplayName("MANAGEMENT scope + enforcer 拒绝 → AccessDeniedException")
    void managementScope_enforcerDenies_throws() throws Throwable {
        setUpAnnotation("system:user", "delete", PermissionScope.MANAGEMENT);
        UserContextHolder.setContext(UserContext.builder()
                .userId(100L).superAdmin(false).build());
        when(enforcer.enforce(eq("100"), eq("1"), eq("system:user"), eq("delete")))
                .thenReturn(false);

        assertThatThrownBy(() -> interceptor.checkAccess(joinPoint))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("system:user")
                .hasMessageContaining("delete");

        verify(joinPoint, never()).proceed();
    }

    // ---- helpers ----

    private void setUpAnnotation(String resource, String action, PermissionScope scope) throws Exception {
        Method method = switch (scope) {
            case PUBLIC -> TestTarget.class.getMethod("publicMethod");
            case SELF -> TestTarget.class.getMethod("selfMethod");
            case MANAGEMENT -> action.equals("delete")
                    ? TestTarget.class.getMethod("mgmtDelete")
                    : TestTarget.class.getMethod("mgmtView");
        };
        // Sanity: annotation values should match the test's inputs
        CasbinAccess ann = method.getAnnotation(CasbinAccess.class);
        assertThat(ann.resource()).isEqualTo(resource);
        assertThat(ann.action()).isEqualTo(action);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
    }

    /** Fixture: carries the @CasbinAccess annotations for each scope variant. */
    static class TestTarget {
        @CasbinAccess(resource = "my", action = "ping", scope = PermissionScope.PUBLIC)
        public void publicMethod() {}

        @CasbinAccess(resource = "my:schedule", action = "view", scope = PermissionScope.SELF)
        public void selfMethod() {}

        @CasbinAccess(resource = "system:user", action = "view", scope = PermissionScope.MANAGEMENT)
        public void mgmtView() {}

        @CasbinAccess(resource = "system:user", action = "delete", scope = PermissionScope.MANAGEMENT)
        public void mgmtDelete() {}
    }
}
