package com.school.management.application.schedule;

import com.school.management.application.schedule.command.CreatePolicyCommand;
import com.school.management.application.schedule.command.UpdatePolicyCommand;
import com.school.management.domain.schedule.ScheduleCodeGenerator;
import com.school.management.domain.schedule.model.*;
import com.school.management.domain.schedule.repository.ScheduleExecutionRepository;
import com.school.management.domain.schedule.repository.SchedulePolicyRepository;
import com.school.management.domain.shared.event.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ScheduleApplicationService {

    private final SchedulePolicyRepository policyRepository;
    private final ScheduleExecutionRepository executionRepository;
    private final ScheduleCodeGenerator codeGenerator;
    private final DomainEventPublisher eventPublisher;

    public SchedulePolicy createPolicy(CreatePolicyCommand command) {
        String policyCode = codeGenerator.generatePolicyCode();

        SchedulePolicy policy = SchedulePolicy.create(
            policyCode,
            command.getPolicyName(),
            PolicyType.valueOf(command.getPolicyType()),
            RotationAlgorithm.valueOf(command.getRotationAlgorithm()),
            command.getTemplateId(),
            command.getInspectorPool(),
            command.getScheduleConfig(),
            command.getExcludedDates(),
            command.getCreatedBy()
        );

        policy = policyRepository.save(policy);
        publishEvents(policy);
        return policy;
    }

    public SchedulePolicy updatePolicy(UpdatePolicyCommand command) {
        SchedulePolicy policy = policyRepository.findById(command.getPolicyId())
            .orElseThrow(() -> new IllegalArgumentException("Policy not found: " + command.getPolicyId()));

        policy.updateConfig(
            command.getPolicyName(),
            RotationAlgorithm.valueOf(command.getRotationAlgorithm()),
            command.getInspectorPool(),
            command.getScheduleConfig(),
            command.getExcludedDates()
        );

        policy = policyRepository.save(policy);
        return policy;
    }

    public void enablePolicy(Long policyId) {
        SchedulePolicy policy = policyRepository.findById(policyId)
            .orElseThrow(() -> new IllegalArgumentException("Policy not found: " + policyId));
        policy.enable();
        policyRepository.save(policy);
        publishEvents(policy);
    }

    public void disablePolicy(Long policyId) {
        SchedulePolicy policy = policyRepository.findById(policyId)
            .orElseThrow(() -> new IllegalArgumentException("Policy not found: " + policyId));
        policy.disable();
        policyRepository.save(policy);
        publishEvents(policy);
    }

    @Transactional(readOnly = true)
    public SchedulePolicy getPolicy(Long policyId) {
        return policyRepository.findById(policyId)
            .orElseThrow(() -> new IllegalArgumentException("Policy not found: " + policyId));
    }

    @Transactional(readOnly = true)
    public List<SchedulePolicy> listPolicies() {
        return policyRepository.findAll();
    }

    public void deletePolicy(Long policyId) {
        SchedulePolicy policy = policyRepository.findById(policyId)
            .orElseThrow(() -> new IllegalArgumentException("Policy not found: " + policyId));
        policyRepository.delete(policy);
    }

    @Transactional(readOnly = true)
    public List<ScheduleExecution> listExecutions(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        return executionRepository.findByDateRange(startDate, endDate);
    }

    private void publishEvents(SchedulePolicy policy) {
        eventPublisher.publishAll(policy.getDomainEvents());
        policy.clearDomainEvents();
    }
}
