package com.school.management.domain.inspection.model.v7.execution;

import com.school.management.domain.shared.AggregateRoot;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * V7 违规记录聚合根
 * 记录检查提交中发现的违规行为，关联到具体的提交明细、分区、条目和违规用户。
 */
public class ViolationRecord extends AggregateRoot<Long> {

    private Long id;
    private Long tenantId;
    private Long submissionId;
    private Long submissionDetailId;
    private Long sectionId;
    private Long itemId;
    private Long userId;
    private String userName;
    private String classInfo;
    private LocalDateTime occurredAt;
    private String severity;       // MINOR / MODERATE / SEVERE
    private String description;
    private String evidenceUrls;   // JSON
    private BigDecimal score;      // 扣分值
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected ViolationRecord() {
    }

    private ViolationRecord(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId;
        this.submissionId = builder.submissionId;
        this.submissionDetailId = builder.submissionDetailId;
        this.sectionId = builder.sectionId;
        this.itemId = builder.itemId;
        this.userId = builder.userId;
        this.userName = builder.userName;
        this.classInfo = builder.classInfo;
        this.occurredAt = builder.occurredAt != null ? builder.occurredAt : LocalDateTime.now();
        this.severity = builder.severity != null ? builder.severity : "MINOR";
        this.description = builder.description;
        this.evidenceUrls = builder.evidenceUrls;
        this.score = builder.score;
        this.createdBy = builder.createdBy;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedAt = builder.updatedAt;
    }

    public static ViolationRecord create(Long submissionId, Long submissionDetailId,
                                          Long sectionId, Long itemId,
                                          Long userId, String userName, String classInfo,
                                          LocalDateTime occurredAt, String severity,
                                          String description, BigDecimal score, Long createdBy) {
        return builder()
                .submissionId(submissionId)
                .submissionDetailId(submissionDetailId)
                .sectionId(sectionId)
                .itemId(itemId)
                .userId(userId)
                .userName(userName)
                .classInfo(classInfo)
                .occurredAt(occurredAt)
                .severity(severity != null ? severity : "MINOR")
                .description(description)
                .score(score)
                .createdBy(createdBy)
                .build();
    }

    public static ViolationRecord reconstruct(Builder builder) {
        return new ViolationRecord(builder);
    }

    public void update(String description, String severity, BigDecimal score,
                       String evidenceUrls, String classInfo) {
        this.description = description;
        this.severity = severity;
        this.score = score;
        this.evidenceUrls = evidenceUrls;
        this.classInfo = classInfo;
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTenantId() { return tenantId; }
    public Long getSubmissionId() { return submissionId; }
    public Long getSubmissionDetailId() { return submissionDetailId; }
    public Long getSectionId() { return sectionId; }
    public Long getItemId() { return itemId; }
    public Long getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getClassInfo() { return classInfo; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public String getSeverity() { return severity; }
    public String getDescription() { return description; }
    public String getEvidenceUrls() { return evidenceUrls; }
    public BigDecimal getScore() { return score; }
    public Long getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long tenantId;
        private Long submissionId;
        private Long submissionDetailId;
        private Long sectionId;
        private Long itemId;
        private Long userId;
        private String userName;
        private String classInfo;
        private LocalDateTime occurredAt;
        private String severity;
        private String description;
        private String evidenceUrls;
        private BigDecimal score;
        private Long createdBy;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(Long tenantId) { this.tenantId = tenantId; return this; }
        public Builder submissionId(Long submissionId) { this.submissionId = submissionId; return this; }
        public Builder submissionDetailId(Long submissionDetailId) { this.submissionDetailId = submissionDetailId; return this; }
        public Builder sectionId(Long sectionId) { this.sectionId = sectionId; return this; }
        public Builder itemId(Long itemId) { this.itemId = itemId; return this; }
        public Builder userId(Long userId) { this.userId = userId; return this; }
        public Builder userName(String userName) { this.userName = userName; return this; }
        public Builder classInfo(String classInfo) { this.classInfo = classInfo; return this; }
        public Builder occurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; return this; }
        public Builder severity(String severity) { this.severity = severity; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder evidenceUrls(String evidenceUrls) { this.evidenceUrls = evidenceUrls; return this; }
        public Builder score(BigDecimal score) { this.score = score; return this; }
        public Builder createdBy(Long createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public ViolationRecord build() { return new ViolationRecord(this); }
    }
}
