package com.school.management.domain.user.event;

import com.school.management.domain.shared.event.BaseDomainEvent;
import lombok.Getter;

/**
 * 用户角色集合发生变更（创建带角色 / 更新角色 / 单独分配 / 删除清空）时发出。
 *
 * <p>触发 jCasbin enforcer 对该用户 grouping policy 的热同步 ——
 * 否则新建/改角色后授权要等下次重启才生效（{@link com.school.management.infrastructure.casbin.CasbinConfig}
 * 仅启动时从 user_roles 重建）。由 {@code AccessEventHandler} 在 AFTER_COMMIT 调
 * {@code PolicyEnforcementService.syncUserRoles(userId)} 全量重算该用户的 g 策略，
 * 一次性处理新增/移除/替换，幂等。
 *
 * <p>与 {@link com.school.management.domain.access.event.UserRoleAssignedEvent}
 * 的区别：后者是 access 域"单条角色分配(带 scope，附带写 access_relations)"语义，仅 add；
 * 本事件是 user 域"该用户角色集整体已变"语义，做全量 resync，覆盖移除/替换。
 */
@Getter
public class UserRolesChangedEvent extends BaseDomainEvent {

    private final Long userId;

    public UserRolesChangedEvent(Long userId) {
        super("User", String.valueOf(userId));
        this.userId = userId;
    }

    @Override
    public String getEventType() {
        return "UserRolesChangedEvent";
    }
}
