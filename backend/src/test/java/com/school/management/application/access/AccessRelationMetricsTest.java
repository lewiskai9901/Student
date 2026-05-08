package com.school.management.application.access;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccessRelationMetricsTest {

    @Test
    void recordCheck_incrementsCorrectCounter() {
        SimpleMeterRegistry reg = new SimpleMeterRegistry();
        AccessRelationMetrics m = new AccessRelationMetrics(reg);
        m.recordCheck(true);
        m.recordCheck(true);
        m.recordCheck(false);
        assertThat(reg.find("access_relation_check_total").tag("result", "true").counter().count()).isEqualTo(2);
        assertThat(reg.find("access_relation_check_total").tag("result", "false").counter().count()).isEqualTo(1);
    }

    @Test
    void recordGrant_emitsTaggedCounter() {
        SimpleMeterRegistry reg = new SimpleMeterRegistry();
        AccessRelationMetrics m = new AccessRelationMetrics(reg);
        m.recordGrant("admin", "CORE");
        m.recordGrant("admin", "CORE");
        m.recordGrant("teaches", "DOMAIN");
        assertThat(reg.find("access_relation_grant_total").tag("relation", "admin").counter().count()).isEqualTo(2);
        assertThat(reg.find("access_relation_grant_total").tag("relation", "teaches").counter().count()).isEqualTo(1);
    }

    @Test
    void recordRevoke_emitsCounter() {
        SimpleMeterRegistry reg = new SimpleMeterRegistry();
        AccessRelationMetrics m = new AccessRelationMetrics(reg);
        m.recordRevoke("admin");
        assertThat(reg.find("access_relation_revoke_total").tag("relation", "admin").counter().count()).isEqualTo(1);
    }
}
