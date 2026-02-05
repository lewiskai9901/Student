package com.school.management.domain.inspection.model.v6;

import com.school.management.domain.shared.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * V6检查证据实体
 * 存储检查过程中的照片、视频等证据附件
 */
public class InspectionEvidence implements Entity<Long> {

    private Long id;
    private Long detailId;      // 关联的检查明细ID
    private Long targetId;      // 关联的检查目标ID（整体证据）

    // 文件信息
    private String fileName;
    private String filePath;
    private String fileUrl;
    private Long fileSize;
    private String fileType;

    // GPS元数据
    private BigDecimal latitude;
    private BigDecimal longitude;

    // 审计
    private Long uploadBy;
    private LocalDateTime uploadTime;
    private LocalDateTime createdAt;

    protected InspectionEvidence() {
    }

    private InspectionEvidence(Builder builder) {
        this.id = builder.id;
        this.detailId = builder.detailId;
        this.targetId = builder.targetId;
        this.fileName = builder.fileName;
        this.filePath = builder.filePath;
        this.fileUrl = builder.fileUrl;
        this.fileSize = builder.fileSize;
        this.fileType = builder.fileType;
        this.latitude = builder.latitude;
        this.longitude = builder.longitude;
        this.uploadBy = builder.uploadBy;
        this.uploadTime = builder.uploadTime != null ? builder.uploadTime : LocalDateTime.now();
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
    }

    /**
     * 创建明细关联的证据
     */
    public static InspectionEvidence createForDetail(Long detailId, String fileName, String filePath,
                                                       String fileUrl, Long fileSize, String fileType,
                                                       Long uploadBy) {
        return builder()
                .detailId(detailId)
                .fileName(fileName)
                .filePath(filePath)
                .fileUrl(fileUrl)
                .fileSize(fileSize)
                .fileType(fileType)
                .uploadBy(uploadBy)
                .build();
    }

    /**
     * 创建目标关联的证据（整体证据）
     */
    public static InspectionEvidence createForTarget(Long targetId, String fileName, String filePath,
                                                       String fileUrl, Long fileSize, String fileType,
                                                       Long uploadBy) {
        return builder()
                .targetId(targetId)
                .fileName(fileName)
                .filePath(filePath)
                .fileUrl(fileUrl)
                .fileSize(fileSize)
                .fileType(fileType)
                .uploadBy(uploadBy)
                .build();
    }

    /**
     * 创建带GPS的证据
     */
    public static InspectionEvidence createWithLocation(Long detailId, Long targetId, String fileName,
                                                          String filePath, String fileUrl, Long fileSize,
                                                          String fileType, BigDecimal latitude,
                                                          BigDecimal longitude, Long uploadBy) {
        return builder()
                .detailId(detailId)
                .targetId(targetId)
                .fileName(fileName)
                .filePath(filePath)
                .fileUrl(fileUrl)
                .fileSize(fileSize)
                .fileType(fileType)
                .latitude(latitude)
                .longitude(longitude)
                .uploadBy(uploadBy)
                .build();
    }

    // Getters
    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDetailId() {
        return detailId;
    }

    public Long getTargetId() {
        return targetId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public String getFileType() {
        return fileType;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public Long getUploadBy() {
        return uploadBy;
    }

    public LocalDateTime getUploadTime() {
        return uploadTime;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long detailId;
        private Long targetId;
        private String fileName;
        private String filePath;
        private String fileUrl;
        private Long fileSize;
        private String fileType;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private Long uploadBy;
        private LocalDateTime uploadTime;
        private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder detailId(Long detailId) { this.detailId = detailId; return this; }
        public Builder targetId(Long targetId) { this.targetId = targetId; return this; }
        public Builder fileName(String fileName) { this.fileName = fileName; return this; }
        public Builder filePath(String filePath) { this.filePath = filePath; return this; }
        public Builder fileUrl(String fileUrl) { this.fileUrl = fileUrl; return this; }
        public Builder fileSize(Long fileSize) { this.fileSize = fileSize; return this; }
        public Builder fileType(String fileType) { this.fileType = fileType; return this; }
        public Builder latitude(BigDecimal latitude) { this.latitude = latitude; return this; }
        public Builder longitude(BigDecimal longitude) { this.longitude = longitude; return this; }
        public Builder uploadBy(Long uploadBy) { this.uploadBy = uploadBy; return this; }
        public Builder uploadTime(LocalDateTime uploadTime) { this.uploadTime = uploadTime; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public InspectionEvidence build() {
            return new InspectionEvidence(this);
        }
    }
}
