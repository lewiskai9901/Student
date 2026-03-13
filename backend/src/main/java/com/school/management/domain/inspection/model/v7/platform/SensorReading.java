package com.school.management.domain.inspection.model.v7.platform;

import com.school.management.domain.shared.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * V7 传感器读数实体
 * 记录IoT传感器的每次读数。
 */
public class SensorReading implements Entity<Long> {

    private Long id;
    private Long sensorId;
    private BigDecimal readingValue;
    private String readingUnit;
    private LocalDateTime recordedAt;

    protected SensorReading() {
    }

    private SensorReading(Builder builder) {
        this.id = builder.id;
        this.sensorId = builder.sensorId;
        this.readingValue = builder.readingValue;
        this.readingUnit = builder.readingUnit;
        this.recordedAt = builder.recordedAt != null ? builder.recordedAt : LocalDateTime.now();
    }

    public static SensorReading create(Long sensorId, BigDecimal readingValue, String readingUnit) {
        return builder()
                .sensorId(sensorId)
                .readingValue(readingValue)
                .readingUnit(readingUnit)
                .recordedAt(LocalDateTime.now())
                .build();
    }

    public static SensorReading reconstruct(Builder builder) {
        return new SensorReading(builder);
    }

    // Getters
    @Override
    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public Long getSensorId() { return sensorId; }
    public BigDecimal getReadingValue() { return readingValue; }
    public String getReadingUnit() { return readingUnit; }
    public LocalDateTime getRecordedAt() { return recordedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long sensorId;
        private BigDecimal readingValue;
        private String readingUnit;
        private LocalDateTime recordedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder sensorId(Long sensorId) { this.sensorId = sensorId; return this; }
        public Builder readingValue(BigDecimal readingValue) { this.readingValue = readingValue; return this; }
        public Builder readingUnit(String readingUnit) { this.readingUnit = readingUnit; return this; }
        public Builder recordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; return this; }

        public SensorReading build() { return new SensorReading(this); }
    }
}
