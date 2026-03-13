package com.school.management.domain.inspection.model.v7.execution;

import com.school.management.domain.shared.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * V7 证据文件
 */
public class InspEvidence implements Entity<Long> {

    private Long id;
    private Long tenantId;
    private Long submissionId;
    private Long detailId;
    private EvidenceType evidenceType;
    private String fileName;
    private String filePath;
    private String fileUrl;
    private Long fileSize;
    private String mimeType;
    private String thumbnailUrl;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private LocalDateTime capturedAt;
    private String metadata;
    private String aiAnalysis;          // JSON: AI分析结果
    private BigDecimal aiConfidence;    // AI置信度 0.0~1.0
    private LocalDateTime createdAt;

    protected InspEvidence() {
    }

    private InspEvidence(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId;
        this.submissionId = builder.submissionId;
        this.detailId = builder.detailId;
        this.evidenceType = builder.evidenceType;
        this.fileName = builder.fileName;
        this.filePath = builder.filePath;
        this.fileUrl = builder.fileUrl;
        this.fileSize = builder.fileSize;
        this.mimeType = builder.mimeType;
        this.thumbnailUrl = builder.thumbnailUrl;
        this.latitude = builder.latitude;
        this.longitude = builder.longitude;
        this.capturedAt = builder.capturedAt;
        this.metadata = builder.metadata;
        this.aiAnalysis = builder.aiAnalysis;
        this.aiConfidence = builder.aiConfidence;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
    }

    public static InspEvidence create(Long submissionId, EvidenceType evidenceType,
                                      String fileName, String fileUrl) {
        return builder()
                .submissionId(submissionId)
                .evidenceType(evidenceType)
                .fileName(fileName)
                .fileUrl(fileUrl)
                .build();
    }

    public static InspEvidence reconstruct(Builder builder) {
        return new InspEvidence(builder);
    }

    // Getters
    @Override
    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public Long getTenantId() { return tenantId; }
    public Long getSubmissionId() { return submissionId; }
    public Long getDetailId() { return detailId; }
    public EvidenceType getEvidenceType() { return evidenceType; }
    public String getFileName() { return fileName; }
    public String getFilePath() { return filePath; }
    public String getFileUrl() { return fileUrl; }
    public Long getFileSize() { return fileSize; }
    public String getMimeType() { return mimeType; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public BigDecimal getLatitude() { return latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public LocalDateTime getCapturedAt() { return capturedAt; }
    public String getMetadata() { return metadata; }
    public String getAiAnalysis() { return aiAnalysis; }
    public BigDecimal getAiConfidence() { return aiConfidence; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long tenantId;
        private Long submissionId;
        private Long detailId;
        private EvidenceType evidenceType;
        private String fileName;
        private String filePath;
        private String fileUrl;
        private Long fileSize;
        private String mimeType;
        private String thumbnailUrl;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private LocalDateTime capturedAt;
        private String metadata;
        private String aiAnalysis;
        private BigDecimal aiConfidence;
        private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(Long tenantId) { this.tenantId = tenantId; return this; }
        public Builder submissionId(Long submissionId) { this.submissionId = submissionId; return this; }
        public Builder detailId(Long detailId) { this.detailId = detailId; return this; }
        public Builder evidenceType(EvidenceType evidenceType) { this.evidenceType = evidenceType; return this; }
        public Builder fileName(String fileName) { this.fileName = fileName; return this; }
        public Builder filePath(String filePath) { this.filePath = filePath; return this; }
        public Builder fileUrl(String fileUrl) { this.fileUrl = fileUrl; return this; }
        public Builder fileSize(Long fileSize) { this.fileSize = fileSize; return this; }
        public Builder mimeType(String mimeType) { this.mimeType = mimeType; return this; }
        public Builder thumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; return this; }
        public Builder latitude(BigDecimal latitude) { this.latitude = latitude; return this; }
        public Builder longitude(BigDecimal longitude) { this.longitude = longitude; return this; }
        public Builder capturedAt(LocalDateTime capturedAt) { this.capturedAt = capturedAt; return this; }
        public Builder metadata(String metadata) { this.metadata = metadata; return this; }
        public Builder aiAnalysis(String aiAnalysis) { this.aiAnalysis = aiAnalysis; return this; }
        public Builder aiConfidence(BigDecimal aiConfidence) { this.aiConfidence = aiConfidence; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public InspEvidence build() { return new InspEvidence(this); }
    }
}
