package com.school.management.infrastructure.event.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.inspection.event.v7.*;
import com.school.management.domain.shared.event.DomainEvent;
import com.school.management.infrastructure.event.DomainEventStore;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Processes pending outbox entries and publishes them as domain events.
 * Implements the Outbox pattern for reliable event publishing.
 *
 * <p>Features:
 * <ul>
 *   <li>Scheduled processing of pending events</li>
 *   <li>Exponential backoff on failures</li>
 *   <li>Automatic cleanup of old published entries</li>
 *   <li>Dead letter handling for permanently failed events</li>
 * </ul>
 */
@Slf4j
@Component
public class OutboxProcessor {

    private final OutboxRepository outboxRepository;
    private final DomainEventStore eventStore;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectMapper objectMapper;

    // Event type to class mapping for deserialization
    private final Map<String, Class<? extends DomainEvent>> eventTypeRegistry = new ConcurrentHashMap<>();

    @Value("${app.outbox.batch-size:100}")
    private int batchSize;

    @Value("${app.outbox.max-retries:5}")
    private int maxRetries;

    @Value("${app.outbox.cleanup-days:7}")
    private int cleanupDays;

    public OutboxProcessor(OutboxRepository outboxRepository,
                          DomainEventStore eventStore,
                          ApplicationEventPublisher applicationEventPublisher,
                          ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.eventStore = eventStore;
        this.applicationEventPublisher = applicationEventPublisher;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void registerV7EventTypes() {
        // Template events
        registerEventType("TemplatePublishedEvent", TemplatePublishedEvent.class);
        // Project events
        registerEventType("ProjectPublishedEvent", ProjectPublishedEvent.class);
        registerEventType("ProjectPausedEvent", ProjectPausedEvent.class);
        registerEventType("ProjectResumedEvent", ProjectResumedEvent.class);
        registerEventType("ProjectCompletedEvent", ProjectCompletedEvent.class);
        // Task events
        registerEventType("TaskCreatedEvent", TaskCreatedEvent.class);
        registerEventType("TaskClaimedEvent", TaskClaimedEvent.class);
        registerEventType("TaskStartedEvent", TaskStartedEvent.class);
        registerEventType("TaskSubmittedEvent", TaskSubmittedEvent.class);
        registerEventType("TaskReviewedEvent", TaskReviewedEvent.class);
        registerEventType("TaskPublishedEvent", TaskPublishedEvent.class);
        registerEventType("TaskCancelledEvent", TaskCancelledEvent.class);
        registerEventType("TaskExpiredEvent", TaskExpiredEvent.class);
        // Submission events
        registerEventType("SubmissionCompletedEvent", SubmissionCompletedEvent.class);
        // Corrective events
        registerEventType("CorrectiveCaseCreatedEvent", CorrectiveCaseCreatedEvent.class);
        registerEventType("CaseAssignedEvent", CaseAssignedEvent.class);
        registerEventType("CorrectionSubmittedEvent", CorrectionSubmittedEvent.class);
        registerEventType("CaseVerifiedEvent", CaseVerifiedEvent.class);
        registerEventType("CaseRejectedEvent", CaseRejectedEvent.class);
        registerEventType("CaseEscalatedEvent", CaseEscalatedEvent.class);
        registerEventType("CaseClosedEvent", CaseClosedEvent.class);
        registerEventType("EffectivenessConfirmedEvent", EffectivenessConfirmedEvent.class);
        registerEventType("EffectivenessFailedEvent", EffectivenessFailedEvent.class);
        registerEventType("SlaBreachedEvent", SlaBreachedEvent.class);
        // Analytics events
        registerEventType("DailySummaryUpdatedEvent", DailySummaryUpdatedEvent.class);
        registerEventType("PeriodSummaryCalculatedEvent", PeriodSummaryCalculatedEvent.class);

        log.info("Registered {} V7 event types in OutboxProcessor", eventTypeRegistry.size());
    }

    /**
     * Registers an event type for deserialization.
     *
     * @param eventType  the event type name
     * @param eventClass the event class
     */
    public void registerEventType(String eventType, Class<? extends DomainEvent> eventClass) {
        eventTypeRegistry.put(eventType, eventClass);
        log.debug("Registered event type: {} -> {}", eventType, eventClass.getName());
    }

    /**
     * Processes pending outbox entries.
     * Runs every 5 seconds by default.
     */
    @Scheduled(fixedDelayString = "${app.outbox.process-interval:5000}")
    public void processPendingEvents() {
        List<OutboxEntry> pendingEntries = outboxRepository.findPendingEntries(batchSize);

        if (pendingEntries.isEmpty()) {
            return;
        }

        log.debug("Processing {} pending outbox entries", pendingEntries.size());

        for (OutboxEntry entry : pendingEntries) {
            processEntry(entry);
        }
    }

    /**
     * Cleans up old published entries.
     * Runs daily at midnight.
     */
    @Scheduled(cron = "${app.outbox.cleanup-cron:0 0 0 * * ?}")
    public void cleanupOldEntries() {
        Instant cutoff = Instant.now().minus(cleanupDays, ChronoUnit.DAYS);
        int deleted = outboxRepository.deletePublishedBefore(cutoff);
        log.info("Cleaned up {} old outbox entries", deleted);
    }

    /**
     * Processes a single outbox entry.
     *
     * @param entry the entry to process
     */
    private void processEntry(OutboxEntry entry) {
        try {
            // Get the stored event from event store
            DomainEventStore.StoredEvent storedEvent = findStoredEvent(entry.getEventId());

            if (storedEvent != null) {
                // Deserialize and publish
                DomainEvent event = deserializeEvent(storedEvent);
                if (event != null) {
                    applicationEventPublisher.publishEvent(event);
                    log.info("Published event from outbox: type={}, eventId={}",
                        entry.getEventType(), entry.getEventId());
                }
            }

            // Mark as published
            entry.markPublished();
            outboxRepository.update(entry);

        } catch (Exception e) {
            log.error("Failed to process outbox entry: eventId={}", entry.getEventId(), e);
            entry.markFailed(e.getMessage(), maxRetries);
            outboxRepository.update(entry);

            if (entry.getStatus() == OutboxEntry.OutboxStatus.FAILED) {
                handleDeadLetter(entry, e);
            }
        }
    }

    /**
     * Finds the stored event by event ID.
     */
    private DomainEventStore.StoredEvent findStoredEvent(String eventId) {
        try {
            return eventStore.findByEventId(eventId).orElse(null);
        } catch (Exception e) {
            log.warn("Failed to find stored event: eventId={}, error={}", eventId, e.getMessage());
            return null;
        }
    }

    /**
     * Deserializes a stored event to a domain event.
     */
    private DomainEvent deserializeEvent(DomainEventStore.StoredEvent storedEvent) {
        Class<? extends DomainEvent> eventClass = eventTypeRegistry.get(storedEvent.getEventType());
        if (eventClass == null) {
            log.warn("Unknown event type: {}", storedEvent.getEventType());
            return null;
        }

        try {
            return objectMapper.readValue(storedEvent.getPayload(), eventClass);
        } catch (Exception e) {
            log.error("Failed to deserialize event: {}", storedEvent.getEventId(), e);
            return null;
        }
    }

    /**
     * Handles a permanently failed event (dead letter).
     */
    private void handleDeadLetter(OutboxEntry entry, Exception e) {
        log.error("Event moved to dead letter: eventId={}, type={}, error={}",
            entry.getEventId(), entry.getEventType(), e.getMessage());

        // In production, you might want to:
        // 1. Send an alert
        // 2. Store in a dead letter queue for manual processing
        // 3. Trigger a notification
    }

    /**
     * Gets statistics about the outbox.
     */
    public OutboxStats getStats() {
        return new OutboxStats(
            outboxRepository.countByStatus(OutboxEntry.OutboxStatus.PENDING),
            outboxRepository.countByStatus(OutboxEntry.OutboxStatus.PUBLISHED),
            outboxRepository.countByStatus(OutboxEntry.OutboxStatus.FAILED)
        );
    }

    /**
     * Outbox statistics.
     */
    public record OutboxStats(long pending, long published, long failed) {
    }
}
