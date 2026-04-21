package com.school.management.infrastructure.extension.plugins.healthcare.policy;

import com.school.management.infrastructure.extension.Policy;
import com.school.management.infrastructure.extension.PolicyContext;
import com.school.management.infrastructure.extension.Violation;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 主治医师必填提示 — user AFTER_CREATE, 若新建的是 PATIENT 类型用户且尚未设置主治医师,
 * 产生 INFO 违规 (纯提示, 不阻断).
 */
@Component
public class AttendingDoctorRequiredPolicy implements Policy<Object> {

    @Override
    public String code() { return "ATTENDING_DOCTOR_REQUIRED"; }

    @Override
    public String name() { return "主治医师必填提示"; }

    @Override
    public boolean supports(PolicyContext<?> ctx) {
        return "user".equals(ctx.entityType()) && "AFTER_CREATE".equals(ctx.phase());
    }

    @Override
    public List<Violation> check(PolicyContext<Object> ctx) {
        if (ctx.payload() instanceof Map<?, ?> m && "PATIENT".equals(m.get("userTypeCode"))) {
            return List.of(Violation.info("NO_ATTENDING_DOCTOR",
                "新建病人用户, 请及时设置主治医师 (attending_of 关系)"));
        }
        return List.of();
    }
}
