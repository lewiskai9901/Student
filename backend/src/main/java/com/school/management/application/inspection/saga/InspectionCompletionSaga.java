package com.school.management.application.inspection.saga;

import com.school.management.domain.inspection.event.SessionPublishedEvent;
import com.school.management.domain.inspection.model.ClassInspectionRecord;
import com.school.management.domain.inspection.repository.ClassInspectionRecordRepository;
import com.school.management.infrastructure.event.DomainEventStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Inspection Completion Saga (检查完成Saga).
 *
 * Orchestrates the full business flow after an inspection session is published:
 *   1. Persist the domain event (via existing DomainEventStore)
 *   2. Load all class inspection records for the session
 *   3. Calculate class-level ratings (EXCELLENT / GOOD / PASS / FAIL)
 *   4. Send notifications to class teachers
 *
 * <p>This saga is triggered by {@link SessionPublishedEvent} and runs
 * asynchronously to avoid blocking the publishing transaction. Failures
 * in the saga do NOT roll back the session publication -- events are
 * already stored for potential retry via the outbox pattern.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InspectionCompletionSaga {

    private final ClassInspectionRecordRepository classRecordRepository;
    private final DomainEventStore eventStore;
    private final RatingCalculationStep ratingCalculationStep;
    private final NotificationStep notificationStep;

    /**
     * Listens for session-published events and starts the saga flow.
     *
     * @param event the session published domain event
     */
    @Async
    @EventListener
    public void onSessionPublished(SessionPublishedEvent event) {
        log.info("=== Saga started: Session published [sessionId={}, date={}, classRecordCount={}] ===",
                event.getSessionId(), event.getInspectionDate(), event.getClassRecordCount());

        // Step 1: Persist the event for auditing (idempotent -- SpringDomainEventPublisher
        // may have already stored it; storing again is safe since event_id is unique via UUID)
        try {
            eventStore.store(event);
        } catch (Exception e) {
            // Non-fatal: the event may already be stored by the publisher
            log.debug("Event store write skipped (likely already stored): {}", e.getMessage());
        }

        try {
            // Step 2: Load all class inspection records for this session
            List<ClassInspectionRecord> classRecords =
                    classRecordRepository.findBySessionId(event.getSessionId());

            if (classRecords.isEmpty()) {
                log.warn("Saga terminated: session {} has no class inspection records", event.getSessionId());
                return;
            }

            log.info("Saga Step 1/3: Loaded {} class inspection records", classRecords.size());

            // Step 3: Calculate ratings
            RatingCalculationResult ratingResult = ratingCalculationStep.execute(
                    event.getSessionId(), event.getInspectionDate(), classRecords);

            log.info("Saga Step 2/3: Rating calculation completed, {} classes rated",
                    ratingResult.getRatedClassCount());

            // Step 4: Send notifications
            notificationStep.execute(
                    event.getSessionId(), event.getInspectionDate(),
                    classRecords, ratingResult);

            log.info("Saga Step 3/3: Notification dispatch completed");
            log.info("=== Saga completed: Session {} processing finished ===", event.getSessionId());

        } catch (Exception e) {
            log.error("=== Saga failed: Session {} processing error ===", event.getSessionId(), e);
            // Saga failure does NOT rollback the session publication.
            // Events are stored for potential retry via the outbox processor.
        }
    }
}
