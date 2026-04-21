package com.school.management.infrastructure.extension.plugins.healthcare.policy;

import com.school.management.infrastructure.extension.Policy;
import com.school.management.infrastructure.extension.PolicyContext;
import com.school.management.infrastructure.extension.Violation;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 病区性别一致性警告 — AFTER_CHECKIN 阶段, 若 payload 含 patientGender 与 wardGender 字段且不一致,
 * 产生 WARN 违规 (不阻断入住, 提示管理员复核).
 */
@Component
public class WardGenderConsistencyPolicy implements Policy<Object> {

    @Override
    public String code() { return "WARD_GENDER_CONSISTENCY"; }

    @Override
    public String name() { return "病区性别一致性警告"; }

    @Override
    public boolean supports(PolicyContext<?> ctx) {
        return "place".equals(ctx.entityType()) && "AFTER_CHECKIN".equals(ctx.phase());
    }

    @Override
    public List<Violation> check(PolicyContext<Object> ctx) {
        if (ctx.payload() instanceof Map<?, ?> m
                && m.containsKey("patientGender")
                && m.containsKey("wardGender")) {
            if (!Objects.equals(m.get("patientGender"), m.get("wardGender"))) {
                return List.of(Violation.warn("WARD_GENDER_MISMATCH",
                    "病人性别与病区不一致, 请核对"));
            }
        }
        return List.of();
    }
}
