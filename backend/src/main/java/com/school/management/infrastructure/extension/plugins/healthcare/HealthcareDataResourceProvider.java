package com.school.management.infrastructure.extension.plugins.healthcare;

import com.school.management.infrastructure.extension.DataResourceProvider;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.school.management.infrastructure.extension.DataResourceProvider.DataResourceDef.of;

/**
 * 医疗行业数据资源 — 病人向加 BY_WARD / BY_ATTENDING_DOCTOR 维度,
 * 其他组织向走 5 种常规 scope.
 *
 * 本 Provider 覆盖 V20260429_1 migration 中 HEALTH 段的 seed, 启动时 UPDATE 覆盖.
 * 若 patient/medical_record/prescription 对应的 data_resources 行不存在,
 * Registrar 记 WARN 并跳过, 不阻塞启动.
 */
@Component
public class HealthcareDataResourceProvider implements DataResourceProvider {

    @Override
    public List<DataResourceDef> getDataResources() {
        return List.of(
            // 病人向: 病房 / 主治医生 维度
            of("patient",         "ALL", "BY_WARD", "BY_ATTENDING_DOCTOR", "SELF", "CUSTOM"),
            of("medical_record",  "ALL", "BY_WARD", "BY_ATTENDING_DOCTOR", "SELF", "CUSTOM"),
            of("prescription",    "ALL", "BY_WARD", "BY_ATTENDING_DOCTOR", "SELF", "CUSTOM")
        );
    }
}
