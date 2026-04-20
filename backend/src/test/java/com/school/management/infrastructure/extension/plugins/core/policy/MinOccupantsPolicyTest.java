package com.school.management.infrastructure.extension.plugins.core.policy;

import com.school.management.infrastructure.extension.PolicyContext;
import com.school.management.infrastructure.extension.Violation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MinOccupantsPolicyTest {

    @Test
    void supportsOnlyAfterCheckIn() {
        MinOccupantsPolicy p = new MinOccupantsPolicy(4);
        assertThat(p.supports(new PolicyContext<>("place", "AFTER_CHECKIN", null))).isTrue();
        assertThat(p.supports(new PolicyContext<>("place", "BEFORE_CHECKIN", null))).isFalse();
        assertThat(p.supports(new PolicyContext<>("org_unit", "AFTER_CHECKIN", null))).isFalse();
    }

    @Test
    void disabledWhenThresholdZero() {
        MinOccupantsPolicy p = new MinOccupantsPolicy(0);
        assertThat(p.supports(new PolicyContext<>("place", "AFTER_CHECKIN", null))).isFalse();
    }

    @Test
    void warnsWhenBelowThreshold() {
        MinOccupantsPolicy p = new MinOccupantsPolicy(4);
        Map<String, Object> payload = Map.of("currentOccupancy", 2);
        List<Violation> vs = p.check(new PolicyContext<>("place", "AFTER_CHECKIN", payload));
        assertThat(vs).hasSize(1);
        Violation v = vs.get(0);
        assertThat(v.severity()).isEqualTo(Violation.Severity.WARN);
        assertThat(v.code()).isEqualTo("MIN_OCCUPANTS");
    }

    @Test
    void passesAtOrAboveThreshold() {
        MinOccupantsPolicy p = new MinOccupantsPolicy(4);
        assertThat(p.check(new PolicyContext<>("place", "AFTER_CHECKIN", Map.of("currentOccupancy", 4))))
            .isEmpty();
        assertThat(p.check(new PolicyContext<>("place", "AFTER_CHECKIN", Map.of("currentOccupancy", 6))))
            .isEmpty();
    }

    @Test
    void passesWhenPayloadLacksOccupancy() {
        MinOccupantsPolicy p = new MinOccupantsPolicy(4);
        assertThat(p.check(new PolicyContext<>("place", "AFTER_CHECKIN", null))).isEmpty();
        assertThat(p.check(new PolicyContext<>("place", "AFTER_CHECKIN", "other"))).isEmpty();
    }
}
