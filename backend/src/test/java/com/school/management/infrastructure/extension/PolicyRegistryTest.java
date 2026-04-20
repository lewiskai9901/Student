package com.school.management.infrastructure.extension;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class PolicyRegistryTest {

    static class AlwaysPass implements Policy<Void> {
        public String code() { return "pass"; }
        public boolean supports(PolicyContext<?> c) { return true; }
        public List<Violation> check(PolicyContext<Void> c) { return List.of(); }
    }

    static class WarnOnPlace implements Policy<Void> {
        public String code() { return "warn-place"; }
        public boolean supports(PolicyContext<?> c) { return "place".equals(c.entityType()); }
        public List<Violation> check(PolicyContext<Void> c) {
            return List.of(Violation.warn("W", "not ideal"));
        }
    }

    static class BlockOnPlace implements Policy<Void> {
        public String code() { return "block-place"; }
        public boolean supports(PolicyContext<?> c) { return "place".equals(c.entityType()); }
        public List<Violation> check(PolicyContext<Void> c) {
            return List.of(Violation.block("B", "nope"));
        }
    }

    @Test
    void checkFiltersBySupports() {
        PolicyRegistry reg = new PolicyRegistry(List.of(new AlwaysPass(), new WarnOnPlace()));
        List<Violation> r1 = reg.check(new PolicyContext<>("place", "X", null));
        assertThat(r1).hasSize(1).first().extracting(Violation::code).isEqualTo("W");
        List<Violation> r2 = reg.check(new PolicyContext<>("user", "X", null));
        assertThat(r2).isEmpty();
    }

    @Test
    void enforceThrowsOnBlock() {
        PolicyRegistry reg = new PolicyRegistry(List.of(new BlockOnPlace(), new WarnOnPlace()));
        assertThatThrownBy(() -> reg.enforce(new PolicyContext<>("place", "X", null)))
            .isInstanceOf(PolicyViolationException.class)
            .satisfies(ex -> {
                List<Violation> vs = ((PolicyViolationException) ex).getViolations();
                assertThat(vs).hasSize(2);
                assertThat(vs).anyMatch(v -> v.severity() == Violation.Severity.BLOCK);
            });
    }

    @Test
    void enforceReturnsWarnsWhenNoBlock() {
        PolicyRegistry reg = new PolicyRegistry(List.of(new WarnOnPlace()));
        List<Violation> r = reg.enforce(new PolicyContext<>("place", "X", null));
        assertThat(r).hasSize(1);
        assertThat(r.get(0).severity()).isEqualTo(Violation.Severity.WARN);
    }

    @Test
    void emptyRegistryAllowsEverything() {
        PolicyRegistry reg = new PolicyRegistry(List.of());
        assertThat(reg.check(new PolicyContext<>("place", "X", null))).isEmpty();
        assertThat(reg.enforce(new PolicyContext<>("place", "X", null))).isEmpty();
    }
}
