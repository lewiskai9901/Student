package com.school.management.security;

import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;

/**
 * 包装 Spring 的 {@link MethodSecurityExpressionOperations} 根对象, 让通配权限 {@code "*"}
 * 在 {@code @PreAuthorize("hasAuthority('X')")} 等表达式中被识别。
 *
 * <p>背景: 超级管理员的 {@code GrantedAuthority} 里带一个 {@code "*"} 通配 (见
 * {@link CustomUserDetailsService}); 前端 hasPermission 与 Casbin 拦截器 (isSuperAdmin)
 * 都识别它, 但 Spring 原生 {@code hasAuthority('X')} 是精确匹配, 不会把 {@code "*"} 当作命中,
 * 导致 21 个用 hasAuthority/hasRole 的端点对超管返回 403。
 *
 * <p>本包装只在原判定失败时追加一条 {@code "*"} 通配检查, 其余全部委托给原根对象 (含
 * filterObject / returnObject / 数据过滤等), 不改变非超管行为。
 */
public class WildcardSecurityExpressionRoot implements MethodSecurityExpressionOperations {

    private final MethodSecurityExpressionOperations delegate;

    public WildcardSecurityExpressionRoot(MethodSecurityExpressionOperations delegate) {
        this.delegate = delegate;
    }

    /** 用户是否持有通配权限 "*"。复用 delegate 的精确匹配, 不直接读 authorities。 */
    private boolean hasWildcard() {
        return delegate.hasAuthority("*");
    }

    // ── 通配增强: 原判定 OR 通配 ──

    @Override
    public boolean hasAuthority(String authority) {
        return delegate.hasAuthority(authority) || hasWildcard();
    }

    @Override
    public boolean hasAnyAuthority(String... authorities) {
        return delegate.hasAnyAuthority(authorities) || hasWildcard();
    }

    @Override
    public boolean hasRole(String role) {
        return delegate.hasRole(role) || hasWildcard();
    }

    @Override
    public boolean hasAnyRole(String... roles) {
        return delegate.hasAnyRole(roles) || hasWildcard();
    }

    @Override
    public boolean hasPermission(Object target, Object permission) {
        return delegate.hasPermission(target, permission) || hasWildcard();
    }

    @Override
    public boolean hasPermission(Object targetId, String targetType, Object permission) {
        return delegate.hasPermission(targetId, targetType, permission) || hasWildcard();
    }

    // ── 纯委托 ──

    @Override public Authentication getAuthentication() { return delegate.getAuthentication(); }
    @Override public boolean permitAll() { return delegate.permitAll(); }
    @Override public boolean denyAll() { return delegate.denyAll(); }
    @Override public boolean isAnonymous() { return delegate.isAnonymous(); }
    @Override public boolean isAuthenticated() { return delegate.isAuthenticated(); }
    @Override public boolean isRememberMe() { return delegate.isRememberMe(); }
    @Override public boolean isFullyAuthenticated() { return delegate.isFullyAuthenticated(); }

    @Override public void setFilterObject(Object filterObject) { delegate.setFilterObject(filterObject); }
    @Override public Object getFilterObject() { return delegate.getFilterObject(); }
    @Override public void setReturnObject(Object returnObject) { delegate.setReturnObject(returnObject); }
    @Override public Object getReturnObject() { return delegate.getReturnObject(); }
    @Override public Object getThis() { return delegate.getThis(); }
}
