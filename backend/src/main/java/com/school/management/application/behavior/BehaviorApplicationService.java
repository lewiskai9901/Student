package com.school.management.application.behavior;

import com.school.management.application.behavior.command.CreateBehaviorRecordCommand;
import com.school.management.application.behavior.query.BehaviorProfileDTO;
import com.school.management.domain.behavior.model.*;
import com.school.management.domain.behavior.repository.BehaviorAlertRepository;
import com.school.management.domain.behavior.repository.BehaviorRecordRepository;
import com.school.management.domain.shared.event.DomainEvent;
import com.school.management.domain.shared.event.DomainEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BehaviorApplicationService {

    private final BehaviorRecordRepository recordRepository;
    private final BehaviorAlertRepository alertRepository;
    private final DomainEventPublisher eventPublisher;

    public BehaviorApplicationService(
            BehaviorRecordRepository recordRepository,
            BehaviorAlertRepository alertRepository,
            DomainEventPublisher eventPublisher) {
        this.recordRepository = recordRepository;
        this.alertRepository = alertRepository;
        this.eventPublisher = eventPublisher;
    }

    public BehaviorRecord createRecord(CreateBehaviorRecordCommand command) {
        BehaviorRecord record;
        if (command.getBehaviorType() == BehaviorType.VIOLATION) {
            record = BehaviorRecord.createViolation(
                    command.getStudentId(), command.getClassId(),
                    command.getSource(), command.getSourceId(),
                    command.getCategory(), command.getTitle(),
                    command.getDeductionAmount(), command.getRecordedBy()
            );
        } else {
            record = BehaviorRecord.createCommendation(
                    command.getStudentId(), command.getClassId(),
                    command.getSource(), command.getSourceId(),
                    command.getCategory(), command.getTitle(),
                    command.getDeductionAmount(), command.getRecordedBy()
            );
        }

        if (command.getDetail() != null) {
            // Detail set through builder in reconstruct; for creation, we handle via factory
        }

        record = recordRepository.save(record);
        publishEvents(record);
        return record;
    }

    public BehaviorRecord createFromInspection(Long studentId, Long classId, Long sourceId,
                                                BehaviorCategory category, String title,
                                                BigDecimal amount, Long recordedBy) {
        CreateBehaviorRecordCommand command = CreateBehaviorRecordCommand.builder()
                .studentId(studentId)
                .classId(classId)
                .behaviorType(BehaviorType.VIOLATION)
                .source(BehaviorSource.INSPECTION)
                .sourceId(sourceId)
                .category(category)
                .title(title)
                .deductionAmount(amount)
                .recordedBy(recordedBy)
                .build();
        return createRecord(command);
    }

    public BehaviorRecord notifyStudent(Long recordId) {
        BehaviorRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("Record not found: " + recordId));
        record.notify_();
        record = recordRepository.save(record);
        publishEvents(record);
        return record;
    }

    public BehaviorRecord acknowledge(Long recordId) {
        BehaviorRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("Record not found: " + recordId));
        record.acknowledge();
        record = recordRepository.save(record);
        return record;
    }

    public BehaviorRecord resolve(Long recordId, String note) {
        BehaviorRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("Record not found: " + recordId));
        record.resolve(note);
        record = recordRepository.save(record);
        return record;
    }

    public BehaviorAlert checkAndGenerateAlerts(Long studentId, Long classId) {
        long violationCount = recordRepository.countByStudentIdAndType(studentId, BehaviorType.VIOLATION);

        // Frequency alert: more than 3 violations
        if (violationCount >= 3) {
            String alertLevel = violationCount >= 5 ? "DANGER" : "WARNING";
            BehaviorAlert alert = BehaviorAlert.builder()
                    .studentId(studentId)
                    .classId(classId)
                    .alertType(AlertType.FREQUENCY)
                    .alertLevel(alertLevel)
                    .title(String.format("学生累计违规 %d 次", violationCount))
                    .description(String.format("该学生已累计违规 %d 次，请关注", violationCount))
                    .triggerData(String.format("{\"violationCount\":%d}", violationCount))
                    .build();
            alert = alertRepository.save(alert);
            publishAlertEvent(alert);
            return alert;
        }
        return null;
    }

    @Transactional(readOnly = true)
    public BehaviorProfileDTO getStudentProfile(Long studentId) {
        long totalViolations = recordRepository.countByStudentIdAndType(studentId, BehaviorType.VIOLATION);
        long totalCommendations = recordRepository.countByStudentIdAndType(studentId, BehaviorType.COMMENDATION);

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<BehaviorRecord> recentRecords = recordRepository.findByStudentIdAndDateRange(
                studentId, thirtyDaysAgo, LocalDateTime.now());
        long recentViolations = recentRecords.stream()
                .filter(r -> r.getBehaviorType() == BehaviorType.VIOLATION)
                .count();

        String riskLevel;
        if (recentViolations >= 5) {
            riskLevel = "HIGH";
        } else if (recentViolations >= 3) {
            riskLevel = "MEDIUM";
        } else {
            riskLevel = "LOW";
        }

        String trend;
        if (recentViolations > totalViolations / 2) {
            trend = "WORSENING";
        } else if (recentViolations == 0) {
            trend = "IMPROVING";
        } else {
            trend = "STABLE";
        }

        return BehaviorProfileDTO.builder()
                .studentId(studentId)
                .totalViolations(totalViolations)
                .totalCommendations(totalCommendations)
                .recentViolations(recentViolations)
                .riskLevel(riskLevel)
                .trend(trend)
                .build();
    }

    @Transactional(readOnly = true)
    public Optional<BehaviorRecord> getRecord(Long recordId) {
        return recordRepository.findById(recordId);
    }

    @Transactional(readOnly = true)
    public List<BehaviorRecord> listByStudentId(Long studentId) {
        return recordRepository.findByStudentId(studentId);
    }

    @Transactional(readOnly = true)
    public List<BehaviorRecord> listByClassId(Long classId) {
        return recordRepository.findByClassId(classId);
    }

    // ==================== Alert Operations ====================

    @Transactional(readOnly = true)
    public List<BehaviorAlert> listAlertsByStudentId(Long studentId) {
        return alertRepository.findByStudentId(studentId);
    }

    @Transactional(readOnly = true)
    public List<BehaviorAlert> listAlertsByClassId(Long classId) {
        return alertRepository.findByClassId(classId);
    }

    @Transactional(readOnly = true)
    public List<BehaviorAlert> listUnhandledAlertsByClassId(Long classId) {
        return alertRepository.findUnhandledByClassId(classId);
    }

    @Transactional(readOnly = true)
    public long countUnhandledAlerts(Long classId) {
        return alertRepository.countUnhandledByClassId(classId);
    }

    public BehaviorAlert markAlertRead(Long alertId) {
        BehaviorAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));
        alert.markRead();
        return alertRepository.save(alert);
    }

    public BehaviorAlert handleAlert(Long alertId, Long handledBy, String note) {
        BehaviorAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));
        alert.handle(handledBy, note);
        return alertRepository.save(alert);
    }

    // ==================== Private Helpers ====================

    private void publishEvents(BehaviorRecord record) {
        for (DomainEvent event : record.getDomainEvents()) {
            eventPublisher.publish(event);
        }
        record.clearDomainEvents();
    }

    private void publishAlertEvent(BehaviorAlert alert) {
        com.school.management.domain.behavior.event.AlertTriggeredEvent event =
                new com.school.management.domain.behavior.event.AlertTriggeredEvent(
                        alert.getId(), alert.getStudentId(), alert.getClassId(),
                        alert.getAlertType().name(), alert.getAlertLevel(), alert.getTitle());
        eventPublisher.publish(event);
    }
}
