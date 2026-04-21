package com.school.management.infrastructure.extension.plugins.healthcare;

import com.school.management.infrastructure.extension.PermissionProvider;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.school.management.infrastructure.extension.PermissionProvider.PermissionDef.of;

/**
 * 医疗行业权限 — 病人/病区相关.
 *
 * 由 PermissionRegistrar UPSERT 到 permissions 表, 打 industry=HEALTH + plugin_class 标签.
 */
@Component
@SuppressWarnings("deprecation")
public class HealthcarePermissionProvider implements PermissionProvider {

    @Override
    public String getModuleCode() { return "healthcare"; }

    @Override
    public String getModuleName() { return "医疗行业"; }

    @Override
    public List<PermissionDef> getPermissions() {
        return List.of(
            // ─── 病人基础 (HEALTH 领域核心) ───
            of("patient:view", "查看病人", ""),
            of("patient:edit", "编辑病人", ""),
            of("patient:add", "新增病人", ""),
            of("patient:delete", "删除病人", ""),

            // ─── 医嘱/诊疗动作 ───
            of("patient:prescribe", "开具医嘱", ""),
            of("patient:transfer", "转科/转病区", ""),
            of("patient:discharge", "办理出院", ""),
            of("patient:care_log:edit", "编辑护理日志", ""),

            // ─── 病区 ───
            of("ward:view", "查看病区", ""),
            of("ward:edit", "编辑病区", "")
        );
    }
}
