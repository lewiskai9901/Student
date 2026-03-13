package com.school.management.application.inspection.v7;

import com.school.management.domain.inspection.model.v7.corrective.*;
import com.school.management.domain.inspection.repository.v7.CorrectiveCaseRepository;
import com.school.management.domain.inspection.repository.v7.CorrectiveSubtaskRepository;
import com.school.management.infrastructure.event.SpringDomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class CorrectiveCaseApplicationService {

    private final CorrectiveCaseRepository caseRepository;
    private final CorrectiveSubtaskRepository subtaskRepository;
    private final SpringDomainEventPublisher eventPublisher;

    // ========== CRUD ==========

    @Transactional
    public CorrectiveCase createCase(String caseCode, String issueDescription,
                                     CasePriority priority, Long submissionId, Long detailId,
                                     Long projectId, Long taskId,
                                     String targetType, Long targetId, String targetName,
                                     String requiredAction, LocalDateTime deadline,
                                     Long createdBy) {
        CorrectiveCase c = CorrectiveCase.create(caseCode, issueDescription, priority, createdBy);

        // Set additional fields via builder + reconstruct is not possible after create,
        // so we use the builder approach directly
        CorrectiveCase fullCase = CorrectiveCase.reconstruct(CorrectiveCase.builder()
                .caseCode(caseCode)
                .issueDescription(issueDescription)
                .priority(priority)
                .submissionId(submissionId)
                .detailId(detailId)
                .projectId(projectId)
                .taskId(taskId)
                .targetType(targetType)
                .targetId(targetId)
                .targetName(targetName)
                .requiredAction(requiredAction)
                .deadline(deadline)
                .createdBy(createdBy)
                .status(CaseStatus.OPEN)
                .escalationLevel(0));

        CorrectiveCase saved = caseRepository.save(fullCase);
        // Publish the created event with actual ID
        eventPublisher.publish(new com.school.management.domain.inspection.event.v7.CorrectiveCaseCreatedEvent(
                saved.getId(), caseCode, submissionId, detailId, priority != null ? priority.name() : null));
        log.info("Created corrective case: code={}, priority={}", caseCode, priority);
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<CorrectiveCase> getCase(Long id) {
        return caseRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<CorrectiveCase> getCaseByCaseCode(String caseCode) {
        return caseRepository.findByCaseCode(caseCode);
    }

    @Transactional(readOnly = true)
    public List<CorrectiveCase> listByProject(Long projectId) {
        return caseRepository.findByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public List<CorrectiveCase> listBySubmission(Long submissionId) {
        return caseRepository.findBySubmissionId(submissionId);
    }

    @Transactional(readOnly = true)
    public List<CorrectiveCase> listByAssignee(Long assigneeId) {
        return caseRepository.findByAssigneeId(assigneeId);
    }

    @Transactional(readOnly = true)
    public List<CorrectiveCase> listByStatus(CaseStatus status) {
        return caseRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<CorrectiveCase> listByTask(Long taskId) {
        return caseRepository.findByTaskId(taskId);
    }

    @Transactional(readOnly = true)
    public List<CorrectiveCase> listAll() {
        return caseRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<CorrectiveCase> listOverdue() {
        return caseRepository.findOverdue(LocalDateTime.now());
    }

    @Transactional
    public void deleteCase(Long id) {
        caseRepository.deleteById(id);
    }

    // ========== Lifecycle ==========

    @Transactional
    public CorrectiveCase assignCase(Long id, Long assigneeId, String assigneeName) {
        CorrectiveCase c = caseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("整改案例不存在: " + id));
        c.assign(assigneeId, assigneeName);
        caseRepository.save(c);
        eventPublisher.publishAll(c.getDomainEvents());
        c.clearDomainEvents();
        log.info("Assigned corrective case {} to {}", c.getCaseCode(), assigneeName);
        return c;
    }

    @Transactional
    public CorrectiveCase startWork(Long id) {
        CorrectiveCase c = caseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("整改案例不存在: " + id));
        c.startWork();
        return caseRepository.save(c);
    }

    @Transactional
    public CorrectiveCase submitCorrection(Long id, String correctionNote, List<Long> evidenceIds) {
        CorrectiveCase c = caseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("整改案例不存在: " + id));
        c.submitCorrection(correctionNote, evidenceIds);
        caseRepository.save(c);
        eventPublisher.publishAll(c.getDomainEvents());
        c.clearDomainEvents();
        log.info("Correction submitted for case {}", c.getCaseCode());
        return c;
    }

    @Transactional
    public CorrectiveCase verifyCase(Long id, Long verifierId, String verifierName, String note) {
        CorrectiveCase c = caseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("整改案例不存在: " + id));
        c.verify(verifierId, verifierName, note);
        caseRepository.save(c);
        eventPublisher.publishAll(c.getDomainEvents());
        c.clearDomainEvents();
        log.info("Corrective case {} verified by {}", c.getCaseCode(), verifierName);
        return c;
    }

    @Transactional
    public CorrectiveCase rejectCase(Long id, Long verifierId, String verifierName, String reason) {
        CorrectiveCase c = caseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("整改案例不存在: " + id));
        c.reject(verifierId, verifierName, reason);
        caseRepository.save(c);
        eventPublisher.publishAll(c.getDomainEvents());
        c.clearDomainEvents();
        log.info("Corrective case {} rejected: {}", c.getCaseCode(), reason);
        return c;
    }

    @Transactional
    public CorrectiveCase closeCase(Long id) {
        CorrectiveCase c = caseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("整改案例不存在: " + id));
        c.close();
        return caseRepository.save(c);
    }

    @Transactional
    public CorrectiveCase escalateCase(Long id) {
        CorrectiveCase c = caseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("整改案例不存在: " + id));
        c.escalate();
        caseRepository.save(c);
        eventPublisher.publishAll(c.getDomainEvents());
        c.clearDomainEvents();
        log.info("Corrective case {} escalated to level {}", c.getCaseCode(), c.getEscalationLevel());
        return c;
    }

    // ========== Subtask Management ==========

    @Transactional
    public CorrectiveSubtask createSubtask(Long caseId, String subtaskName, String description,
                                           Long assigneeId, Integer priority, LocalDate dueDate,
                                           Long createdBy) {
        // Verify case exists
        caseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("整改案例不存在: " + caseId));

        CorrectiveSubtask subtask = CorrectiveSubtask.reconstruct(CorrectiveSubtask.builder()
                .caseId(caseId)
                .subtaskName(subtaskName)
                .description(description)
                .assigneeId(assigneeId)
                .priority(priority)
                .dueDate(dueDate)
                .createdBy(createdBy)
                .status("PENDING"));
        CorrectiveSubtask saved = subtaskRepository.save(subtask);
        log.info("Created subtask for case {}: subtaskId={}, name={}", caseId, saved.getId(), subtaskName);
        return saved;
    }

    @Transactional
    public CorrectiveSubtask updateSubtask(Long subtaskId, String subtaskName, String description,
                                           Long assigneeId, Integer priority, LocalDate dueDate) {
        CorrectiveSubtask subtask = subtaskRepository.findById(subtaskId)
                .orElseThrow(() -> new IllegalArgumentException("子任务不存在: " + subtaskId));
        subtask.updateDetails(subtaskName, description, assigneeId, priority, dueDate);
        subtaskRepository.save(subtask);
        log.info("Updated subtask: id={}", subtaskId);
        return subtask;
    }

    @Transactional
    public CorrectiveSubtask startSubtask(Long subtaskId) {
        CorrectiveSubtask subtask = subtaskRepository.findById(subtaskId)
                .orElseThrow(() -> new IllegalArgumentException("子任务不存在: " + subtaskId));
        subtask.start();
        subtaskRepository.save(subtask);
        log.info("Started subtask: id={}", subtaskId);
        return subtask;
    }

    @Transactional
    public CorrectiveSubtask completeSubtask(Long subtaskId) {
        CorrectiveSubtask subtask = subtaskRepository.findById(subtaskId)
                .orElseThrow(() -> new IllegalArgumentException("子任务不存在: " + subtaskId));
        subtask.complete();
        subtaskRepository.save(subtask);
        log.info("Completed subtask: id={}", subtaskId);
        return subtask;
    }

    @Transactional
    public CorrectiveSubtask blockSubtask(Long subtaskId) {
        CorrectiveSubtask subtask = subtaskRepository.findById(subtaskId)
                .orElseThrow(() -> new IllegalArgumentException("子任务不存在: " + subtaskId));
        subtask.block();
        subtaskRepository.save(subtask);
        log.info("Blocked subtask: id={}", subtaskId);
        return subtask;
    }

    @Transactional(readOnly = true)
    public List<CorrectiveSubtask> getSubtasks(Long caseId) {
        return subtaskRepository.findByCaseId(caseId);
    }

    @Transactional
    public void deleteSubtask(Long subtaskId) {
        subtaskRepository.deleteById(subtaskId);
        log.info("Deleted subtask: id={}", subtaskId);
    }
}
