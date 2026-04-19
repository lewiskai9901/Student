package com.school.management.infrastructure.extension.plugins.core;

import com.school.management.infrastructure.extension.RolePresetPlugin;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 通用核心预置角色 — 平台级、行业无关.
 */
@Component
public class CoreRolePresetPlugin implements RolePresetPlugin {

    @Override
    public List<RolePresetDef> getPresets() {
        return List.of(
            RolePresetDef.of("SUPER_ADMIN", "超级管理员",
                "平台最高权限,系统唯一,不受租户/行业限制", 0),

            RolePresetDef.of("TENANT_ADMIN", "租户管理员",
                "单租户下的管理员,管理本租户所有业务", 10),

            RolePresetDef.of("GUEST", "访客",
                "只读角色,不能创建/修改任何数据", 90)
        );
    }
}
