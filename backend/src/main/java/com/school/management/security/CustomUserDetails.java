package com.school.management.security;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 自定义用户详情 - 使用基本类型，不依赖任何实体类
 */
@Data
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
    private Long classId;
    private Integer userType;
    private String gradeLevel;

    public CustomUserDetails(Long userId, String username, String password, String realName,
                             Integer status, Long orgUnitId, Long classId, Integer userType,
                             List<String> roles, List<String> permissions) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.realName = realName;
        this.status = status;
        this.orgUnitId = orgUnitId;
        this.classId = classId;
        this.userType = userType;
        this.roles = roles;
        this.permissions = permissions;
    }

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
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
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
}
