package com.school.management.application.corrective;

import com.school.management.application.corrective.command.CreateActionCommand;
import com.school.management.application.corrective.command.ResolveActionCommand;
import com.school.management.application.corrective.command.VerifyActionCommand;
import com.school.management.domain.corrective.model.*;
import com.school.management.domain.corrective.repository.AutoActionRuleRepository;
import com.school.management.domain.corrective.repository.CorrectiveActionRepository;
import com.school.management.domain.shared.event.DomainEvent;
import com.school.management.domain.shared.event.DomainEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class CorrectiveActionApplicationService {

    private final CorrectiveActionRepository actionRepository;
    private final AutoActionRuleRepository ruleRepository;
    private final DomainEventPublisher eventPublisher;

    public CorrectiveActionApplicationService(
            CorrectiveActionRepository actionRepository,
            AutoActionRuleRepository ruleRepository,
            DomainEventPublisher eventPublisher) {
        this.actionRepository = actionRepository;
        this.ruleRepository = ruleRepository;
        this.eventPublisher = eventPublisher;
    }

    public CorrectiveAction createAction(CreateActionCommand command) {
        String actionCode = generateActionCode();

        CorrectiveAction action = CorrectiveAction.create(
                actionCode,
                command.getTitle(),
                command.getDescription(),
                command.getSource(),
                command.getSourceId(),
                command.getSeverity(),
                command.getCategory(),
                command.getClassId(),
                command.getAssigneeId(),
                command.getDeadline(),
                command.getCreatedBy()
        );

        action = actionRepository.save(action);
        publishEvents(action);
        return action;
    }

    public CorrectiveAction createFromInspection(Long sourceId, String title, String description,
                                                   ActionSeverity severity, ActionCategory category,
                                                   Long classId, Long assigneeId, int deadlineHours,
                                                   Long createdBy) {
        CreateActionCommand command = CreateActionCommand.builder()
                .title(title)
                .description(description)
                .source(ActionSource.INSPECTION)
                .sourceId(sourceId)
                .severity(severity)
                .category(category)
                .classId(classId)
                .assigneeId(assigneeId)
                .deadline(LocalDateTime.now().plusHours(deadlineHours))
                .createdBy(createdBy)
                .build();
        return createAction(command);
    }

    public CorrectiveAction startProgress(Long actionId) {
        CorrectiveAction action = actionRepository.findById(actionId)
                .orElseThrow(() -> new IllegalArgumentException("Action not found: " + actionId));
        action.startProgress();
        action = actionRepository.save(action);
        publishEvents(action);
        return action;
    }

    public CorrectiveAction resolve(ResolveActionCommand command) {
        CorrectiveAction action = actionRepository.findById(command.getActionId())
                .orElseThrow(() -> new IllegalArgumentException("Action not found: " + command.getActionId()));
        action.resolve(command.getResolutionNote(), command.getAttachments());
        action = actionRepository.save(action);
        publishEvents(action);
        return action;
    }

    public CorrectiveAction verify(VerifyActionCommand command) {
        CorrectiveAction action = actionRepository.findById(command.getActionId())
                .orElseThrow(() -> new IllegalArgumentException("Action not found: " + command.getActionId()));
        action.verify(command.getVerifierId(), command.getResult(), command.getComment());
        action = actionRepository.save(action);
        publishEvents(action);
        return action;
    }

    public void markOverdueActions() {
        List<CorrectiveAction> overdueActions = actionRepository.findOverdue();
        for (CorrectiveAction action : overdueActions) {
            action.markOverdue();
            actionRepository.save(action);
            publishEvents(action);
        }
    }

    public CorrectiveAction escalate(Long actionId) {
        CorrectiveAction action = actionRepository.findById(actionId)
                .orElseThrow(() -> new IllegalArgumentException("Action not found: " + actionId));
        action.escalate();
        action = actionRepository.save(action);
        publishEvents(action);
        return action;
    }

    @Transactional(readOnly = true)
    public Optional<CorrectiveAction> getAction(Long actionId) {
        return actionRepository.findById(actionId);
    }

    @Transactional(readOnly = true)
    public List<CorrectiveAction> listByStatus(ActionStatus status) {
        return actionRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<CorrectiveAction> listByClassId(Long classId) {
        return actionRepository.findByClassId(classId);
    }

    @Transactional(readOnly = true)
    public List<CorrectiveAction> listByAssigneeId(Long assigneeId) {
        return actionRepository.findByAssigneeId(assigneeId);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getStatistics() {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (ActionStatus status : ActionStatus.values()) {
            stats.put(status.name(), actionRepository.countByStatus(status));
        }
        return stats;
    }

    // ==================== Rule Operations ====================

    @Transactional(readOnly = true)
    public List<AutoActionRule> listEnabledRules() {
        return ruleRepository.findEnabled();
    }

    @Transactional(readOnly = true)
    public Optional<AutoActionRule> getRule(Long ruleId) {
        return ruleRepository.findById(ruleId);
    }

    public AutoActionRule saveRule(AutoActionRule rule) {
        return ruleRepository.save(rule);
    }

    public void deleteRule(Long ruleId) {
        ruleRepository.deleteById(ruleId);
    }

    // ==================== Private Helpers ====================

    private void publishEvents(CorrectiveAction action) {
        for (DomainEvent event : action.getDomainEvents()) {
            eventPublisher.publish(event);
        }
        action.clearDomainEvents();
    }

    private String generateActionCode() {
        return String.format("CA-%d-%s",
                System.currentTimeMillis(),
                UUID.randomUUID().toString().substring(0, 6).toUpperCase());
    }
}
