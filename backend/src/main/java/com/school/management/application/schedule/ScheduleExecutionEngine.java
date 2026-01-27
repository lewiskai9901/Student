package com.school.management.application.schedule;

import com.school.management.domain.schedule.model.*;
import com.school.management.domain.schedule.repository.ScheduleExecutionRepository;
import com.school.management.domain.schedule.repository.SchedulePolicyRepository;
import com.school.management.domain.schedule.service.HolidayService;
import com.school.management.domain.schedule.service.InspectorRotationService;
import com.school.management.domain.shared.event.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleExecutionEngine {

    private final SchedulePolicyRepository policyRepository;
    private final ScheduleExecutionRepository executionRepository;
    private final InspectorRotationService rotationService;
    private final HolidayService holidayService;
    private final DomainEventPublisher eventPublisher;

    /**
     * Daily scheduled execution at 06:00.
     */
    @Scheduled(cron = "0 0 6 * * ?")
    @Transactional
    public void executeDailySchedule() {
        LocalDate today = LocalDate.now();
        log.info("Starting daily schedule execution for date: {}", today);

        List<SchedulePolicy> enabledPolicies = policyRepository.findEnabled();
        log.info("Found {} enabled policies", enabledPolicies.size());

        for (SchedulePolicy policy : enabledPolicies) {
            try {
                executeForPolicy(policy, today);
            } catch (Exception e) {
                log.error("Failed to execute schedule for policy {}: {}", policy.getPolicyCode(), e.getMessage(), e);
            }
        }

        log.info("Daily schedule execution completed for date: {}", today);
    }

    /**
     * Manual trigger for a specific date.
     */
    @Transactional
    public ScheduleExecution triggerManual(Long policyId, LocalDate date) {
        SchedulePolicy policy = policyRepository.findById(policyId)
            .orElseThrow(() -> new IllegalArgumentException("Policy not found: " + policyId));
        return executeForPolicy(policy, date);
    }

    private ScheduleExecution executeForPolicy(SchedulePolicy policy, LocalDate date) {
        // Check if already executed for this date
        List<ScheduleExecution> existing = executionRepository.findByPolicyIdAndDate(policy.getId(), date);
        if (!existing.isEmpty()) {
            log.info("Policy {} already has execution for date {}, skipping", policy.getPolicyCode(), date);
            return existing.get(0);
        }

        // Check if date is excluded
        if (holidayService.isExcluded(date, policy.getExcludedDates())) {
            log.info("Date {} is excluded for policy {}, creating SKIPPED execution", date, policy.getPolicyCode());
            ScheduleExecution skipped = ScheduleExecution.builder()
                .policyId(policy.getId())
                .executionDate(date)
                .assignedInspectors(List.of())
                .build();
            skipped.skip("Date is excluded (holiday or excluded date)");
            return executionRepository.save(skipped);
        }

        // Check if it's a holiday (weekend)
        if (holidayService.isHoliday(date)) {
            log.info("Date {} is a holiday for policy {}, creating SKIPPED execution", date, policy.getPolicyCode());
            ScheduleExecution skipped = ScheduleExecution.builder()
                .policyId(policy.getId())
                .executionDate(date)
                .assignedInspectors(List.of())
                .build();
            skipped.skip("Date is a holiday/weekend");
            return executionRepository.save(skipped);
        }

        // Select inspectors using rotation algorithm
        List<Long> selectedInspectors = rotationService.selectInspectors(policy, date);

        // Create execution record
        ScheduleExecution execution = ScheduleExecution.builder()
            .policyId(policy.getId())
            .executionDate(date)
            .assignedInspectors(selectedInspectors)
            .build();

        try {
            execution.execute();
            execution = executionRepository.save(execution);
            publishEvents(execution);
            log.info("Successfully executed schedule for policy {} on {}, assigned inspectors: {}",
                policy.getPolicyCode(), date, selectedInspectors);
        } catch (Exception e) {
            execution.fail(e.getMessage());
            execution = executionRepository.save(execution);
            publishEvents(execution);
            log.error("Failed to execute schedule for policy {} on {}: {}",
                policy.getPolicyCode(), date, e.getMessage());
        }

        return execution;
    }

    private void publishEvents(ScheduleExecution execution) {
        eventPublisher.publishAll(execution.getDomainEvents());
        execution.clearDomainEvents();
    }
}
