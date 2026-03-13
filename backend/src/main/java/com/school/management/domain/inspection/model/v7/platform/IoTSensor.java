package com.school.management.domain.inspection.model.v7.platform;

import com.school.management.domain.shared.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * V7 IoT传感器实体
 * 支持温度、湿度、空气质量等传感器注册与读数记录。
 */
public class IoTSensor implements Entity<Long> {

    private Long id;
    private Long tenantId;
    private String sensorCode;
    private String sensorName;
    private String sensorType;
    private String locationName;
    private Long placeId;
    private String mqttTopic;
    private String dataUnit;
    private Boolean isActive;
    private BigDecimal lastReading;
    private LocalDateTime lastReadingAt;
    private LocalDateTime createdAt;

    protected IoTSensor() {
    }

    private IoTSensor(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId;
        this.sensorCode = builder.sensorCode;
        this.sensorName = builder.sensorName;
        this.sensorType = builder.sensorType;
        this.locationName = builder.locationName;
        this.placeId = builder.placeId;
        this.mqttTopic = builder.mqttTopic;
        this.dataUnit = builder.dataUnit;
        this.isActive = builder.isActive != null ? builder.isActive : true;
        this.lastReading = builder.lastReading;
        this.lastReadingAt = builder.lastReadingAt;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
    }

    public static IoTSensor create(String sensorCode, String sensorName, String sensorType,
                                    String locationName, Long placeId, String mqttTopic, String dataUnit) {
        return builder()
                .sensorCode(sensorCode)
                .sensorName(sensorName)
                .sensorType(sensorType)
                .locationName(locationName)
                .placeId(placeId)
                .mqttTopic(mqttTopic)
                .dataUnit(dataUnit)
                .isActive(true)
                .build();
    }

    public static IoTSensor reconstruct(Builder builder) {
        return new IoTSensor(builder);
    }

    public void updateReading(BigDecimal value) {
        this.lastReading = value;
        this.lastReadingAt = LocalDateTime.now();
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void updateInfo(String sensorName, String sensorType, String locationName,
                           Long placeId, String mqttTopic, String dataUnit) {
        this.sensorName = sensorName;
        this.sensorType = sensorType;
        this.locationName = locationName;
        this.placeId = placeId;
        this.mqttTopic = mqttTopic;
        this.dataUnit = dataUnit;
    }

    // Getters
    @Override
    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public Long getTenantId() { return tenantId; }
    public String getSensorCode() { return sensorCode; }
    public String getSensorName() { return sensorName; }
    public String getSensorType() { return sensorType; }
    public String getLocationName() { return locationName; }
    public Long getPlaceId() { return placeId; }
    public String getMqttTopic() { return mqttTopic; }
    public String getDataUnit() { return dataUnit; }
    public Boolean getIsActive() { return isActive; }
    public BigDecimal getLastReading() { return lastReading; }
    public LocalDateTime getLastReadingAt() { return lastReadingAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long tenantId;
        private String sensorCode;
        private String sensorName;
        private String sensorType;
        private String locationName;
        private Long placeId;
        private String mqttTopic;
        private String dataUnit;
        private Boolean isActive;
        private BigDecimal lastReading;
        private LocalDateTime lastReadingAt;
        private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(Long tenantId) { this.tenantId = tenantId; return this; }
        public Builder sensorCode(String sensorCode) { this.sensorCode = sensorCode; return this; }
        public Builder sensorName(String sensorName) { this.sensorName = sensorName; return this; }
        public Builder sensorType(String sensorType) { this.sensorType = sensorType; return this; }
        public Builder locationName(String locationName) { this.locationName = locationName; return this; }
        public Builder placeId(Long placeId) { this.placeId = placeId; return this; }
        public Builder mqttTopic(String mqttTopic) { this.mqttTopic = mqttTopic; return this; }
        public Builder dataUnit(String dataUnit) { this.dataUnit = dataUnit; return this; }
        public Builder isActive(Boolean isActive) { this.isActive = isActive; return this; }
        public Builder lastReading(BigDecimal lastReading) { this.lastReading = lastReading; return this; }
        public Builder lastReadingAt(LocalDateTime lastReadingAt) { this.lastReadingAt = lastReadingAt; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public IoTSensor build() { return new IoTSensor(this); }
    }
}
