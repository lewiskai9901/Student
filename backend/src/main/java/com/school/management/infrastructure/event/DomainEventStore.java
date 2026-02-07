package com.school.management.infrastructure.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.shared.event.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Stores and retrieves domain events for event sourcing.
 * Provides both write (store) and read (query) capabilities.
 */
@Component
public class DomainEventStore {

    private static final Logger log = LoggerFactory.getLogger(DomainEventStore.class);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public DomainEventStore(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Stores a domain event in the database.
     *
     * @param event the event to store
     */
    public void store(DomainEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            String sql = """
                INSERT INTO domain_events
                (event_id, event_type, aggregate_type, aggregate_id, aggregate_version,
                 payload, occurred_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

            jdbcTemplate.update(sql,
                event.getEventId(),
                event.getEventType(),
                event.getAggregateType(),
                event.getAggregateId(),
                1, // aggregate version - simplified for now
                payload,
                Timestamp.from(event.getOccurredAt())
            );

            log.debug("Stored domain event: {}", event.getEventId());
        } catch (Exception e) {
            log.error("Failed to store domain event: {}", event.getEventId(), e);
            // For now, we just log the error but don't fail the operation
            // In production, you might want to use a transactional outbox pattern
        }
    }

    /**
     * Retrieves all events for a specific aggregate.
     *
     * @param aggregateType the aggregate type
     * @param aggregateId   the aggregate ID
     * @return list of stored events in chronological order
     */
    public List<StoredEvent> findByAggregate(String aggregateType, String aggregateId) {
        String sql = """
            SELECT id, event_id, event_type, aggregate_type, aggregate_id,
                   aggregate_version, payload, metadata, occurred_at, created_at
            FROM domain_events
            WHERE aggregate_type = ? AND aggregate_id = ?
            ORDER BY id ASC
            """;

        return jdbcTemplate.query(sql, new StoredEventRowMapper(), aggregateType, aggregateId);
    }

    /**
     * Retrieves events for an aggregate starting from a specific version.
     *
     * @param aggregateType the aggregate type
     * @param aggregateId   the aggregate ID
     * @param fromVersion   minimum version (inclusive)
     * @return list of stored events from that version
     */
    public List<StoredEvent> findByAggregateFromVersion(String aggregateType, String aggregateId, int fromVersion) {
        String sql = """
            SELECT id, event_id, event_type, aggregate_type, aggregate_id,
                   aggregate_version, payload, metadata, occurred_at, created_at
            FROM domain_events
            WHERE aggregate_type = ? AND aggregate_id = ? AND aggregate_version >= ?
            ORDER BY id ASC
            """;

        return jdbcTemplate.query(sql, new StoredEventRowMapper(), aggregateType, aggregateId, fromVersion);
    }

    /**
     * Retrieves all events of a specific type.
     *
     * @param eventType the event type class name
     * @return list of stored events
     */
    public List<StoredEvent> findByEventType(String eventType) {
        String sql = """
            SELECT id, event_id, event_type, aggregate_type, aggregate_id,
                   aggregate_version, payload, metadata, occurred_at, created_at
            FROM domain_events
            WHERE event_type = ?
            ORDER BY id ASC
            """;

        return jdbcTemplate.query(sql, new StoredEventRowMapper(), eventType);
    }

    /**
     * Retrieves events occurred within a time range.
     *
     * @param from start time (inclusive)
     * @param to   end time (inclusive)
     * @return list of stored events
     */
    public List<StoredEvent> findByTimeRange(Instant from, Instant to) {
        String sql = """
            SELECT id, event_id, event_type, aggregate_type, aggregate_id,
                   aggregate_version, payload, metadata, occurred_at, created_at
            FROM domain_events
            WHERE occurred_at >= ? AND occurred_at <= ?
            ORDER BY id ASC
            """;

        return jdbcTemplate.query(sql, new StoredEventRowMapper(),
                Timestamp.from(from), Timestamp.from(to));
    }

    /**
     * Retrieves the latest event for an aggregate.
     *
     * @param aggregateType the aggregate type
     * @param aggregateId   the aggregate ID
     * @return the latest event, if any
     */
    public Optional<StoredEvent> findLatestByAggregate(String aggregateType, String aggregateId) {
        String sql = """
            SELECT id, event_id, event_type, aggregate_type, aggregate_id,
                   aggregate_version, payload, metadata, occurred_at, created_at
            FROM domain_events
            WHERE aggregate_type = ? AND aggregate_id = ?
            ORDER BY id DESC
            LIMIT 1
            """;

        List<StoredEvent> events = jdbcTemplate.query(sql, new StoredEventRowMapper(), aggregateType, aggregateId);
        return events.isEmpty() ? Optional.empty() : Optional.of(events.get(0));
    }

    /**
     * Counts events for an aggregate.
     *
     * @param aggregateType the aggregate type
     * @param aggregateId   the aggregate ID
     * @return event count
     */
    public long countByAggregate(String aggregateType, String aggregateId) {
        String sql = "SELECT COUNT(*) FROM domain_events WHERE aggregate_type = ? AND aggregate_id = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, aggregateType, aggregateId);
        return count != null ? count : 0L;
    }

    /**
     * Retrieves all events with pagination.
     *
     * @param offset starting offset
     * @param limit  max number of events
     * @return list of stored events
     */
    public List<StoredEvent> findAll(int offset, int limit) {
        String sql = """
            SELECT id, event_id, event_type, aggregate_type, aggregate_id,
                   aggregate_version, payload, metadata, occurred_at, created_at
            FROM domain_events
            ORDER BY id ASC
            LIMIT ? OFFSET ?
            """;

        return jdbcTemplate.query(sql, new StoredEventRowMapper(), limit, offset);
    }

    /**
     * Retrieves a stored event by its event ID.
     *
     * @param eventId the event ID
     * @return the stored event, if found
     */
    public Optional<StoredEvent> findByEventId(String eventId) {
        String sql = """
            SELECT id, event_id, event_type, aggregate_type, aggregate_id,
                   aggregate_version, payload, metadata, occurred_at, created_at
            FROM domain_events
            WHERE event_id = ?
            LIMIT 1
            """;

        List<StoredEvent> events = jdbcTemplate.query(sql, new StoredEventRowMapper(), eventId);
        return events.isEmpty() ? Optional.empty() : Optional.of(events.get(0));
    }

    /**
     * Deserializes a stored event payload to a domain event.
     *
     * @param storedEvent the stored event
     * @param eventClass  the target event class
     * @param <T>         event type
     * @return the deserialized event
     */
    public <T extends DomainEvent> T deserialize(StoredEvent storedEvent, Class<T> eventClass) {
        try {
            return objectMapper.readValue(storedEvent.getPayload(), eventClass);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize event: " + storedEvent.getEventId(), e);
        }
    }

    /**
     * Stored event record containing event metadata and payload.
     */
    public record StoredEvent(
            Long id,
            String eventId,
            String eventType,
            String aggregateType,
            String aggregateId,
            int aggregateVersion,
            String payload,
            String metadata,
            Instant occurredAt,
            LocalDateTime createdAt
    ) {
        public String getEventId() { return eventId; }
        public String getEventType() { return eventType; }
        public String getAggregateType() { return aggregateType; }
        public String getAggregateId() { return aggregateId; }
        public int getAggregateVersion() { return aggregateVersion; }
        public String getPayload() { return payload; }
        public Instant getOccurredAt() { return occurredAt; }
    }

    private static class StoredEventRowMapper implements RowMapper<StoredEvent> {
        @Override
        public StoredEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new StoredEvent(
                    rs.getLong("id"),
                    rs.getString("event_id"),
                    rs.getString("event_type"),
                    rs.getString("aggregate_type"),
                    rs.getString("aggregate_id"),
                    rs.getInt("aggregate_version"),
                    rs.getString("payload"),
                    rs.getString("metadata"),
                    rs.getTimestamp("occurred_at") != null ? rs.getTimestamp("occurred_at").toInstant() : Instant.now(),
                    rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : LocalDateTime.now()
            );
        }
    }
}
