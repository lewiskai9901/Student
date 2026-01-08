package com.school.management.application.inspection;

import com.school.management.domain.inspection.model.*;
import com.school.management.domain.inspection.repository.AppealRepository;
import com.school.management.domain.inspection.repository.InspectionRecordRepository;
import com.school.management.domain.inspection.repository.InspectionTemplateRepository;
import com.school.management.domain.shared.event.DomainEvent;
import com.school.management.domain.shared.event.DomainEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service for inspection domain operations.
 * Orchestrates domain logic and coordinates between aggregates.
 */
@Service
@Transactional
public class InspectionApplicationService {

    private final InspectionTemplateRepository templateRepository;
    private final InspectionRecordRepository recordRepository;
    private final AppealRepository appealRepository;
    private final DomainEventPublisher eventPublisher;

    public InspectionApplicationService(
            InspectionTemplateRepository templateRepository,
            InspectionRecordRepository recordRepository,
            AppealRepository appealRepository,
            DomainEventPublisher eventPublisher) {
        this.templateRepository = templateRepository;
        this.recordRepository = recordRepository;
        this.appealRepository = appealRepository;
        this.eventPublisher = eventPublisher;
    }

    // ==================== Template Operations ====================

    /**
     * Creates a new inspection template.
     */
    public InspectionTemplate createTemplate(CreateTemplateCommand command) {
        // Check for duplicate code
        if (templateRepository.existsByTemplateCode(command.getTemplateCode())) {
            throw new IllegalArgumentException("Template code already exists: " + command.getTemplateCode());
        }

        InspectionTemplate template = InspectionTemplate.create(
            command.getTemplateCode(),
            command.getTemplateName(),
            command.getDescription(),
            command.getScope(),
            command.getApplicableOrgUnitId(),
            command.getCreatedBy()
        );

        template = templateRepository.save(template);
        publishEvents(template);
        return template;
    }

    /**
     * Adds a category to a template.
     */
    public InspectionTemplate addCategoryToTemplate(Long templateId, AddCategoryCommand command) {
        InspectionTemplate template = templateRepository.findById(templateId)
            .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));

        InspectionCategory category = InspectionCategory.builder()
            .categoryCode(command.getCategoryCode())
            .categoryName(command.getCategoryName())
            .baseScore(command.getBaseScore())
            .sortOrder(command.getSortOrder())
            .build();

        template.addCategory(category);
        template = templateRepository.save(template);
        return template;
    }

    /**
     * Publishes a template for use.
     */
    public InspectionTemplate publishTemplate(Long templateId) {
        InspectionTemplate template = templateRepository.findById(templateId)
            .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));

        template.publish();
        template = templateRepository.save(template);
        return template;
    }

    /**
     * Sets a template as the default.
     */
    public InspectionTemplate setTemplateAsDefault(Long templateId) {
        InspectionTemplate template = templateRepository.findById(templateId)
            .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));

        // Clear existing default for the same scope
        templateRepository.findDefaultByScope(template.getScope())
            .ifPresent(existing -> {
                if (!existing.getId().equals(templateId)) {
                    // Mark existing as non-default (needs repository method)
                }
            });

        template.setAsDefault();
        template = templateRepository.save(template);
        return template;
    }

    /**
     * Gets a template by ID.
     */
    @Transactional(readOnly = true)
    public Optional<InspectionTemplate> getTemplate(Long templateId) {
        return templateRepository.findById(templateId);
    }

    /**
     * Lists all published templates.
     */
    @Transactional(readOnly = true)
    public List<InspectionTemplate> listPublishedTemplates() {
        return templateRepository.findByStatus(TemplateStatus.PUBLISHED);
    }

    // ==================== Record Operations ====================

    /**
     * Creates a new inspection record.
     */
    public InspectionRecord createRecord(CreateRecordCommand command) {
        // Validate template exists
        InspectionTemplate template = templateRepository.findById(command.getTemplateId())
            .orElseThrow(() -> new IllegalArgumentException("Template not found: " + command.getTemplateId()));

        if (template.getStatus() != TemplateStatus.PUBLISHED) {
            throw new IllegalStateException("Cannot create record with unpublished template");
        }

        String recordCode = generateRecordCode(command.getInspectionDate());

        InspectionRecord record = InspectionRecord.create(
            recordCode,
            command.getTemplateId(),
            template.getCurrentVersion(),
            command.getRoundId(),
            command.getInspectionDate(),
            command.getInspectionPeriod(),
            command.getInspectorId(),
            command.getInspectorName(),
            command.getCreatedBy()
        );

        record = recordRepository.save(record);
        return record;
    }

    /**
     * Adds a class score to an inspection record.
     */
    public InspectionRecord addClassScore(Long recordId, Long classId, String className, Integer baseScore) {
        InspectionRecord record = recordRepository.findById(recordId)
            .orElseThrow(() -> new IllegalArgumentException("Record not found: " + recordId));

        record.addClassScore(classId, className, baseScore);
        record = recordRepository.save(record);
        return record;
    }

    /**
     * Records a deduction for a class.
     */
    public InspectionRecord recordDeduction(Long recordId, RecordDeductionCommand command) {
        InspectionRecord record = recordRepository.findById(recordId)
            .orElseThrow(() -> new IllegalArgumentException("Record not found: " + recordId));

        record.recordDeduction(
            command.getClassId(),
            command.getDeductionItemId(),
            command.getItemName(),
            command.getCount(),
            command.getDeductionAmount(),
            command.getRemark(),
            command.getEvidenceUrls()
        );

        record = recordRepository.save(record);
        return record;
    }

    /**
     * Submits an inspection record for review.
     */
    public InspectionRecord submitRecord(Long recordId) {
        InspectionRecord record = recordRepository.findById(recordId)
            .orElseThrow(() -> new IllegalArgumentException("Record not found: " + recordId));

        record.submit();
        record = recordRepository.save(record);
        return record;
    }

    /**
     * Approves an inspection record.
     */
    public InspectionRecord approveRecord(Long recordId, Long reviewerId) {
        InspectionRecord record = recordRepository.findById(recordId)
            .orElseThrow(() -> new IllegalArgumentException("Record not found: " + recordId));

        record.approve(reviewerId);
        record = recordRepository.save(record);
        return record;
    }

    /**
     * Rejects an inspection record.
     */
    public InspectionRecord rejectRecord(Long recordId, Long reviewerId, String reason) {
        InspectionRecord record = recordRepository.findById(recordId)
            .orElseThrow(() -> new IllegalArgumentException("Record not found: " + recordId));

        record.reject(reviewerId, reason);
        record = recordRepository.save(record);
        return record;
    }

    /**
     * Publishes an inspection record.
     */
    public InspectionRecord publishRecord(Long recordId) {
        InspectionRecord record = recordRepository.findById(recordId)
            .orElseThrow(() -> new IllegalArgumentException("Record not found: " + recordId));

        record.publish();
        record = recordRepository.save(record);
        publishEvents(record);
        return record;
    }

    /**
     * Gets an inspection record by ID.
     */
    @Transactional(readOnly = true)
    public Optional<InspectionRecord> getRecord(Long recordId) {
        return recordRepository.findById(recordId);
    }

    /**
     * Lists records by date range.
     */
    @Transactional(readOnly = true)
    public List<InspectionRecord> listRecordsByDateRange(LocalDate startDate, LocalDate endDate) {
        return recordRepository.findByDateRange(startDate, endDate);
    }

    // ==================== Appeal Operations ====================

    /**
     * Creates a new appeal.
     */
    public Appeal createAppeal(CreateAppealCommand command) {
        // Validate inspection record exists
        InspectionRecord record = recordRepository.findById(command.getInspectionRecordId())
            .orElseThrow(() -> new IllegalArgumentException("Record not found: " + command.getInspectionRecordId()));

        if (record.getStatus() != RecordStatus.PUBLISHED) {
            throw new IllegalStateException("Can only appeal published records");
        }

        // Check for existing pending appeal
        if (appealRepository.existsByDeductionDetailId(command.getDeductionDetailId())) {
            throw new IllegalStateException("An appeal is already pending for this deduction");
        }

        String appealCode = generateAppealCode();

        Appeal appeal = Appeal.create(
            command.getInspectionRecordId(),
            command.getDeductionDetailId(),
            command.getClassId(),
            appealCode,
            command.getReason(),
            command.getAttachments(),
            command.getOriginalDeduction(),
            command.getRequestedDeduction(),
            command.getApplicantId()
        );

        appeal = appealRepository.save(appeal);
        return appeal;
    }

    /**
     * Starts level 1 review for an appeal.
     */
    public Appeal startLevel1Review(Long appealId, Long reviewerId) {
        Appeal appeal = appealRepository.findById(appealId)
            .orElseThrow(() -> new IllegalArgumentException("Appeal not found: " + appealId));

        appeal.startLevel1Review(reviewerId);
        appeal = appealRepository.save(appeal);
        publishEvents(appeal);
        return appeal;
    }

    /**
     * Level 1 approves an appeal.
     */
    public Appeal level1Approve(Long appealId, Long reviewerId, String comment) {
        Appeal appeal = appealRepository.findById(appealId)
            .orElseThrow(() -> new IllegalArgumentException("Appeal not found: " + appealId));

        appeal.level1Approve(reviewerId, comment);
        appeal = appealRepository.save(appeal);
        publishEvents(appeal);
        return appeal;
    }

    /**
     * Level 1 rejects an appeal.
     */
    public Appeal level1Reject(Long appealId, Long reviewerId, String comment) {
        Appeal appeal = appealRepository.findById(appealId)
            .orElseThrow(() -> new IllegalArgumentException("Appeal not found: " + appealId));

        appeal.level1Reject(reviewerId, comment);
        appeal = appealRepository.save(appeal);
        publishEvents(appeal);
        return appeal;
    }

    /**
     * Starts level 2 review for an appeal.
     */
    public Appeal startLevel2Review(Long appealId, Long reviewerId) {
        Appeal appeal = appealRepository.findById(appealId)
            .orElseThrow(() -> new IllegalArgumentException("Appeal not found: " + appealId));

        appeal.startLevel2Review(reviewerId);
        appeal = appealRepository.save(appeal);
        publishEvents(appeal);
        return appeal;
    }

    /**
     * Final approval of an appeal.
     */
    public Appeal approveAppeal(Long appealId, Long reviewerId, String comment, BigDecimal approvedDeduction) {
        Appeal appeal = appealRepository.findById(appealId)
            .orElseThrow(() -> new IllegalArgumentException("Appeal not found: " + appealId));

        appeal.approve(reviewerId, comment, approvedDeduction);
        appeal = appealRepository.save(appeal);
        publishEvents(appeal);
        return appeal;
    }

    /**
     * Final rejection of an appeal.
     */
    public Appeal rejectAppeal(Long appealId, Long reviewerId, String comment) {
        Appeal appeal = appealRepository.findById(appealId)
            .orElseThrow(() -> new IllegalArgumentException("Appeal not found: " + appealId));

        appeal.reject(reviewerId, comment);
        appeal = appealRepository.save(appeal);
        publishEvents(appeal);
        return appeal;
    }

    /**
     * Withdraws an appeal.
     */
    public Appeal withdrawAppeal(Long appealId, Long applicantId) {
        Appeal appeal = appealRepository.findById(appealId)
            .orElseThrow(() -> new IllegalArgumentException("Appeal not found: " + appealId));

        appeal.withdraw(applicantId);
        appeal = appealRepository.save(appeal);
        publishEvents(appeal);
        return appeal;
    }

    /**
     * Makes an approved appeal effective.
     */
    public Appeal makeAppealEffective(Long appealId) {
        Appeal appeal = appealRepository.findById(appealId)
            .orElseThrow(() -> new IllegalArgumentException("Appeal not found: " + appealId));

        appeal.makeEffective();

        // Update the original deduction in the inspection record
        applyAppealToRecord(appeal);

        appeal = appealRepository.save(appeal);
        publishEvents(appeal);
        return appeal;
    }

    /**
     * Gets an appeal by ID.
     */
    @Transactional(readOnly = true)
    public Optional<Appeal> getAppeal(Long appealId) {
        return appealRepository.findById(appealId);
    }

    /**
     * Lists appeals by status.
     */
    @Transactional(readOnly = true)
    public List<Appeal> listAppealsByStatus(AppealStatus status) {
        return appealRepository.findByStatus(status);
    }

    /**
     * Lists appeals for a class.
     */
    @Transactional(readOnly = true)
    public List<Appeal> listAppealsForClass(Long classId) {
        return appealRepository.findByClassId(classId);
    }

    // ==================== Private Helpers ====================

    private void publishEvents(Object aggregate) {
        List<DomainEvent> events;
        if (aggregate instanceof InspectionTemplate) {
            events = ((InspectionTemplate) aggregate).getDomainEvents();
        } else if (aggregate instanceof InspectionRecord) {
            events = ((InspectionRecord) aggregate).getDomainEvents();
        } else if (aggregate instanceof Appeal) {
            events = ((Appeal) aggregate).getDomainEvents();
        } else {
            return;
        }

        for (DomainEvent event : events) {
            eventPublisher.publish(event);
        }
    }

    private String generateRecordCode(LocalDate date) {
        return String.format("REC-%s-%s",
            date.toString().replace("-", ""),
            UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }

    private String generateAppealCode() {
        return String.format("APL-%d-%s",
            System.currentTimeMillis(),
            UUID.randomUUID().toString().substring(0, 6).toUpperCase());
    }

    private void applyAppealToRecord(Appeal appeal) {
        // This would update the deduction detail in the inspection record
        // Implementation depends on business requirements
        // For now, this is handled by the appeal becoming effective
    }
}
