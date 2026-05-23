package com.school.management.domain.inspection.model.execution;

import com.school.management.domain.inspection.event.ProjectCompletedEvent;
import com.school.management.domain.inspection.event.ProjectPausedEvent;
import com.school.management.domain.inspection.event.ProjectPublishedEvent;
import com.school.management.domain.inspection.event.ProjectResumedEvent;
import com.school.management.domain.shared.AggregateRoot;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 检查项目聚合根（V62 统一分区版）.
 *
 * <p>评级引擎完美架构 (2026-05-23): 撤销 {@code defaultScoringProfileId} —
 * 评分配置由 ScoringProfile 按 (project, section) 自动定位, 项目层不再保有评分兜底字段.
 * 评级则由 Indicator 跨分区聚合, 与项目本身解耦.
 *
 * <p>状态机: DRAFT → PUBLISHED → PAUSED → COMPLETED → ARCHIVED
 */
public class InspProject extends AggregateRoot<Long> {

    private Long tenantId;
    private String projectCode;
    private String projectName;
    /**
     * 数据权限边界: 项目覆盖到哪个组织单元 (被检对象的 org root, 非创建者归属).
     * 强制非空 — DataPermissionInterceptor 注入的 org_unit_id 过滤依赖该列.
     */
    private Long orgUnitId;
    /**
     * 向后兼容保留。V66 起新项目可为 null，模板通过 InspectionPlan.rootSectionId 关联。
     */
    private Long rootSectionId;          // 关联的根分区ID（替代 templateId），可空
    private Long templateVersionId;      // 锁定的版本快照
    private ScopeType scopeType;
    private String scopeConfig;          // JSON: 范围配置
    private LocalDate startDate;
    private LocalDate endDate;
    private AssignmentMode assignmentMode;
    private Boolean reviewRequired;
    private Boolean autoPublish;
    private String scoringConfigSnapshot; // JSON 快照，发布时锁定
    // review #E + #F: 项目级业务策略 (NULL=系统默认)
    private Integer maxRejectCount;       // 任务自动驳回上限, NULL=3
    private Integer maxEscalationLevel;   // 整改自动升级上限, NULL=3
    private Integer appealWindowDays;     // 申诉时效 (从 task 发布起的天数), NULL=7
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
        this.orgUnitId = builder.orgUnitId;
        this.rootSectionId = builder.rootSectionId;
        this.templateVersionId = builder.templateVersionId;
        this.scopeType = builder.scopeType != null ? builder.scopeType : ScopeType.ORG;
        this.scopeConfig = builder.scopeConfig;
        this.startDate = builder.startDate;
        this.endDate = builder.endDate;
        this.assignmentMode = builder.assignmentMode != null ? builder.assignmentMode : AssignmentMode.ASSIGNED;
        this.scoringConfigSnapshot = builder.scoringConfigSnapshot;
        this.maxRejectCount = builder.maxRejectCount;
        this.maxEscalationLevel = builder.maxEscalationLevel;
        this.appealWindowDays = builder.appealWindowDays;
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
                                     Long rootSectionId, LocalDate startDate,
                                     Long orgUnitId, Long createdBy) {
        return builder()
                .projectCode(projectCode)
                .projectName(projectName)
                .rootSectionId(rootSectionId)
                .orgUnitId(orgUnitId)
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
        validateAutoPublishReviewConflict(this.autoPublish, this.reviewRequired);
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

    public void lockScoringConfig(String snapshot) {
        this.scoringConfigSnapshot = snapshot;
        this.updatedAt = LocalDateTime.now();
    }

    public void updatePolicyConfig(Integer maxRejectCount, Integer maxEscalationLevel,
                                    Integer appealWindowDays, Long updatedBy) {
        if (this.status == ProjectStatus.ARCHIVED) {
            throw new IllegalStateException("已归档的项目不能修改策略配置");
        }
        if (maxRejectCount != null && maxRejectCount < 0) {
            throw new IllegalArgumentException("maxRejectCount 不能为负");
        }
        if (maxEscalationLevel != null && maxEscalationLevel < 0) {
            throw new IllegalArgumentException("maxEscalationLevel 不能为负");
        }
        if (appealWindowDays != null && appealWindowDays < 0) {
            throw new IllegalArgumentException("appealWindowDays 不能为负");
        }
        this.maxRejectCount = maxRejectCount;
        this.maxEscalationLevel = maxEscalationLevel;
        this.appealWindowDays = appealWindowDays;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    public void relockTemplateVersion(Long newTemplateVersionId) {
        if (this.status != ProjectStatus.PUBLISHED && this.status != ProjectStatus.PAUSED) {
            throw new IllegalStateException("只有已发布或已暂停的项目才能升级模板版本");
        }
        if (newTemplateVersionId == null) {
            throw new IllegalArgumentException("新模板版本ID不能为空");
        }
        this.templateVersionId = newTemplateVersionId;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 评级引擎完美架构 (2026-05-23): updateInfo 撤销 defaultScoringProfileId 参数.
     */
    public void updateInfo(String projectName, Long rootSectionId,
                           ScopeType scopeType, String scopeConfig,
                           LocalDate startDate, LocalDate endDate,
                           AssignmentMode assignmentMode, Boolean reviewRequired,
                           Boolean autoPublish, Long updatedBy) {
        if (this.status != ProjectStatus.DRAFT) {
            throw new IllegalStateException("只有草稿状态的项目才能修改");
        }
        Boolean effectiveAutoPublish = autoPublish != null ? autoPublish : this.autoPublish;
        Boolean effectiveReviewRequired = reviewRequired != null ? reviewRequired : this.reviewRequired;
        validateAutoPublishReviewConflict(effectiveAutoPublish, effectiveReviewRequired);
        if (projectName != null) this.projectName = projectName;
        if (rootSectionId != null) this.rootSectionId = rootSectionId;
        if (scopeType != null) this.scopeType = scopeType;
        if (scopeConfig != null) this.scopeConfig = scopeConfig;
        if (startDate != null) this.startDate = startDate;
        if (endDate != null) this.endDate = endDate;
        if (assignmentMode != null) this.assignmentMode = assignmentMode;
        this.reviewRequired = effectiveReviewRequired;
        this.autoPublish = effectiveAutoPublish;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateOperationalConfig(AssignmentMode assignmentMode, Boolean reviewRequired,
                                         Boolean autoPublish, String projectName, Long updatedBy) {
        if (this.status == ProjectStatus.ARCHIVED) {
            throw new IllegalStateException("已归档的项目不能修改");
        }
        Boolean effectiveAutoPublish = autoPublish != null ? autoPublish : this.autoPublish;
        Boolean effectiveReviewRequired = reviewRequired != null ? reviewRequired : this.reviewRequired;
        validateAutoPublishReviewConflict(effectiveAutoPublish, effectiveReviewRequired);
        if (assignmentMode != null) this.assignmentMode = assignmentMode;
        if (reviewRequired != null) this.reviewRequired = reviewRequired;
        if (autoPublish != null) this.autoPublish = autoPublish;
        if (projectName != null) this.projectName = projectName;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    private static void validateAutoPublishReviewConflict(Boolean autoPublish, Boolean reviewRequired) {
        if (Boolean.TRUE.equals(autoPublish) && Boolean.TRUE.equals(reviewRequired)) {
            throw new IllegalStateException("自动发布和必须审核不能同时启用");
        }
    }

    // Getters
    public Long getTenantId() { return tenantId; }
    public String getProjectCode() { return projectCode; }
    public String getProjectName() { return projectName; }
    public Long getOrgUnitId() { return orgUnitId; }
    public Long getRootSectionId() { return rootSectionId; }
    public Long getTemplateVersionId() { return templateVersionId; }
    public ScopeType getScopeType() { return scopeType; }
    public String getScopeConfig() { return scopeConfig; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public AssignmentMode getAssignmentMode() { return assignmentMode; }
    public Boolean getReviewRequired() { return reviewRequired; }
    public Boolean getAutoPublish() { return autoPublish; }
    public String getScoringConfigSnapshot() { return scoringConfigSnapshot; }
    public Integer getMaxRejectCount() { return maxRejectCount; }
    public Integer getMaxEscalationLevel() { return maxEscalationLevel; }
    public Integer getAppealWindowDays() { return appealWindowDays; }
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
        private Long orgUnitId;
        private Long rootSectionId;
        private Long templateVersionId;
        private ScopeType scopeType;
        private String scopeConfig;
        private LocalDate startDate;
        private LocalDate endDate;
        private AssignmentMode assignmentMode;
        private Boolean reviewRequired;
        private Boolean autoPublish;
        private String scoringConfigSnapshot;
        private Integer maxRejectCount;
        private Integer maxEscalationLevel;
        private Integer appealWindowDays;
        private ProjectStatus status;
        private Long createdBy;
        private LocalDateTime createdAt;
        private Long updatedBy;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(Long tenantId) { this.tenantId = tenantId; return this; }
        public Builder projectCode(String projectCode) { this.projectCode = projectCode; return this; }
        public Builder projectName(String projectName) { this.projectName = projectName; return this; }
        public Builder orgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; return this; }
        public Builder rootSectionId(Long rootSectionId) { this.rootSectionId = rootSectionId; return this; }
        public Builder templateVersionId(Long templateVersionId) { this.templateVersionId = templateVersionId; return this; }
        public Builder scopeType(ScopeType scopeType) { this.scopeType = scopeType; return this; }
        public Builder scopeConfig(String scopeConfig) { this.scopeConfig = scopeConfig; return this; }
        public Builder startDate(LocalDate startDate) { this.startDate = startDate; return this; }
        public Builder endDate(LocalDate endDate) { this.endDate = endDate; return this; }
        public Builder assignmentMode(AssignmentMode assignmentMode) { this.assignmentMode = assignmentMode; return this; }
        public Builder reviewRequired(Boolean reviewRequired) { this.reviewRequired = reviewRequired; return this; }
        public Builder autoPublish(Boolean autoPublish) { this.autoPublish = autoPublish; return this; }
        public Builder scoringConfigSnapshot(String scoringConfigSnapshot) { this.scoringConfigSnapshot = scoringConfigSnapshot; return this; }
        public Builder maxRejectCount(Integer v) { this.maxRejectCount = v; return this; }
        public Builder maxEscalationLevel(Integer v) { this.maxEscalationLevel = v; return this; }
        public Builder appealWindowDays(Integer v) { this.appealWindowDays = v; return this; }
        public Builder status(ProjectStatus status) { this.status = status; return this; }
        public Builder createdBy(Long createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedBy(Long updatedBy) { this.updatedBy = updatedBy; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public InspProject build() { return new InspProject(this); }
    }
}
