package com.school.management.infrastructure.extension.plugins.healthcare;

import com.school.management.infrastructure.extension.RolePresetPlugin;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 医疗行业预置角色.
 *
 * 启动时由 RolePresetRegistrar UPSERT 到 roles 表, 打 is_plugin_preset=1 + industry=HEALTH.
 * 管理员不可删, 可禁用 (status=0).
 */
@Component
@SuppressWarnings("deprecation")
public class HealthcareRolePresetPlugin implements RolePresetPlugin {

    @Override
    public List<RolePresetDef> getPresets() {
        return List.of(
            RolePresetDef.of("DOCTOR", "医师",
                "临床医师, 可查看/开具医嘱/安排转科/办理出院", 10)
                .withPermissions(Set.of(
                    "patient:view", "patient:edit",
                    "patient:prescribe", "patient:transfer", "patient:discharge",
                    "ward:view"
                )),

            RolePresetDef.of("NURSE", "护士",
                "责任护士, 仅查看并记录护理日志", 20)
                .withPermissions(Set.of(
                    "patient:view",
                    "patient:care_log:edit"
                )),

            RolePresetDef.of("WARD_HEAD", "病区主任",
                "病区管理者, 管理本病区人员配置与运营", 5)
                .withPermissions(Set.of(
                    "patient:view",
                    "ward:view", "ward:edit"
                ))
        );
    }
}
