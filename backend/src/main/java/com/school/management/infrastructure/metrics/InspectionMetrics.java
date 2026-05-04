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
    private Timer submitTimer;

    public InspectionMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    @PostConstruct
    void init() {
        taskCreated = Counter.builder("inspection_task_created_total").register(registry);
        taskClaimed = Counter.builder("inspection_task_claimed_total").register(registry);
        taskSubmitted = Counter.builder("inspection_task_submitted_total").register(registry);
        taskPublished = Counter.builder("inspection_task_published_total").register(registry);
        appealSubmitted = Counter.builder("inspection_appeal_submitted_total").register(registry);
        // 预先注册带 outcome label 的计数器, 避免 dashboard 空查询
        Counter.builder("inspection_task_reviewed_total").tag("outcome", "approved").register(registry);
        Counter.builder("inspection_appeal_resolved_total").tag("outcome", "approved").register(registry);
        Counter.builder("inspection_appeal_resolved_total").tag("outcome", "rejected").register(registry);
        submitTimer = Timer.builder("inspection_submit_duration_seconds")
                .publishPercentileHistogram()
                .register(registry);
    }

    public void taskCreated() { taskCreated.increment(); }
    public void taskClaimed() { taskClaimed.increment(); }
    public void taskSubmitted() { taskSubmitted.increment(); }
    public void taskPublished() { taskPublished.increment(); }
    public void appealSubmitted() { appealSubmitted.increment(); }

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
