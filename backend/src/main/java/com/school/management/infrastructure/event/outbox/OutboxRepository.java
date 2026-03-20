package com.school.management.infrastructure.event.outbox;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * Repository for managing outbox entries.
 * Provides CRUD operations for the event_publications table.
 */
@Slf4j
@Repository
public class OutboxRepository {

    private final JdbcTemplate jdbcTemplate;

    public OutboxRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Saves a new outbox entry.
     *
     * @param entry the outbox entry to save
     */
    public void save(OutboxEntry entry) {
        String sql = """
            INSERT INTO event_publications
            (event_id, event_type, status, retry_count, created_at)
            VALUES (?, ?, ?, ?, ?)
            """;

        jdbcTemplate.update(sql,
            entry.getEventId(),
            entry.getEventType(),
            entry.getStatus().name(),
            entry.getRetryCount(),
            Timestamp.from(entry.getCreatedAt())
        );

        log.debug("Saved outbox entry: eventId={}", entry.getEventId());
    }

    /**
     * Updates an existing outbox entry.
     *
     * @param entry the outbox entry to update
     */
    public void update(OutboxEntry entry) {
        String sql = """
            UPDATE event_publications
            SET status = ?, retry_count = ?, last_error = ?,
                published_at = ?, next_retry_at = ?
            WHERE event_id = ?
            """;

        jdbcTemplate.update(sql,
            entry.getStatus().name(),
            entry.getRetryCount(),
            entry.getLastError(),
            entry.getPublishedAt() != null ? Timestamp.from(entry.getPublishedAt()) : null,
            entry.getNextRetryAt() != null ? Timestamp.from(entry.getNextRetryAt()) : null,
            entry.getEventId()
        );
    }

    /**
     * Finds all pending entries that are ready for publishing.
     *
     * @param limit maximum number of entries to return
     * @return list of pending outbox entries
     */
    public List<OutboxEntry> findPendingEntries(int limit) {
        String sql = """
            SELECT id, event_id, event_type, status, retry_count,
                   last_error, created_at, published_at, next_retry_at
            FROM event_publications
            WHERE status = 'PENDING'
              AND (next_retry_at IS NULL OR next_retry_at <= ?)
            ORDER BY created_at ASC
            LIMIT ?
            """;

        return jdbcTemplate.query(sql, new OutboxEntryRowMapper(),
            Timestamp.from(Instant.now()), limit);
    }

    /**
     * Finds entries by status.
     *
     * @param status the status to filter by
     * @param limit  maximum number of entries
     * @return list of outbox entries
     */
    public List<OutboxEntry> findByStatus(OutboxEntry.OutboxStatus status, int limit) {
        String sql = """
            SELECT id, event_id, event_type, status, retry_count,
                   last_error, created_at, published_at, next_retry_at
            FROM event_publications
            WHERE status = ?
            ORDER BY created_at ASC
            LIMIT ?
            """;

        return jdbcTemplate.query(sql, new OutboxEntryRowMapper(), status.name(), limit);
    }

    /**
     * Deletes published entries older than the specified time.
     *
     * @param olderThan entries published before this time will be deleted
     * @return number of entries deleted
     */
    public int deletePublishedBefore(Instant olderThan) {
        String sql = """
            DELETE FROM event_publications
            WHERE status = 'PUBLISHED' AND published_at < ?
            """;

        int deleted = jdbcTemplate.update(sql, Timestamp.from(olderThan));
        log.info("Deleted {} published outbox entries older than {}", deleted, olderThan);
        return deleted;
    }

    /**
     * Counts entries by status.
     *
     * @param status the status to count
     * @return count of entries
     */
    public long countByStatus(OutboxEntry.OutboxStatus status) {
        String sql = "SELECT COUNT(*) FROM event_publications WHERE status = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, status.name());
        return count != null ? count : 0L;
    }

    private static class OutboxEntryRowMapper implements RowMapper<OutboxEntry> {
        @Override
        public OutboxEntry mapRow(ResultSet rs, int rowNum) throws SQLException {
            OutboxEntry entry = new OutboxEntry();
            entry.setId(rs.getLong("id"));
            entry.setEventId(rs.getString("event_id"));
            entry.setEventType(rs.getString("event_type"));
            entry.setStatus(OutboxEntry.OutboxStatus.valueOf(rs.getString("status")));
            entry.setRetryCount(rs.getInt("retry_count"));
            entry.setLastError(rs.getString("last_error"));
            entry.setCreatedAt(rs.getTimestamp("created_at").toInstant());

            Timestamp publishedAt = rs.getTimestamp("published_at");
            if (publishedAt != null) {
                entry.setPublishedAt(publishedAt.toInstant());
            }

            Timestamp nextRetryAt = rs.getTimestamp("next_retry_at");
            if (nextRetryAt != null) {
                entry.setNextRetryAt(nextRetryAt.toInstant());
            }

            return entry;
        }
    }
}
