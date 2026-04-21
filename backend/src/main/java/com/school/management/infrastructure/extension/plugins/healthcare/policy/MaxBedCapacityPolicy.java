package com.school.management.infrastructure.extension.plugins.healthcare.policy;

import com.school.management.infrastructure.extension.Policy;
import com.school.management.infrastructure.extension.PolicyContext;
import com.school.management.infrastructure.extension.Violation;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 病区床位上限 — BEFORE_CHECKIN 阶段, 若 currentOccupancy &ge; capacity 产生 BLOCK 违规,
 * 阻断入住.
 */
@Component
public class MaxBedCapacityPolicy implements Policy<Object> {

    @Override
    public String code() { return "MAX_BED_CAPACITY"; }

    @Override
    public String name() { return "病区床位上限"; }

    @Override
    public boolean supports(PolicyContext<?> ctx) {
        return "place".equals(ctx.entityType()) && "BEFORE_CHECKIN".equals(ctx.phase());
    }

    @Override
    public List<Violation> check(PolicyContext<Object> ctx) {
        if (ctx.payload() instanceof Map<?, ?> m) {
            Object current = m.get("currentOccupancy");
            Object capacity = m.get("capacity");
            if (current instanceof Number c && capacity instanceof Number cap) {
                if (c.intValue() >= cap.intValue()) {
                    return List.of(Violation.block("BED_FULL",
                        "病区床位已满 (" + c + "/" + cap + ")"));
                }
            }
        }
        return List.of();
    }
}
