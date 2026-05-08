package com.school.management.application.access;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 关系管理 Prometheus 监控指标.
 *
 * <p>暴露:
 * <ul>
 *   <li>access_relation_check_total{result, cache} — check() 次数, 区分 hit/miss + cached/uncached</li>
 *   <li>access_relation_grant_total{relation, tier} — grant 操作次数</li>
 *   <li>access_relation_revoke_total{relation} — revoke 次数</li>
 *   <li>access_relation_check_seconds — check 延迟分布</li>
 * </ul>
 *
 * <p>由 AccessRelationService / AccessCheckCache 调用.
 */
@Slf4j
@Component
public class AccessRelationMetrics {

    private final MeterRegistry registry;
    private final ConcurrentMap<String, Counter> grantCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> revokeCounters = new ConcurrentHashMap<>();

    private final Counter checkHit;
    private final Counter checkMiss;
    private final Timer checkTimer;

    public AccessRelationMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.checkHit = Counter.builder("access_relation_check_total")
            .tag("result", "true").register(registry);
        this.checkMiss = Counter.builder("access_relation_check_total")
            .tag("result", "false").register(registry);
        this.checkTimer = Timer.builder("access_relation_check_seconds")
            .register(registry);
    }

    public void recordCheck(boolean result) {
        if (result) checkHit.increment();
        else checkMiss.increment();
    }

    public Timer.Sample startCheckTimer() {
        return Timer.start(registry);
    }

    public void stopCheckTimer(Timer.Sample sample) {
        sample.stop(checkTimer);
    }

    public void recordGrant(String relation, String tier) {
        grantCounters.computeIfAbsent(relation + ":" + tier, k ->
            Counter.builder("access_relation_grant_total")
                .tag("relation", relation).tag("tier", tier == null ? "unknown" : tier)
                .register(registry)
        ).increment();
    }

    public void recordRevoke(String relation) {
        revokeCounters.computeIfAbsent(relation, k ->
            Counter.builder("access_relation_revoke_total")
                .tag("relation", relation).register(registry)
        ).increment();
    }
}
