package com.school.management.infrastructure.extension;

import com.school.management.infrastructure.extension.event.PolicyWarningEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
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

    /** 构造器小工具: 忽略 publisher 失败的简易桩. */
    private static PolicyRegistry build(ApplicationEventPublisher publisher, Policy<?>... policies) {
        return new PolicyRegistry(List.<Policy<?>>of(policies), publisher);
    }
    private static PolicyRegistry build(Policy<?>... policies) {
        return build(e -> {}, policies);
    }

    @Test
    void checkFiltersBySupports() {
        PolicyRegistry reg = build(new AlwaysPass(), new WarnOnPlace());
        List<Violation> r1 = reg.check(new PolicyContext<>("place", "X", null));
        assertThat(r1).hasSize(1).first().extracting(Violation::code).isEqualTo("W");
        List<Violation> r2 = reg.check(new PolicyContext<>("user", "X", null));
        assertThat(r2).isEmpty();
    }

    @Test
    void enforceThrowsOnBlock() {
        PolicyRegistry reg = build(new BlockOnPlace(), new WarnOnPlace());
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
        PolicyRegistry reg = build(new WarnOnPlace());
        List<Violation> r = reg.enforce(new PolicyContext<>("place", "X", null));
        assertThat(r).hasSize(1);
        assertThat(r.get(0).severity()).isEqualTo(Violation.Severity.WARN);
    }

    @Test
    void emptyRegistryAllowsEverything() {
        PolicyRegistry reg = build();
        assertThat(reg.check(new PolicyContext<>("place", "X", null))).isEmpty();
        assertThat(reg.enforce(new PolicyContext<>("place", "X", null))).isEmpty();
    }

    /** M4.3: WARN violations 应发布 PolicyWarningEvent. */
    @Test
    void warnViolationsPublishEvent() {
        List<Object> captured = new ArrayList<>();
        PolicyRegistry reg = build(captured::add, new WarnOnPlace());

        reg.check(new PolicyContext<>("place", "X", null));

        assertThat(captured).hasSize(1);
        assertThat(captured.get(0)).isInstanceOf(PolicyWarningEvent.class);
        PolicyWarningEvent ev = (PolicyWarningEvent) captured.get(0);
        assertThat(ev.getViolations()).hasSize(1);
        assertThat(ev.getViolations().get(0).severity()).isEqualTo(Violation.Severity.WARN);
    }

    /** M4.3: 纯 BLOCK 违规不发 event (BLOCK 走异常路径). */
    @Test
    void pureBlockViolationsDoNotPublishEvent() {
        List<Object> captured = new ArrayList<>();
        PolicyRegistry reg = build(captured::add, new BlockOnPlace());

        // 用 check() 避免 enforce 抛出
        reg.check(new PolicyContext<>("place", "X", null));

        // 只有 BLOCK → 不应发 event
        assertThat(captured).isEmpty();
    }

    /** M4.3: 无违规不发 event. */
    @Test
    void noViolationsDoNotPublishEvent() {
        List<Object> captured = new ArrayList<>();
        PolicyRegistry reg = build(captured::add, new AlwaysPass());

        reg.check(new PolicyContext<>("user", "X", null));

        assertThat(captured).isEmpty();
    }
}
