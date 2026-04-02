package com.school.management.domain.inspection.model.v7.template;

import com.school.management.domain.inspection.event.v7.TemplatePublishedEvent;
import com.school.management.domain.inspection.model.v7.execution.TargetType;
import com.school.management.domain.shared.AggregateRoot;

import java.time.LocalDateTime;
import java.util.List;

/**
 * V7 统一分区模型（聚合根）
 * 根分区（parentSectionId=null, templateId=null）即原来的模板。
 * 子分区组织内容，一级子分区可设 targetType。
 * 引用分区（refSectionId != null）为只读快捷方式。
 *
 * 状态机（仅根分区）: DRAFT → PUBLISHED → DEPRECATED → ARCHIVED
 */
public class TemplateSection extends AggregateRoot<Long> {

    private Long id;
    private Long tenantId;
    private Long templateId;           // null = 根分区（自己就是根）
    private Long parentSectionId;      // null = 顶层
    private Long refSectionId;         // 引用的分区ID（只读快捷方式）
    private String sectionCode;
    private String sectionName;
    private String description;
    private String tags;               // JSON: ["安全","卫生"]
    private Long catalogId;
    private TargetType targetType;     // 仅一级分区可设: ORG/PLACE/USER
    private String targetSourceMode;   // INDEPENDENT / PARENT_ASSOCIATED (nullable)
    private String targetTypeFilter;   // e.g. "org_type=班级" or "place_category=宿舍" (nullable)
    private TemplateStatus status;     // 仅根分区有效
    private Integer latestVersion;     // 仅根分区有效
    private Integer sortOrder;
    private Boolean isRepeatable;
    private String conditionLogic;     // JSON
    private String scoringConfig;      // JSON: {aggregation, maxScore, minScore, gradeBands[]}
    private String inputMode;         // INLINE / EVENT_STREAM
    private String inspectionMode;    // SNAPSHOT / CONTINUOUS
    private String continuousStart;   // HH:mm
    private String continuousEnd;     // HH:mm
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    protected TemplateSection() {
    }

    private TemplateSection(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId;
        this.templateId = builder.templateId;
        this.parentSectionId = builder.parentSectionId;
        this.refSectionId = builder.refSectionId;
        this.sectionCode = builder.sectionCode;
        this.sectionName = builder.sectionName;
        this.description = builder.description;
        this.tags = builder.tags;
        this.catalogId = builder.catalogId;
        this.targetType = builder.targetType;
        this.status = builder.status != null ? builder.status : TemplateStatus.DRAFT;
        this.latestVersion = builder.latestVersion != null ? builder.latestVersion : 0;
        this.sortOrder = builder.sortOrder != null ? builder.sortOrder : 0;
        this.isRepeatable = builder.isRepeatable != null ? builder.isRepeatable : false;
        this.conditionLogic = builder.conditionLogic;
        this.scoringConfig = builder.scoringConfig;
        this.inputMode = builder.inputMode != null ? builder.inputMode : "INLINE";
        this.inspectionMode = builder.inspectionMode != null ? builder.inspectionMode : "SNAPSHOT";
        this.targetSourceMode = builder.targetSourceMode;
        this.targetTypeFilter = builder.targetTypeFilter;
        this.continuousStart = builder.continuousStart;
        this.continuousEnd = builder.continuousEnd;
        this.createdBy = builder.createdBy;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedBy = builder.updatedBy;
        this.updatedAt = builder.updatedAt;
    }

    /**
     * 创建根分区（即原来的"模板"）
     */
    public static TemplateSection createRoot(String sectionCode, String sectionName, Long createdBy) {
        return builder()
                .sectionCode(sectionCode)
                .sectionName(sectionName)
                .status(TemplateStatus.DRAFT)
                .createdBy(createdBy)
                .build();
    }

    /**
     * 创建子分区
     */
    public static TemplateSection createChild(Long parentSectionId, String sectionCode,
                                               String sectionName, Long createdBy) {
        return builder()
                .parentSectionId(parentSectionId)
                .sectionCode(sectionCode)
                .sectionName(sectionName)
                .createdBy(createdBy)
                .build();
    }

    public static TemplateSection reconstruct(Builder builder) {
        return new TemplateSection(builder);
    }

    // ========== 根分区操作（模板级） ==========

    /**
     * 发布 — 创建不可变版本快照
     */
    public TemplateVersion publish(List<TemplateSection> allSections,
                                    List<TemplateItem> allItems,
                                    String structureSnapshot,
                                    String scoringProfileSnapshot) {
        if (!isRoot()) {
            throw new IllegalStateException("只有根分区才能发布");
        }
        if (this.status != TemplateStatus.DRAFT && this.status != TemplateStatus.PUBLISHED) {
            throw new IllegalStateException("只有草稿或已发布的分区才能发布新版本");
        }
        this.latestVersion = this.latestVersion + 1;
        this.status = TemplateStatus.PUBLISHED;
        this.updatedAt = LocalDateTime.now();

        TemplateVersion version = TemplateVersion.create(
                this.id, this.latestVersion, structureSnapshot, scoringProfileSnapshot, this.createdBy);
        registerEvent(new TemplatePublishedEvent(this.id, this.latestVersion, this.sectionCode));
        return version;
    }

    public void deprecate() {
        if (!isRoot()) throw new IllegalStateException("只有根分区才能弃用");
        if (this.status != TemplateStatus.PUBLISHED) throw new IllegalStateException("只有已发布的才能弃用");
        this.status = TemplateStatus.DEPRECATED;
        this.updatedAt = LocalDateTime.now();
    }

    public void archive() {
        if (!isRoot()) throw new IllegalStateException("只有根分区才能归档");
        if (this.status != TemplateStatus.DEPRECATED && this.status != TemplateStatus.PUBLISHED)
            throw new IllegalStateException("只有已发布或已弃用的才能归档");
        this.status = TemplateStatus.ARCHIVED;
        this.updatedAt = LocalDateTime.now();
    }

    // ========== 通用操作 ==========

    public void updateInfo(String sectionName, String description, String tags,
                           Long catalogId, Long updatedBy) {
        this.sectionName = sectionName;
        if (description != null) this.description = description;
        if (tags != null) this.tags = tags;
        if (catalogId != null) this.catalogId = catalogId;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    public void update(String sectionName,
                       Boolean isRepeatable, String conditionLogic, Long updatedBy) {
        this.sectionName = sectionName;
        this.isRepeatable = isRepeatable;
        this.conditionLogic = conditionLogic;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    public void setStatus(TemplateStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public void setTargetType(TargetType targetType) {
        this.targetType = targetType;
        this.updatedAt = LocalDateTime.now();
    }

    public void setInputMode(String inputMode) {
        this.inputMode = inputMode;
        this.updatedAt = LocalDateTime.now();
    }

    public void setTargetSourceMode(String targetSourceMode) {
        this.targetSourceMode = targetSourceMode;
        this.updatedAt = LocalDateTime.now();
    }

    public void setTargetTypeFilter(String targetTypeFilter) {
        this.targetTypeFilter = targetTypeFilter;
        this.updatedAt = LocalDateTime.now();
    }

    public void reorder(Integer sortOrder) {
        this.sortOrder = sortOrder;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateScoringConfig(String scoringConfig, Long updatedBy) {
        this.scoringConfig = scoringConfig;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }


    // ========== 查询方法 ==========

    public boolean isRoot() {
        return templateId == null && parentSectionId == null;
    }

    public boolean isFirstLevel() {
        // 一级分区 = parent 是根分区
        return parentSectionId != null && templateId == null;
    }

    // ========== Getters ==========

    @Override
    public Long getId() { return id; }

    @Override
    public void setId(Long id) { this.id = id; }

    public Long getTenantId() { return tenantId; }
    public Long getTemplateId() { return templateId; }
    public Long getParentSectionId() { return parentSectionId; }
    public Long getRefSectionId() { return refSectionId; }
    public String getSectionCode() { return sectionCode; }
    public String getSectionName() { return sectionName; }
    public String getDescription() { return description; }
    public String getTags() { return tags; }
    public Long getCatalogId() { return catalogId; }
    public TargetType getTargetType() { return targetType; }
    public String getTargetSourceMode() { return targetSourceMode; }
    public String getTargetTypeFilter() { return targetTypeFilter; }
    public TemplateStatus getStatus() { return status; }
    public Integer getLatestVersion() { return latestVersion; }
    public Integer getSortOrder() { return sortOrder; }
    public Boolean getIsRepeatable() { return isRepeatable; }
    public String getConditionLogic() { return conditionLogic; }
    public String getScoringConfig() { return scoringConfig; }
    public String getInputMode() { return inputMode; }
    public String getInspectionMode() { return inspectionMode; }
    public String getContinuousStart() { return continuousStart; }
    public String getContinuousEnd() { return continuousEnd; }
    public Long getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Long getUpdatedBy() { return updatedBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long tenantId;
        private Long templateId;
        private Long parentSectionId;
        private Long refSectionId;
        private String sectionCode;
        private String sectionName;
        private String description;
        private String tags;
        private Long catalogId;
        private TargetType targetType;
        private String targetSourceMode;
        private String targetTypeFilter;
        private TemplateStatus status;
        private Integer latestVersion;
        private Integer sortOrder;
        private Boolean isRepeatable;
        private String conditionLogic;
        private String scoringConfig;
        private String inputMode;
        private String inspectionMode;
        private String continuousStart;
        private String continuousEnd;
        private Long createdBy;
        private LocalDateTime createdAt;
        private Long updatedBy;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(Long tenantId) { this.tenantId = tenantId; return this; }
        public Builder templateId(Long templateId) { this.templateId = templateId; return this; }
        public Builder parentSectionId(Long parentSectionId) { this.parentSectionId = parentSectionId; return this; }
        public Builder refSectionId(Long refSectionId) { this.refSectionId = refSectionId; return this; }
        public Builder sectionCode(String sectionCode) { this.sectionCode = sectionCode; return this; }
        public Builder sectionName(String sectionName) { this.sectionName = sectionName; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder tags(String tags) { this.tags = tags; return this; }
        public Builder catalogId(Long catalogId) { this.catalogId = catalogId; return this; }
        public Builder targetType(TargetType targetType) { this.targetType = targetType; return this; }
        public Builder targetSourceMode(String targetSourceMode) { this.targetSourceMode = targetSourceMode; return this; }
        public Builder targetTypeFilter(String targetTypeFilter) { this.targetTypeFilter = targetTypeFilter; return this; }
        public Builder status(TemplateStatus status) { this.status = status; return this; }
        public Builder latestVersion(Integer latestVersion) { this.latestVersion = latestVersion; return this; }
        public Builder sortOrder(Integer sortOrder) { this.sortOrder = sortOrder; return this; }
        public Builder isRepeatable(Boolean isRepeatable) { this.isRepeatable = isRepeatable; return this; }
        public Builder conditionLogic(String conditionLogic) { this.conditionLogic = conditionLogic; return this; }
        public Builder scoringConfig(String scoringConfig) { this.scoringConfig = scoringConfig; return this; }
        public Builder inputMode(String inputMode) { this.inputMode = inputMode; return this; }
        public Builder inspectionMode(String inspectionMode) { this.inspectionMode = inspectionMode; return this; }
        public Builder continuousStart(String continuousStart) { this.continuousStart = continuousStart; return this; }
        public Builder continuousEnd(String continuousEnd) { this.continuousEnd = continuousEnd; return this; }
        public Builder createdBy(Long createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedBy(Long updatedBy) { this.updatedBy = updatedBy; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public TemplateSection build() { return new TemplateSection(this); }
    }
}
