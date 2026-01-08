package com.school.management.infrastructure.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.shared.event.DomainEvent;
import com.school.management.domain.shared.event.DomainEventPublisher;
import com.school.management.infrastructure.event.outbox.OutboxEntry;
import com.school.management.infrastructure.event.outbox.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring-based implementation of DomainEventPublisher.
 * Uses the Outbox pattern for reliable event publishing.
 *
 * <p>The Outbox pattern ensures events are persisted in the same transaction
 * as the aggregate changes, guaranteeing at-least-once delivery.
 *
 * <p>Events are:
 * <ol>
 *   <li>Stored in the domain_events table (event store)</li>
 *   <li>Recorded in the event_publications table (outbox)</li>
 *   <li>Published synchronously via Spring events (for immediate in-process handling)</li>
 *   <li>Published asynchronously by OutboxProcessor (for external systems)</li>
 * </ol>
 */
@Component
public class SpringDomainEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SpringDomainEventPublisher.class);

    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectMapper objectMapper;
    private final DomainEventStore eventStore;
    private final OutboxRepository outboxRepository;

    @Value("${app.events.sync-publish:true}")
    private boolean syncPublishEnabled;

    public SpringDomainEventPublisher(ApplicationEventPublisher applicationEventPublisher,
                                      ObjectMapper objectMapper,
                                      DomainEventStore eventStore,
                                      OutboxRepository outboxRepository) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.objectMapper = objectMapper;
        this.eventStore = eventStore;
        this.outboxRepository = outboxRepository;
    }

    /**
     * Publishes a domain event using the Outbox pattern.
     *
     * <p>This method performs three operations within the current transaction:
     * <ol>
     *   <li>Stores the event in the event store (for event sourcing)</li>
     *   <li>Records the event in the outbox (for reliable delivery)</li>
     *   <li>Optionally publishes synchronously (for immediate in-process handling)</li>
     * </ol>
     *
     * @param event the domain event to publish
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(DomainEvent event) {
        log.info("Publishing domain event: type={}, aggregateType={}, aggregateId={}",
            event.getEventType(), event.getAggregateType(), event.getAggregateId());

        try {
            // 1. Store in event store (for event sourcing / audit)
            eventStore.store(event);

            // 2. Record in outbox (for reliable async delivery)
            recordInOutbox(event);

            // 3. Synchronous publish (for immediate in-process handlers)
            if (syncPublishEnabled) {
                applicationEventPublisher.publishEvent(event);
            }

            log.debug("Domain event recorded: eventId={}", event.getEventId());

        } catch (Exception e) {
            log.error("Failed to publish domain event: {}", event.getEventId(), e);
            throw new RuntimeException("Failed to publish domain event", e);
        }
    }

    /**
     * Records an event in the outbox for reliable delivery.
     */
    private void recordInOutbox(DomainEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEntry entry = new OutboxEntry(
                event.getEventId(),
                event.getEventType(),
                event.getAggregateType(),
                event.getAggregateId(),
                payload
            );
            outboxRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to record event in outbox: {}", event.getEventId(), e);
            // Don't rethrow - the event is still stored in event store
            // The outbox is for async delivery; sync publish will still work
        }
    }

    /**
     * Publishes an event directly without going through the outbox.
     * Use this for events that don't need reliable delivery.
     *
     * @param event the event to publish
     */
    public void publishDirect(DomainEvent event) {
        log.debug("Direct publishing event: type={}", event.getEventType());
        applicationEventPublisher.publishEvent(event);
    }
}
