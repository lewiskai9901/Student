package com.school.management.infrastructure.event.outbox;

import java.time.Instant;

/**
 * Represents an entry in the event outbox.
 * Used for reliable event publishing via the Outbox pattern.
 */
public class OutboxEntry {

    private Long id;
    private String eventId;
    private String eventType;
    private String aggregateType;
    private String aggregateId;
    private String payload;
    private OutboxStatus status;
    private int retryCount;
    private String lastError;
    private Instant createdAt;
    private Instant publishedAt;
    private Instant nextRetryAt;

    public OutboxEntry() {
    }

    public OutboxEntry(String eventId, String eventType, String aggregateType,
                       String aggregateId, String payload) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.payload = payload;
        this.status = OutboxStatus.PENDING;
        this.retryCount = 0;
        this.createdAt = Instant.now();
    }

    public enum OutboxStatus {
        PENDING,
        PUBLISHED,
        FAILED
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public void setAggregateType(String aggregateType) {
        this.aggregateType = aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(String aggregateId) {
        this.aggregateId = aggregateId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public void setStatus(OutboxStatus status) {
        this.status = status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Instant getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(Instant nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    /**
     * Marks this entry as published.
     */
    public void markPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = Instant.now();
    }

    /**
     * Marks this entry as failed with retry scheduling.
     */
    public void markFailed(String error, int maxRetries) {
        this.retryCount++;
        this.lastError = error;
        if (this.retryCount >= maxRetries) {
            this.status = OutboxStatus.FAILED;
        } else {
            // Exponential backoff: 1s, 2s, 4s, 8s, etc.
            long delaySeconds = (long) Math.pow(2, retryCount);
            this.nextRetryAt = Instant.now().plusSeconds(delaySeconds);
        }
    }

    /**
     * Checks if this entry can be retried.
     */
    public boolean canRetry() {
        return status == OutboxStatus.PENDING
            && (nextRetryAt == null || Instant.now().isAfter(nextRetryAt));
    }
}
