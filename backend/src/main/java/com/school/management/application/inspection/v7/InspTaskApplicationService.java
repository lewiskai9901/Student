package com.school.management.application.inspection.v7;

import com.school.management.domain.inspection.model.v7.execution.*;
import com.school.management.domain.inspection.repository.v7.InspTaskRepository;
import com.school.management.infrastructure.event.SpringDomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@RequiredArgsConstructor
@Service
public class InspTaskApplicationService {

    private final InspTaskRepository taskRepository;
    private final SpringDomainEventPublisher eventPublisher;

    // ========== Task CRUD ==========

    @Transactional
    public InspTask createTask(Long projectId, LocalDate taskDate,
                               String timeSlotCode, java.time.LocalTime timeSlotStart,
                               java.time.LocalTime timeSlotEnd) {
        String taskCode = generateTaskCode();
        InspTask task = InspTask.create(taskCode, projectId, taskDate);
        InspTask saved = taskRepository.save(task);
        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<InspTask> getTask(Long id) {
        return taskRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<InspTask> listTasksByProject(Long projectId) {
        return taskRepository.findByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public List<InspTask> listMyTasks(Long inspectorId) {
        return taskRepository.findByInspectorId(inspectorId);
    }

    @Transactional(readOnly = true)
    public List<InspTask> listAllTasks() {
        return taskRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<InspTask> listAvailableTasks() {
        return taskRepository.findAvailableTasks();
    }

    // ========== Task Lifecycle ==========

    @Transactional
    public InspTask claimTask(Long id, Long inspectorId, String inspectorName) {
        InspTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        task.claim(inspectorId, inspectorName);
        InspTask saved = taskRepository.save(task);
        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();
        return saved;
    }

    @Transactional
    public InspTask startTask(Long id) {
        InspTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        task.start();
        InspTask saved = taskRepository.save(task);
        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();
        return saved;
    }

    @Transactional
    public InspTask submitTask(Long id) {
        InspTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        task.submit();
        InspTask saved = taskRepository.save(task);
        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();
        return saved;
    }

    @Transactional
    public InspTask startReview(Long id, Long reviewerId, String reviewerName) {
        InspTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        task.startReview(reviewerId, reviewerName);
        return taskRepository.save(task);
    }

    @Transactional
    public InspTask reviewTask(Long id, String comment) {
        InspTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        task.review(comment);
        InspTask saved = taskRepository.save(task);
        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();
        return saved;
    }

    @Transactional
    public InspTask publishTask(Long id) {
        InspTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        task.publish();
        InspTask saved = taskRepository.save(task);
        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();
        return saved;
    }

    @Transactional
    public InspTask cancelTask(Long id) {
        InspTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        task.cancel();
        InspTask saved = taskRepository.save(task);
        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();
        return saved;
    }

    @Transactional
    public InspTask assignTask(Long id, Long inspectorId, String inspectorName) {
        InspTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        task.assign(inspectorId, inspectorName);
        return taskRepository.save(task);
    }

    private String generateTaskCode() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "TSK-" + dateStr + "-" + random;
    }
}
