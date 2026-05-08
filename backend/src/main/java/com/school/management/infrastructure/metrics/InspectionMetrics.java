package com.school.management.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class InspectionMetrics {

    private final MeterRegistry registry;

    private Counter taskCreated;
    private Counter taskClaimed;
    private Counter taskSubmitted;
    private Counter taskPublished;
    private Counter appealSubmitted;
    private Counter orgUnitFilledExisting;
    private Counter orgUnitFilledUpstream;
    private Counter orgUnitFilledContext;
    private Counter orgUnitFilledMissed;
    private Timer submitTimer;

    public InspectionMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    @PostConstruct
    public void init() {
        taskCreated = Counter.builder("inspection_task_created_total").register(registry);
        taskClaimed = Counter.builder("inspection_task_claimed_total").register(registry);
        taskSubmitted = Counter.builder("inspection_task_submitted_total").register(registry);
        taskPublished = Counter.builder("inspection_task_published_total").register(registry);
        appealSubmitted = Counter.builder("inspection_appeal_submitted_total").register(registry);
        // 预先注册带 outcome label 的计数器, 避免 dashboard 空查询
        Counter.builder("inspection_task_reviewed_total").tag("outcome", "approved").register(registry);
        Counter.builder("inspection_appeal_resolved_total").tag("outcome", "approved").register(registry);
        Counter.builder("inspection_appeal_resolved_total").tag("outcome", "rejected").register(registry);
        // orgUnitId 自动填充策略命中分布 (InspectionDataPermissionFiller)
        // - existing: 业务层显式 set (override 通道)
        // - upstream: router 反查上游成功
        // - context : SecurityContext 兜底成功
        // - missed  : 全部失败 (生产监控漏率, 应≈0)
        orgUnitFilledExisting = Counter.builder("inspection_orgunit_filled_total")
                .tag("strategy", "existing").register(registry);
        orgUnitFilledUpstream = Counter.builder("inspection_orgunit_filled_total")
                .tag("strategy", "upstream").register(registry);
        orgUnitFilledContext = Counter.builder("inspection_orgunit_filled_total")
                .tag("strategy", "context").register(registry);
        orgUnitFilledMissed = Counter.builder("inspection_orgunit_filled_total")
                .tag("strategy", "missed").register(registry);
        submitTimer = Timer.builder("inspection_submit_duration_seconds")
                .publishPercentileHistogram()
                .register(registry);
    }

    public void taskCreated() { taskCreated.increment(); }
    public void taskClaimed() { taskClaimed.increment(); }
    public void taskSubmitted() { taskSubmitted.increment(); }
    public void taskPublished() { taskPublished.increment(); }
    public void appealSubmitted() { appealSubmitted.increment(); }

    /** orgUnitId 自动填充策略命中: existing/upstream/context/missed */
    public void orgUnitFilled(String strategy) {
        switch (strategy) {
            case "existing": orgUnitFilledExisting.increment(); break;
            case "upstream": orgUnitFilledUpstream.increment(); break;
            case "context":  orgUnitFilledContext.increment();  break;
            case "missed":   orgUnitFilledMissed.increment();   break;
            default: /* unknown strategy — silently ignore */
        }
    }

    public void taskReviewed(String outcome) {
        Counter.builder("inspection_task_reviewed_total")
                .tag("outcome", outcome)
                .register(registry).increment();
    }
    public void appealResolved(String outcome) {
        Counter.builder("inspection_appeal_resolved_total")
                .tag("outcome", outcome)
                .register(registry).increment();
    }

    public Timer.Sample startSubmitTimer() { return Timer.start(registry); }
    public void recordSubmitDuration(Timer.Sample sample) { sample.stop(submitTimer); }
}
