package com.school.management.domain.inspection.model.v7.platform;

import com.school.management.domain.shared.Entity;

import java.time.LocalDateTime;

/**
 * V7 NFC标签实体
 * 用于检查人员到场打卡验证。
 */
public class NfcTag implements Entity<Long> {

    private Long id;
    private Long tenantId;
    private String tagUid;
    private String locationName;
    private Long placeId;
    private Long orgUnitId;
    private Boolean isActive;
    private LocalDateTime createdAt;

    protected NfcTag() {
    }

    private NfcTag(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId;
        this.tagUid = builder.tagUid;
        this.locationName = builder.locationName;
        this.placeId = builder.placeId;
        this.orgUnitId = builder.orgUnitId;
        this.isActive = builder.isActive != null ? builder.isActive : true;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
    }

    public static NfcTag create(String tagUid, String locationName, Long placeId, Long orgUnitId) {
        return builder()
                .tagUid(tagUid)
                .locationName(locationName)
                .placeId(placeId)
                .orgUnitId(orgUnitId)
                .isActive(true)
                .build();
    }

    public static NfcTag reconstruct(Builder builder) {
        return new NfcTag(builder);
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void updateLocation(String locationName, Long placeId, Long orgUnitId) {
        this.locationName = locationName;
        this.placeId = placeId;
        this.orgUnitId = orgUnitId;
    }

    // Getters
    @Override
    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public Long getTenantId() { return tenantId; }
    public String getTagUid() { return tagUid; }
    public String getLocationName() { return locationName; }
    public Long getPlaceId() { return placeId; }
    public Long getOrgUnitId() { return orgUnitId; }
    public Boolean getIsActive() { return isActive; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long tenantId;
        private String tagUid;
        private String locationName;
        private Long placeId;
        private Long orgUnitId;
        private Boolean isActive;
        private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(Long tenantId) { this.tenantId = tenantId; return this; }
        public Builder tagUid(String tagUid) { this.tagUid = tagUid; return this; }
        public Builder locationName(String locationName) { this.locationName = locationName; return this; }
        public Builder placeId(Long placeId) { this.placeId = placeId; return this; }
        public Builder orgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; return this; }
        public Builder isActive(Boolean isActive) { this.isActive = isActive; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public NfcTag build() { return new NfcTag(this); }
    }
}
