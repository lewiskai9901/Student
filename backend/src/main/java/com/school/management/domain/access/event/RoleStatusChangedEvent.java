package com.school.management.domain.access.event;

import com.school.management.domain.access.model.Role;
import com.school.management.domain.shared.event.BaseDomainEvent;
import lombok.Getter;

/**
 * 角色生效状态变更(启用 / 禁用 / 删除)事件。
 *
 * <p>这些操作改变角色功能权限的生效性, 但不走 grant/revoke 的
 * {@link RolePermissionsChangedEvent} 路径。单列此事件闭合 Casbin enforcer 同步缺口:
 * 否则禁用/删除一个角色后, 其 {@code @CasbinAccess} 授权仍在内存 enforcer 中放行,
 * 直到下次重启或插件生命周期事件才失效。
 */
@Getter
public class RoleStatusChangedEvent extends BaseDomainEvent {

    /** ENABLED / DISABLED / DELETED */
    private final String changeType;
    private final String roleCode;

    public RoleStatusChangedEvent(Role role, String changeType) {
        super("Role", role.getId() != null ? role.getId().toString() : "unknown");
        this.roleCode = role.getRoleCode();
        this.changeType = changeType;
    }

    /** Returns the role ID (null if unknown). */
    public Long getRoleId() {
        String aggId = getAggregateId();
        return "unknown".equals(aggId) ? null : Long.valueOf(aggId);
    }

    @Override
    public String getEventType() {
        return "RoleStatusChangedEvent";
    }
}
