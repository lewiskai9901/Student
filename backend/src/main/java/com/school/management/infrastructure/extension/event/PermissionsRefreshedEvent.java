package com.school.management.infrastructure.extension.event;

import org.springframework.context.ApplicationEvent;

/**
 * 权限/角色被 Registrar 批量同步后发出的事件.
 *
 * 订阅者:
 *   - CasbinPolicyService → 重载 enforcer 策略
 *   - 其他缓存 (PermissionCache 等) → 失效重建
 *
 * 发布时机:
 *   - PermissionRegistrar.afterSync() 完成后
 *   - RolePresetRegistrar.afterSync() 完成后
 */
public class PermissionsRefreshedEvent extends ApplicationEvent {

    private final String source;  // "PermissionRegistrar" / "RolePresetRegistrar" / "CREATE_ROLE" etc.

    public PermissionsRefreshedEvent(Object src, String source) {
        super(src);
        this.source = source;
    }

    public String getSourceName() { return source; }
}
