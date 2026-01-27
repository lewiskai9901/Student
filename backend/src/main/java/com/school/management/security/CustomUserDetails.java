package com.school.management.security;

import com.school.management.entity.User;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 自定义用户详情
 *
 * @author system
 * @since 1.0.0
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

    public CustomUserDetails(User user, List<String> roles, List<String> permissions) {
        this.userId = user.getId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.realName = user.getRealName();
        this.status = user.getStatus();
        this.roles = roles;
        this.permissions = permissions;
        // 数据权限字段
        this.orgUnitId = user.getOrgUnitId();
        this.classId = user.getClassId();
        this.userType = user.getUserType();
    }

    public CustomUserDetails(User user, List<String> roles, List<String> permissions,
                             String gradeLevel) {
        this(user, roles, permissions);
        this.gradeLevel = gradeLevel;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 合并角色和权限
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
        return status == 1; // 1表示启用状态
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == 1;
    }

    /**
     * 检查是否有指定角色
     */
    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    /**
     * 检查是否有指定权限
     */
    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    /**
     * 获取用户ID
     */
    public Long getId() {
        return userId;
    }

    /**
     * 获取组织单元名称（用于审批等业务场景）
     * 注：当前返回空字符串，后续可通过缓存或查询获取实际名称
     */
    public String getOrgUnitName() {
        return "";
    }
}