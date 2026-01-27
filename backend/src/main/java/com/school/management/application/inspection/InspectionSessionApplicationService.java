package com.school.management.application.inspection;

import com.school.management.application.inspection.command.*;
import com.school.management.domain.inspection.model.*;
import com.school.management.domain.inspection.repository.ClassInspectionRecordRepository;
import com.school.management.domain.inspection.repository.InspectionSessionRepository;
import com.school.management.domain.inspection.repository.InspectionTemplateRepository;
import com.school.management.domain.inspection.service.ClassAllocation;
import com.school.management.domain.inspection.service.SpaceToOrgResolver;
import com.school.management.domain.inspection.service.StudentOrgInfo;
import com.school.management.domain.shared.event.DomainEvent;
import com.school.management.domain.shared.event.DomainEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Application service for V4 inspection session operations.
 * Orchestrates domain logic for session-based inspection workflows.
 */
@Service
@Transactional
public class InspectionSessionApplicationService {

    private final InspectionSessionRepository sessionRepository;
    private final ClassInspectionRecordRepository classRecordRepository;
    private final InspectionTemplateRepository templateRepository;
    private final SpaceToOrgResolver spaceToOrgResolver;
    private final DomainEventPublisher eventPublisher;

    public InspectionSessionApplicationService(
            InspectionSessionRepository sessionRepository,
            ClassInspectionRecordRepository classRecordRepository,
            InspectionTemplateRepository templateRepository,
            SpaceToOrgResolver spaceToOrgResolver,
            DomainEventPublisher eventPublisher) {
        this.sessionRepository = sessionRepository;
        this.classRecordRepository = classRecordRepository;
        this.templateRepository = templateRepository;
        this.spaceToOrgResolver = spaceToOrgResolver;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Creates a new inspection session.
     */
    public InspectionSession createSession(CreateSessionCommand command) {
        // Validate template exists and is published
        InspectionTemplate template = templateRepository.findById(command.getTemplateId())
            .orElseThrow(() -> new IllegalArgumentException("Template not found: " + command.getTemplateId()));

        if (template.getStatus() != TemplateStatus.PUBLISHED) {
            throw new IllegalStateException("Cannot create session with unpublished template");
        }

        String sessionCode = generateSessionCode(command.getInspectionDate());

        InspectionSession session = InspectionSession.create(
            sessionCode,
            command.getTemplateId(),
            template.getCurrentVersion(),
            command.getInspectionDate(),
            command.getInspectionPeriod(),
            command.getInputMode() != null ? command.getInputMode() : InputMode.SPACE_FIRST,
            command.getScoringMode() != null ? command.getScoringMode() : ScoringMode.DEDUCTION_ONLY,
            command.getBaseScore() != null ? command.getBaseScore() : 100,
            command.getInspectorId(),
            command.getInspectorName(),
            command.getCreatedBy()
        );

        session = sessionRepository.save(session);
        return session;
    }

    /**
     * Records a space-based deduction. Resolves physical space to class(es) automatically.
     */
    public List<ClassInspectionRecord> recordSpaceDeduction(SpaceDeductionCommand command) {
        InspectionSession session = getSessionOrThrow(command.getSessionId());
        ensureSessionEditable(session);

        // Auto-start session if still CREATED
        if (session.getStatus() == SessionStatus.CREATED) {
            session.startInspection();
            sessionRepository.save(session);
        }

        // Resolve space to class allocations
        List<ClassAllocation> allocations;
        if (command.getSpaceType() == SpaceType.DORMITORY) {
            allocations = spaceToOrgResolver.resolveDormitory(command.getSpaceId());
        } else if (command.getSpaceType() == SpaceType.CLASSROOM) {
            allocations = spaceToOrgResolver.resolveClassroom(command.getSpaceId());
        } else {
            throw new IllegalArgumentException("Space type is required for space-based deduction");
        }

        if (allocations.isEmpty()) {
            throw new IllegalStateException("No classes found for space: " + command.getSpaceName());
        }

        List<ClassInspectionRecord> updatedRecords = new ArrayList<>();

        for (ClassAllocation allocation : allocations) {
            // Get or create class record
            ClassInspectionRecord classRecord = getOrCreateClassRecord(
                session, allocation.getClassId(), allocation.getClassName(),
                allocation.getOrgUnitId(), allocation.getOrgUnitName()
            );

            // Calculate proportional deduction if multiple classes share the space
            BigDecimal deduction = allocations.size() == 1
                ? command.getDeductionAmount()
                : command.getDeductionAmount().multiply(allocation.getRatio());

            InspectionDeduction deductionEntity = InspectionDeduction.builder()
                .sessionId(session.getId())
                .classRecordId(classRecord.getId())
                .deductionItemId(command.getDeductionItemId())
                .itemName(command.getItemName())
                .categoryName(command.getCategoryName())
                .spaceType(command.getSpaceType())
                .spaceId(command.getSpaceId())
                .spaceName(command.getSpaceName())
                .studentIds(command.getStudentIds())
                .studentNames(command.getStudentNames())
                .personCount(command.getPersonCount() != null ? command.getPersonCount() : 0)
                .deductionAmount(deduction)
                .inputSource(InputSource.SPACE_RESOLVED)
                .remark(command.getRemark())
                .evidenceUrls(command.getEvidenceUrls())
                .build();

            classRecord.addDeduction(deductionEntity);
            classRecordRepository.save(classRecord);
            updatedRecords.add(classRecord);
        }

        return updatedRecords;
    }

    /**
     * Records a person-based deduction. Resolves students to their class automatically.
     */
    public List<ClassInspectionRecord> recordPersonDeduction(PersonDeductionCommand command) {
        InspectionSession session = getSessionOrThrow(command.getSessionId());
        ensureSessionEditable(session);

        if (session.getStatus() == SessionStatus.CREATED) {
            session.startInspection();
            sessionRepository.save(session);
        }

        // Resolve students to their classes
        List<StudentOrgInfo> studentInfos = spaceToOrgResolver.resolveStudentBatch(command.getStudentIds());
        if (studentInfos.isEmpty()) {
            throw new IllegalStateException("No valid students found");
        }

        // Group students by class
        Map<Long, List<StudentOrgInfo>> byClass = studentInfos.stream()
            .filter(s -> s.getClassId() != null)
            .collect(Collectors.groupingBy(StudentOrgInfo::getClassId));

        List<ClassInspectionRecord> updatedRecords = new ArrayList<>();

        for (Map.Entry<Long, List<StudentOrgInfo>> entry : byClass.entrySet()) {
            List<StudentOrgInfo> classStudents = entry.getValue();
            StudentOrgInfo first = classStudents.get(0);

            ClassInspectionRecord classRecord = getOrCreateClassRecord(
                session, first.getClassId(), first.getClassName(),
                first.getOrgUnitId(), first.getOrgUnitName()
            );

            List<Long> sIds = classStudents.stream().map(StudentOrgInfo::getStudentId).collect(Collectors.toList());
            List<String> sNames = classStudents.stream().map(StudentOrgInfo::getStudentName).collect(Collectors.toList());

            InspectionDeduction deduction = InspectionDeduction.builder()
                .sessionId(session.getId())
                .classRecordId(classRecord.getId())
                .deductionItemId(command.getDeductionItemId())
                .itemName(command.getItemName())
                .categoryName(command.getCategoryName())
                .spaceType(SpaceType.NONE)
                .studentIds(sIds)
                .studentNames(sNames)
                .personCount(classStudents.size())
                .deductionAmount(command.getDeductionAmount())
                .inputSource(InputSource.PERSON_RESOLVED)
                .remark(command.getRemark())
                .evidenceUrls(command.getEvidenceUrls())
                .build();

            classRecord.addDeduction(deduction);
            classRecordRepository.save(classRecord);
            updatedRecords.add(classRecord);
        }

        return updatedRecords;
    }

    /**
     * Batch submits checklist responses for a class.
     */
    public ClassInspectionRecord submitChecklistResponses(BatchChecklistResponseCommand command) {
        InspectionSession session = getSessionOrThrow(command.getSessionId());
        ensureSessionEditable(session);

        if (session.getStatus() == SessionStatus.CREATED) {
            session.startInspection();
            sessionRepository.save(session);
        }

        // Get or create class record - we need classId from the command
        ClassInspectionRecord classRecord = classRecordRepository
            .findBySessionIdAndClassId(command.getSessionId(), command.getClassId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Class record not found for session " + command.getSessionId() + " and class " + command.getClassId()));

        for (BatchChecklistResponseCommand.ChecklistItem item : command.getItems()) {
            ChecklistResponse response = ChecklistResponse.builder()
                .sessionId(session.getId())
                .classRecordId(classRecord.getId())
                .checklistItemId(item.getChecklistItemId())
                .itemName(item.getItemName())
                .categoryName(item.getCategoryName())
                .result(item.getResult())
                .autoDeduction(item.getResult() == ChecklistResult.FAIL && item.getDeductionWhenFail() != null
                    ? item.getDeductionWhenFail() : BigDecimal.ZERO)
                .inspectorNote(item.getInspectorNote())
                .build();

            classRecord.addOrUpdateChecklistResponse(response);
        }

        classRecordRepository.save(classRecord);
        return classRecord;
    }

    /**
     * Submits the session.
     */
    public InspectionSession submitSession(Long sessionId) {
        InspectionSession session = getSessionOrThrow(sessionId);
        int classRecordCount = classRecordRepository.countBySessionId(sessionId);

        session.submit(classRecordCount);
        session = sessionRepository.save(session);
        publishEvents(session);
        return session;
    }

    /**
     * Publishes the session.
     */
    public InspectionSession publishSession(Long sessionId) {
        InspectionSession session = getSessionOrThrow(sessionId);
        int classRecordCount = classRecordRepository.countBySessionId(sessionId);

        // Complete all class records
        List<ClassInspectionRecord> records = classRecordRepository.findBySessionId(sessionId);
        for (ClassInspectionRecord record : records) {
            record.complete();
            classRecordRepository.save(record);
        }

        session.publish(classRecordCount);
        session = sessionRepository.save(session);
        publishEvents(session);
        return session;
    }

    /**
     * Gets a session by ID.
     */
    @Transactional(readOnly = true)
    public Optional<InspectionSession> getSession(Long sessionId) {
        return sessionRepository.findById(sessionId);
    }

    /**
     * Gets all class records for a session.
     */
    @Transactional(readOnly = true)
    public List<ClassInspectionRecord> getSessionClassRecords(Long sessionId) {
        return classRecordRepository.findBySessionId(sessionId);
    }

    /**
     * Gets checklist progress for a session.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getChecklistProgress(Long sessionId) {
        List<ClassInspectionRecord> records = classRecordRepository.findBySessionId(sessionId);

        int totalClasses = records.size();
        long completedClasses = records.stream()
            .filter(r -> r.getStatus() == ClassRecordStatus.COMPLETED)
            .count();
        int totalResponses = records.stream()
            .mapToInt(r -> r.getChecklistResponses().size())
            .sum();

        Map<String, Object> progress = new HashMap<>();
        progress.put("totalClasses", totalClasses);
        progress.put("completedClasses", completedClasses);
        progress.put("totalChecklistResponses", totalResponses);
        return progress;
    }

    /**
     * Lists sessions by date range.
     */
    @Transactional(readOnly = true)
    public List<InspectionSession> listSessionsByDateRange(LocalDate startDate, LocalDate endDate) {
        return sessionRepository.findByDateRange(startDate, endDate);
    }

    // ==================== Private Helpers ====================

    private ClassInspectionRecord getOrCreateClassRecord(InspectionSession session,
                                                          Long classId, String className,
                                                          Long orgUnitId, String orgUnitName) {
        return classRecordRepository.findBySessionIdAndClassId(session.getId(), classId)
            .orElseGet(() -> {
                ClassInspectionRecord newRecord = ClassInspectionRecord.create(
                    session.getId(), classId, className,
                    orgUnitId, orgUnitName, session.getBaseScore()
                );
                return classRecordRepository.save(newRecord);
            });
    }

    private InspectionSession getSessionOrThrow(Long sessionId) {
        return sessionRepository.findById(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
    }

    private void ensureSessionEditable(InspectionSession session) {
        SessionStatus status = session.getStatus();
        if (status == SessionStatus.SUBMITTED || status == SessionStatus.PUBLISHED) {
            throw new IllegalStateException("Cannot modify session in status: " + status);
        }
    }

    private void publishEvents(InspectionSession session) {
        for (DomainEvent event : session.getDomainEvents()) {
            eventPublisher.publish(event);
        }
        session.clearDomainEvents();
    }

    private String generateSessionCode(LocalDate date) {
        return String.format("SES-%s-%s",
            date.toString().replace("-", ""),
            UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}
