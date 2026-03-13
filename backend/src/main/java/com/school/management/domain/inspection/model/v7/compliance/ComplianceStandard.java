package com.school.management.domain.inspection.model.v7.compliance;

import com.school.management.domain.shared.AggregateRoot;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 合规标准聚合根
 */
public class ComplianceStandard extends AggregateRoot<Long> {

    private Long id;
    private String standardCode;
    private String standardName;
    private String issuingBody;
    private LocalDate effectiveDate;
    private String standardVersion;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected ComplianceStandard() {
    }

    private ComplianceStandard(Builder builder) {
        this.id = builder.id;
        this.standardCode = builder.standardCode;
        this.standardName = builder.standardName;
        this.issuingBody = builder.issuingBody;
        this.effectiveDate = builder.effectiveDate;
        this.standardVersion = builder.standardVersion;
        this.description = builder.description;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedAt = builder.updatedAt != null ? builder.updatedAt : LocalDateTime.now();
    }

    public static ComplianceStandard create(String code, String name, Long createdBy) {
        return builder()
                .standardCode(code)
                .standardName(name)
                .build();
    }

    public void update(String standardName, String issuingBody, LocalDate effectiveDate,
                       String version, String description) {
        this.standardName = standardName;
        this.issuingBody = issuingBody;
        this.effectiveDate = effectiveDate;
        this.standardVersion = version;
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    public static ComplianceStandard reconstruct(Builder builder) {
        return new ComplianceStandard(builder);
    }

    @Override
    public Long getId() { return id; }

    @Override
    public void setId(Long id) { this.id = id; }

    public String getStandardCode() { return standardCode; }
    public String getStandardName() { return standardName; }
    public String getIssuingBody() { return issuingBody; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public String getStandardVersion() { return standardVersion; }
    public String getDescription() { return description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String standardCode;
        private String standardName;
        private String issuingBody;
        private LocalDate effectiveDate;
        private String standardVersion;
        private String description;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder standardCode(String standardCode) { this.standardCode = standardCode; return this; }
        public Builder standardName(String standardName) { this.standardName = standardName; return this; }
        public Builder issuingBody(String issuingBody) { this.issuingBody = issuingBody; return this; }
        public Builder effectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; return this; }
        public Builder standardVersion(String standardVersion) { this.standardVersion = standardVersion; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public ComplianceStandard build() { return new ComplianceStandard(this); }
    }
}
