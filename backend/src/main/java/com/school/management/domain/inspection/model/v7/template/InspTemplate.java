package com.school.management.domain.inspection.model.v7.template;

import com.school.management.domain.inspection.event.v7.TemplatePublishedEvent;
import com.school.management.domain.inspection.model.v7.execution.TargetType;
import com.school.management.domain.shared.AggregateRoot;

import java.time.LocalDateTime;
import java.util.List;

/**
 * V7 检查模板聚合根
 * 状态机: DRAFT → PUBLISHED → DEPRECATED → ARCHIVED
 */
public class InspTemplate extends AggregateRoot<Long> {

    private Long id;
    private String templateCode;
    private String templateName;
    private String description;
    private Long catalogId;
    private String tags;                  // JSON: ["安全","卫生"]
    private TargetType targetType;
    private Integer latestVersion;
    private TemplateStatus status;
    private Boolean isDefault;
    private Integer useCount;
    private LocalDateTime lastUsedAt;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    protected InspTemplate() {
    }

    private InspTemplate(Builder builder) {
        this.id = builder.id;
        this.templateCode = builder.templateCode;
        this.templateName = builder.templateName;
        this.description = builder.description;
        this.catalogId = builder.catalogId;
        this.tags = builder.tags;
        this.targetType = builder.targetType != null ? builder.targetType : TargetType.ORG;
        this.latestVersion = builder.latestVersion != null ? builder.latestVersion : 0;
        this.status = builder.status != null ? builder.status : TemplateStatus.DRAFT;
        this.isDefault = builder.isDefault != null ? builder.isDefault : false;
        this.useCount = builder.useCount != null ? builder.useCount : 0;
        this.lastUsedAt = builder.lastUsedAt;
        this.createdBy = builder.createdBy;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedBy = builder.updatedBy;
        this.updatedAt = builder.updatedAt;
    }

    /**
     * 创建新模板（草稿状态）
     */
    public static InspTemplate create(String templateCode, String templateName, Long createdBy) {
        return builder()
                .templateCode(templateCode)
                .templateName(templateName)
                .status(TemplateStatus.DRAFT)
                .createdBy(createdBy)
                .build();
    }

    /**
     * 从持久化数据重建
     */
    public static InspTemplate reconstruct(Builder builder) {
        return new InspTemplate(builder);
    }

    /**
     * 发布模板 — 创建不可变版本快照
     *
     * @param sections 当前所有分区
     * @param items    当前所有字段
     * @param scoringProfileSnapshot 评分配置快照JSON
     * @return 新版本快照
     */
    public TemplateVersion publish(List<TemplateSection> sections,
                                   List<TemplateItem> items,
                                   String structureSnapshot,
                                   String scoringProfileSnapshot) {
        if (this.status != TemplateStatus.DRAFT && this.status != TemplateStatus.PUBLISHED) {
            throw new IllegalStateException("只有草稿或已发布的模板才能发布新版本");
        }
        this.latestVersion = this.latestVersion + 1;
        this.status = TemplateStatus.PUBLISHED;
        this.updatedAt = LocalDateTime.now();

        TemplateVersion version = TemplateVersion.create(
                this.id, this.latestVersion, structureSnapshot, scoringProfileSnapshot, this.createdBy);

        registerEvent(new TemplatePublishedEvent(this.id, this.latestVersion, this.templateCode));
        return version;
    }

    /**
     * 弃用模板
     */
    public void deprecate() {
        if (this.status != TemplateStatus.PUBLISHED) {
            throw new IllegalStateException("只有已发布的模板才能弃用");
        }
        this.status = TemplateStatus.DEPRECATED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 归档模板
     */
    public void archive() {
        if (this.status != TemplateStatus.DEPRECATED && this.status != TemplateStatus.PUBLISHED) {
            throw new IllegalStateException("只有已发布或已弃用的模板才能归档");
        }
        this.status = TemplateStatus.ARCHIVED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新模板基本信息（仅草稿可编辑）
     */
    public void updateInfo(String templateName, String description, Long catalogId,
                           String tags, TargetType targetType, Long updatedBy) {
        if (this.status != TemplateStatus.DRAFT) {
            throw new IllegalStateException("只有草稿状态的模板才能修改");
        }
        this.templateName = templateName;
        this.description = description;
        this.catalogId = catalogId;
        this.tags = tags;
        this.targetType = targetType != null ? targetType : this.targetType;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 增加使用计数
     */
    public void incrementUseCount() {
        this.useCount = (this.useCount != null ? this.useCount : 0) + 1;
        this.lastUsedAt = LocalDateTime.now();
    }

    // Getters
    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public String getTemplateName() {
        return templateName;
    }

    public String getDescription() {
        return description;
    }

    public Long getCatalogId() {
        return catalogId;
    }

    public String getTags() {
        return tags;
    }

    public TargetType getTargetType() {
        return targetType;
    }

    public void setTargetType(TargetType targetType) {
        this.targetType = targetType;
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getLatestVersion() {
        return latestVersion;
    }

    public TemplateStatus getStatus() {
        return status;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public Integer getUseCount() {
        return useCount;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String templateCode;
        private String templateName;
        private String description;
        private Long catalogId;
        private String tags;
        private TargetType targetType;
        private Integer latestVersion;
        private TemplateStatus status;
        private Boolean isDefault;
        private Integer useCount;
        private LocalDateTime lastUsedAt;
        private Long createdBy;
        private LocalDateTime createdAt;
        private Long updatedBy;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder templateCode(String templateCode) { this.templateCode = templateCode; return this; }
        public Builder templateName(String templateName) { this.templateName = templateName; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder catalogId(Long catalogId) { this.catalogId = catalogId; return this; }
        public Builder tags(String tags) { this.tags = tags; return this; }
        public Builder targetType(TargetType targetType) { this.targetType = targetType; return this; }
        public Builder latestVersion(Integer latestVersion) { this.latestVersion = latestVersion; return this; }
        public Builder status(TemplateStatus status) { this.status = status; return this; }
        public Builder isDefault(Boolean isDefault) { this.isDefault = isDefault; return this; }
        public Builder useCount(Integer useCount) { this.useCount = useCount; return this; }
        public Builder lastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; return this; }
        public Builder createdBy(Long createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedBy(Long updatedBy) { this.updatedBy = updatedBy; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public InspTemplate build() {
            return new InspTemplate(this);
        }
    }
}
