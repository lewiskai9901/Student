package com.school.management.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.school.management.infrastructure.access.UserContext;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 自定义用户详情 - 使用基本类型，不依赖任何实体类
 */
public class CustomUserDetails implements UserDetails {

    private Long userId;
    private String username;
    private String password;
    private String realName;
    private Integer status;
    private List<String> roles;
    private List<String> permissions;

    // 数据权限相关字段
    private Long orgUnitId;
    private String orgUnitPath;
    private List<Long> roleIds;
    private Long tenantId;
    private List<UserContext.ScopedRoleInfo> scopedRoles;

    public CustomUserDetails() {
    }

    public CustomUserDetails(Long userId, String username, String password, String realName,
                             Integer status, Long orgUnitId, Long tenantId,
                             List<String> roles, List<String> permissions) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.realName = realName;
        this.status = status;
        this.orgUnitId = orgUnitId;
        this.tenantId = tenantId;
        this.roles = roles;
        this.permissions = permissions;
    }

    // Getters and Setters

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    public Long getOrgUnitId() {
        return orgUnitId;
    }

    public void setOrgUnitId(Long orgUnitId) {
        this.orgUnitId = orgUnitId;
    }

    public String getOrgUnitPath() {
        return orgUnitPath;
    }

    public void setOrgUnitPath(String orgUnitPath) {
        this.orgUnitPath = orgUnitPath;
    }

    public List<Long> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<Long> roleIds) {
        this.roleIds = roleIds;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public List<UserContext.ScopedRoleInfo> getScopedRoles() {
        return scopedRoles != null ? scopedRoles : Collections.emptyList();
    }

    public void setScopedRoles(List<UserContext.ScopedRoleInfo> scopedRoles) {
        this.scopedRoles = scopedRoles;
    }

    // UserDetails interface implementations

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());

        List<GrantedAuthority> permissionAuthorities = permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        authorities.addAll(permissionAuthorities);
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != null && status == 1;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status != null && status == 1;
    }

    // Utility methods

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    public Long getId() {
        return userId;
    }

    public String getOrgUnitName() {
        return "";
    }

    // equals, hashCode, and toString

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomUserDetails that = (CustomUserDetails) o;
        return userId != null && userId.equals(that.userId);
    }

    @Override
    public int hashCode() {
        return userId != null ? userId.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "CustomUserDetails{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", realName='" + realName + '\'' +
                ", status=" + status +
                ", orgUnitId=" + orgUnitId +
                ", tenantId=" + tenantId +
                ", roles=" + roles +
                ", permissions=" + permissions +
                '}';
    }
}
