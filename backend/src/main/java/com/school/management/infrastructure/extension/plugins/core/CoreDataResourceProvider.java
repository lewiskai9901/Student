package com.school.management.infrastructure.extension.plugins.core;

import com.school.management.infrastructure.extension.DataResourceProvider;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.school.management.infrastructure.extension.DataResourceProvider.DataResourceDef.of;

/**
 * 通用核心数据资源 — 组织向走 5 种常规 scope, 个人向只能 SELF.
 *
 * 本 Provider 覆盖 V20260429_1 migration 中 CORE 段的 seed, 启动时 UPDATE 覆盖.
 */
@Component
public class CoreDataResourceProvider implements DataResourceProvider {

    @Override
    public List<DataResourceDef> getDataResources() {
        return List.of(
            // 组织向: 5 种常规 scope
            of("user",        "ALL", "DEPARTMENT_AND_BELOW", "DEPARTMENT", "SELF", "CUSTOM"),
            of("org_unit",    "ALL", "DEPARTMENT_AND_BELOW", "DEPARTMENT", "SELF", "CUSTOM"),
            of("role",        "ALL", "DEPARTMENT_AND_BELOW", "DEPARTMENT", "SELF", "CUSTOM"),
            of("place",       "ALL", "DEPARTMENT_AND_BELOW", "DEPARTMENT", "SELF", "CUSTOM"),
            of("system_role", "ALL", "DEPARTMENT_AND_BELOW", "DEPARTMENT", "SELF", "CUSTOM"),
            of("system_user", "ALL", "DEPARTMENT_AND_BELOW", "DEPARTMENT", "SELF", "CUSTOM"),

            // 个人向: 只能 SELF
            of("notification", "SELF"),
            of("dashboard",    "SELF"),

            // 检查平台 (通用核心): 行级数据走 5 种常规 scope; appeal/personal 个人向走 SELF
            of("inspection_project",     "ALL", "DEPARTMENT_AND_BELOW", "DEPARTMENT", "SELF", "CUSTOM"),
            of("inspection_task",        "ALL", "DEPARTMENT_AND_BELOW", "DEPARTMENT", "SELF", "CUSTOM"),
            of("inspection_record",      "ALL", "DEPARTMENT_AND_BELOW", "DEPARTMENT", "SELF", "CUSTOM"),
            of("inspection_corrective",  "ALL", "DEPARTMENT_AND_BELOW", "DEPARTMENT", "SELF", "CUSTOM"),
            of("inspection_alert",       "ALL", "DEPARTMENT_AND_BELOW", "DEPARTMENT", "SELF", "CUSTOM"),
            of("inspection_observation", "ALL", "DEPARTMENT_AND_BELOW", "DEPARTMENT", "SELF", "CUSTOM"),
            of("inspection_violation",   "ALL", "DEPARTMENT_AND_BELOW", "DEPARTMENT", "SELF", "CUSTOM"),
            of("inspection_summary",     "ALL", "DEPARTMENT_AND_BELOW", "DEPARTMENT", "CUSTOM"),
            of("inspection_template",    "ALL", "SELF"),
            of("inspection_appeal",      "SELF"),
            of("inspection_personal",    "SELF")
        );
    }
}
