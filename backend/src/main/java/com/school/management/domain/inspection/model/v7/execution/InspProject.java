package com.school.management.domain.inspection.model.v7.execution;

import com.school.management.domain.inspection.event.v7.ProjectCompletedEvent;
import com.school.management.domain.inspection.event.v7.ProjectPausedEvent;
import com.school.management.domain.inspection.event.v7.ProjectPublishedEvent;
import com.school.management.domain.inspection.event.v7.ProjectResumedEvent;
import com.school.management.domain.shared.AggregateRoot;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * V7 检查项目聚合根（V62 统一分区版）
 * 不再有 parentProjectId/targetType/cycleType/cycleConfig（排期移到 InspectionPlan）
 * templateId 改为 rootSectionId（关联根分区而非模板）
 *
 * V66 多模板支持：rootSectionId 改为可空，保留作向后兼容。
 * 新项目通过 InspectionPlan.rootSectionId 关联模板（每个计划绑定一个模板）。
 *
 * 状态机: DRAFT → PUBLISHED → PAUSED → COMPLETED → ARCHIVED
 */
public class InspProject extends AggregateRoot<Long> {

    private Long id;
    private Long tenantId;
    private String projectCode;
    private String projectName;
    /**
     * 向后兼容保留。V66 起新项目可为 null，模板通过 InspectionPlan.rootSectionId 关联。
     * 旧项目此字段仍有值，可作为"主模板"快速访问。
     */
    private Long rootSectionId;          // 关联的根分区ID（替代 templateId），可空
    private Long templateVersionId;      // 锁定的版本快照
    private Long scoringProfileId;
    private ScopeType scopeType;
    private String scopeConfig;          // JSON: 范围配置
    private LocalDate startDate;
    private LocalDate endDate;
    private AssignmentMode assignmentMode;
    private Boolean reviewRequired;
    private Boolean autoPublish;
    // 评分策略（项目级）
    private String evaluationMode;       // SINGLE, MULTI
    private String multiRaterMode;       // AVERAGE, WEIGHTED_AVERAGE, MEDIAN, MAX, MIN, CONSENSUS
    private String raterWeightBy;        // EQUAL, BY_ROLE, BY_EXPERIENCE
    private java.math.BigDecimal consensusThreshold;
    private Boolean trendEnabled;
    private Integer trendLookbackDays;
    private Boolean decayEnabled;
    private String decayMode;            // LINEAR, EXPONENTIAL
    private Boolean calibrationEnabled;
    private String calibrationMethod;    // Z_SCORE, MIN_MAX, PERCENTILE_RANK
    private String splitStrategy;        // NONE, BY_TARGET, BY_SECTION, MANUAL
    private String scoringConfigSnapshot; // JSON 快照，发布时锁定
    private ProjectStatus status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    protected InspProject() {
    }

    private InspProject(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId;
        this.projectCode = builder.projectCode;
        this.projectName = builder.projectName;
        this.rootSectionId = builder.rootSectionId;
        this.templateVersionId = builder.templateVersionId;
        this.scoringProfileId = builder.scoringProfileId;
        this.scopeType = builder.scopeType != null ? builder.scopeType : ScopeType.ORG;
        this.scopeConfig = builder.scopeConfig;
        this.startDate = builder.startDate;
        this.endDate = builder.endDate;
        this.assignmentMode = builder.assignmentMode != null ? builder.assignmentMode : AssignmentMode.ASSIGNED;
        this.evaluationMode = builder.evaluationMode != null ? builder.evaluationMode : "SINGLE";
        this.multiRaterMode = builder.multiRaterMode != null ? builder.multiRaterMode : "AVERAGE";
        this.raterWeightBy = builder.raterWeightBy;
        this.consensusThreshold = builder.consensusThreshold;
        this.trendEnabled = builder.trendEnabled != null ? builder.trendEnabled : false;
        this.trendLookbackDays = builder.trendLookbackDays;
        this.decayEnabled = builder.decayEnabled != null ? builder.decayEnabled : false;
        this.decayMode = builder.decayMode;
        this.calibrationEnabled = builder.calibrationEnabled != null ? builder.calibrationEnabled : false;
        this.calibrationMethod = builder.calibrationMethod;
        this.splitStrategy = builder.splitStrategy != null ? builder.splitStrategy : "NONE";
        this.scoringConfigSnapshot = builder.scoringConfigSnapshot;
        this.reviewRequired = builder.reviewRequired != null ? builder.reviewRequired : true;
        this.autoPublish = builder.autoPublish != null ? builder.autoPublish : false;
        this.status = builder.status != null ? builder.status : ProjectStatus.DRAFT;
        this.createdBy = builder.createdBy;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedBy = builder.updatedBy;
        this.updatedAt = builder.updatedAt;
    }

    /**
     * 创建新项目。rootSectionId 可为 null（多模板项目通过 InspectionPlan 关联模板）。
     */
    public static InspProject create(String projectCode, String projectName,
                                     Long rootSectionId, LocalDate startDate, Long createdBy) {
        return builder()
                .projectCode(projectCode)
                .projectName(projectName)
                .rootSectionId(rootSectionId)   // nullable: null 表示通过计划关联模板
                .startDate(startDate)
                .status(ProjectStatus.DRAFT)
                .createdBy(createdBy)
                .build();
    }

    public static InspProject reconstruct(Builder builder) {
        return new InspProject(builder);
    }

    public void publish(Long templateVersionId) {
        if (this.status != ProjectStatus.DRAFT) {
            throw new IllegalStateException("只有草稿项目才能发布");
        }
        this.templateVersionId = templateVersionId;
        this.status = ProjectStatus.PUBLISHED;
        this.updatedAt = LocalDateTime.now();
        registerEvent(new ProjectPublishedEvent(this.id, this.projectCode, this.rootSectionId));
    }

    public void pause() {
        if (this.status != ProjectStatus.PUBLISHED) {
            throw new IllegalStateException("只有已发布的项目才能暂停");
        }
        this.status = ProjectStatus.PAUSED;
        this.updatedAt = LocalDateTime.now();
        registerEvent(new ProjectPausedEvent(this.id, this.projectCode));
    }

    public void resume() {
        if (this.status != ProjectStatus.PAUSED) {
            throw new IllegalStateException("只有已暂停的项目才能恢复");
        }
        this.status = ProjectStatus.PUBLISHED;
        this.updatedAt = LocalDateTime.now();
        registerEvent(new ProjectResumedEvent(this.id, this.projectCode));
    }

    public void complete() {
        if (this.status != ProjectStatus.PUBLISHED && this.status != ProjectStatus.PAUSED) {
            throw new IllegalStateException("只有已发布或已暂停的项目才能完成");
        }
        this.status = ProjectStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
        registerEvent(new ProjectCompletedEvent(this.id, this.projectCode));
    }

    public void archive() {
        if (this.status != ProjectStatus.COMPLETED) {
            throw new IllegalStateException("只有已完成的项目才能归档");
        }
        this.status = ProjectStatus.ARCHIVED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 锁定评分配置快照（发布时调用）
     */
    public void lockScoringConfig(String snapshot) {
        this.scoringConfigSnapshot = snapshot;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateInfo(String projectName, Long rootSectionId, Long scoringProfileId,
                           ScopeType scopeType, String scopeConfig,
                           LocalDate startDate, LocalDate endDate,
                           AssignmentMode assignmentMode, Boolean reviewRequired,
                           Boolean autoPublish, Long updatedBy) {
        if (this.status != ProjectStatus.DRAFT) {
            throw new IllegalStateException("只有草稿状态的项目才能修改");
        }
        this.projectName = projectName;
        this.rootSectionId = rootSectionId;
        this.scoringProfileId = scoringProfileId;
        this.scopeType = scopeType;
        this.scopeConfig = scopeConfig;
        this.startDate = startDate;
        this.endDate = endDate;
        this.assignmentMode = assignmentMode;
        this.reviewRequired = reviewRequired;
        this.autoPublish = autoPublish;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateOperationalConfig(AssignmentMode assignmentMode, Boolean reviewRequired,
                                         Boolean autoPublish, String projectName, Long updatedBy) {
        if (this.status == ProjectStatus.ARCHIVED) {
            throw new IllegalStateException("已归档的项目不能修改");
        }
        if (assignmentMode != null) this.assignmentMode = assignmentMode;
        if (reviewRequired != null) this.reviewRequired = reviewRequired;
        if (autoPublish != null) this.autoPublish = autoPublish;
        if (projectName != null) this.projectName = projectName;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    @Override
    public Long getId() { return id; }

    @Override
    public void setId(Long id) { this.id = id; }

    public Long getTenantId() { return tenantId; }
    public String getProjectCode() { return projectCode; }
    public String getProjectName() { return projectName; }
    public Long getRootSectionId() { return rootSectionId; }
    public Long getTemplateVersionId() { return templateVersionId; }
    public Long getScoringProfileId() { return scoringProfileId; }
    public ScopeType getScopeType() { return scopeType; }
    public String getScopeConfig() { return scopeConfig; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public AssignmentMode getAssignmentMode() { return assignmentMode; }
    public Boolean getReviewRequired() { return reviewRequired; }
    public Boolean getAutoPublish() { return autoPublish; }
    public String getEvaluationMode() { return evaluationMode; }
    public String getMultiRaterMode() { return multiRaterMode; }
    public String getRaterWeightBy() { return raterWeightBy; }
    public java.math.BigDecimal getConsensusThreshold() { return consensusThreshold; }
    public Boolean getTrendEnabled() { return trendEnabled; }
    public Integer getTrendLookbackDays() { return trendLookbackDays; }
    public Boolean getDecayEnabled() { return decayEnabled; }
    public String getDecayMode() { return decayMode; }
    public Boolean getCalibrationEnabled() { return calibrationEnabled; }
    public String getCalibrationMethod() { return calibrationMethod; }
    public String getSplitStrategy() { return splitStrategy; }
    public String getScoringConfigSnapshot() { return scoringConfigSnapshot; }
    public ProjectStatus getStatus() { return status; }
    public Long getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Long getUpdatedBy() { return updatedBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Backward compatibility — InspProjectPO still has templateId column during migration
    @Deprecated
    public Long getTemplateId() { return rootSectionId; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long tenantId;
        private String projectCode;
        private String projectName;
        private Long rootSectionId;
        private Long templateVersionId;
        private Long scoringProfileId;
        private ScopeType scopeType;
        private String scopeConfig;
        private LocalDate startDate;
        private LocalDate endDate;
        private AssignmentMode assignmentMode;
        private Boolean reviewRequired;
        private Boolean autoPublish;
        private String evaluationMode;
        private String multiRaterMode;
        private String raterWeightBy;
        private java.math.BigDecimal consensusThreshold;
        private Boolean trendEnabled;
        private Integer trendLookbackDays;
        private Boolean decayEnabled;
        private String decayMode;
        private Boolean calibrationEnabled;
        private String calibrationMethod;
        private String splitStrategy;
        private String scoringConfigSnapshot;
        private ProjectStatus status;
        private Long createdBy;
        private LocalDateTime createdAt;
        private Long updatedBy;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(Long tenantId) { this.tenantId = tenantId; return this; }
        public Builder projectCode(String projectCode) { this.projectCode = projectCode; return this; }
        public Builder projectName(String projectName) { this.projectName = projectName; return this; }
        public Builder rootSectionId(Long rootSectionId) { this.rootSectionId = rootSectionId; return this; }
        public Builder templateVersionId(Long templateVersionId) { this.templateVersionId = templateVersionId; return this; }
        public Builder scoringProfileId(Long scoringProfileId) { this.scoringProfileId = scoringProfileId; return this; }
        public Builder scopeType(ScopeType scopeType) { this.scopeType = scopeType; return this; }
        public Builder scopeConfig(String scopeConfig) { this.scopeConfig = scopeConfig; return this; }
        public Builder startDate(LocalDate startDate) { this.startDate = startDate; return this; }
        public Builder endDate(LocalDate endDate) { this.endDate = endDate; return this; }
        public Builder assignmentMode(AssignmentMode assignmentMode) { this.assignmentMode = assignmentMode; return this; }
        public Builder reviewRequired(Boolean reviewRequired) { this.reviewRequired = reviewRequired; return this; }
        public Builder autoPublish(Boolean autoPublish) { this.autoPublish = autoPublish; return this; }
        public Builder evaluationMode(String evaluationMode) { this.evaluationMode = evaluationMode; return this; }
        public Builder multiRaterMode(String multiRaterMode) { this.multiRaterMode = multiRaterMode; return this; }
        public Builder raterWeightBy(String raterWeightBy) { this.raterWeightBy = raterWeightBy; return this; }
        public Builder consensusThreshold(java.math.BigDecimal consensusThreshold) { this.consensusThreshold = consensusThreshold; return this; }
        public Builder trendEnabled(Boolean trendEnabled) { this.trendEnabled = trendEnabled; return this; }
        public Builder trendLookbackDays(Integer trendLookbackDays) { this.trendLookbackDays = trendLookbackDays; return this; }
        public Builder decayEnabled(Boolean decayEnabled) { this.decayEnabled = decayEnabled; return this; }
        public Builder decayMode(String decayMode) { this.decayMode = decayMode; return this; }
        public Builder calibrationEnabled(Boolean calibrationEnabled) { this.calibrationEnabled = calibrationEnabled; return this; }
        public Builder calibrationMethod(String calibrationMethod) { this.calibrationMethod = calibrationMethod; return this; }
        public Builder splitStrategy(String splitStrategy) { this.splitStrategy = splitStrategy; return this; }
        public Builder scoringConfigSnapshot(String scoringConfigSnapshot) { this.scoringConfigSnapshot = scoringConfigSnapshot; return this; }
        public Builder status(ProjectStatus status) { this.status = status; return this; }
        public Builder createdBy(Long createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedBy(Long updatedBy) { this.updatedBy = updatedBy; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public InspProject build() { return new InspProject(this); }
    }
}
