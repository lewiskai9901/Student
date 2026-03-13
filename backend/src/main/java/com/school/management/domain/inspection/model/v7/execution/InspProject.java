package com.school.management.domain.inspection.model.v7.execution;

import com.school.management.domain.inspection.event.v7.ProjectCompletedEvent;
import com.school.management.domain.inspection.event.v7.ProjectPausedEvent;
import com.school.management.domain.inspection.event.v7.ProjectPublishedEvent;
import com.school.management.domain.inspection.event.v7.ProjectResumedEvent;
import com.school.management.domain.shared.AggregateRoot;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * V7 检查项目聚合根
 * 状态机: DRAFT → PUBLISHED → PAUSED → COMPLETED → ARCHIVED
 */
public class InspProject extends AggregateRoot<Long> {

    private Long id;
    private Long tenantId;
    private Long parentProjectId;
    private String projectCode;
    private String projectName;
    private Long templateId;
    private Long templateVersionId;
    private Long scoringProfileId;
    private ScopeType scopeType;
    private String scopeConfig;
    private TargetType targetType;
    private LocalDate startDate;
    private LocalDate endDate;
    private CycleType cycleType;
    private String cycleConfig;
    private String timeSlots;
    private Boolean skipHolidays;
    private Long holidayCalendarId;
    private String excludedDates;
    private AssignmentMode assignmentMode;
    private Boolean reviewRequired;
    private Boolean autoPublish;
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
        this.parentProjectId = builder.parentProjectId;
        this.projectCode = builder.projectCode;
        this.projectName = builder.projectName;
        this.templateId = builder.templateId;
        this.templateVersionId = builder.templateVersionId;
        this.scoringProfileId = builder.scoringProfileId;
        this.scopeType = builder.scopeType != null ? builder.scopeType : ScopeType.ORG;
        this.scopeConfig = builder.scopeConfig;
        this.targetType = builder.targetType != null ? builder.targetType : TargetType.ORG;
        this.startDate = builder.startDate;
        this.endDate = builder.endDate;
        this.cycleType = builder.cycleType != null ? builder.cycleType : CycleType.DAILY;
        this.cycleConfig = builder.cycleConfig;
        this.timeSlots = builder.timeSlots;
        this.skipHolidays = builder.skipHolidays != null ? builder.skipHolidays : false;
        this.holidayCalendarId = builder.holidayCalendarId;
        this.excludedDates = builder.excludedDates;
        this.assignmentMode = builder.assignmentMode != null ? builder.assignmentMode : AssignmentMode.ASSIGNED;
        this.reviewRequired = builder.reviewRequired != null ? builder.reviewRequired : true;
        this.autoPublish = builder.autoPublish != null ? builder.autoPublish : false;
        this.status = builder.status != null ? builder.status : ProjectStatus.DRAFT;
        this.createdBy = builder.createdBy;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedBy = builder.updatedBy;
        this.updatedAt = builder.updatedAt;
    }

    public static InspProject create(String projectCode, String projectName,
                                     Long templateId, LocalDate startDate, Long createdBy) {
        return builder()
                .projectCode(projectCode)
                .projectName(projectName)
                .templateId(templateId)
                .startDate(startDate)
                .status(ProjectStatus.DRAFT)
                .createdBy(createdBy)
                .build();
    }

    public static InspProject reconstruct(Builder builder) {
        return new InspProject(builder);
    }

    /**
     * 发布项目 — DRAFT → PUBLISHED
     */
    public void publish(Long templateVersionId) {
        if (this.status != ProjectStatus.DRAFT) {
            throw new IllegalStateException("只有草稿项目才能发布");
        }
        this.templateVersionId = templateVersionId;
        this.status = ProjectStatus.PUBLISHED;
        this.updatedAt = LocalDateTime.now();
        registerEvent(new ProjectPublishedEvent(this.id, this.projectCode, this.templateId));
    }

    /**
     * 暂停项目 — PUBLISHED → PAUSED
     */
    public void pause() {
        if (this.status != ProjectStatus.PUBLISHED) {
            throw new IllegalStateException("只有已发布的项目才能暂停");
        }
        this.status = ProjectStatus.PAUSED;
        this.updatedAt = LocalDateTime.now();
        registerEvent(new ProjectPausedEvent(this.id, this.projectCode));
    }

    /**
     * 恢复项目 — PAUSED → PUBLISHED
     */
    public void resume() {
        if (this.status != ProjectStatus.PAUSED) {
            throw new IllegalStateException("只有已暂停的项目才能恢复");
        }
        this.status = ProjectStatus.PUBLISHED;
        this.updatedAt = LocalDateTime.now();
        registerEvent(new ProjectResumedEvent(this.id, this.projectCode));
    }

    /**
     * 完成项目 — PUBLISHED/PAUSED → COMPLETED
     */
    public void complete() {
        if (this.status != ProjectStatus.PUBLISHED && this.status != ProjectStatus.PAUSED) {
            throw new IllegalStateException("只有已发布或已暂停的项目才能完成");
        }
        this.status = ProjectStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
        registerEvent(new ProjectCompletedEvent(this.id, this.projectCode));
    }

    /**
     * 归档项目 — COMPLETED → ARCHIVED
     */
    public void archive() {
        if (this.status != ProjectStatus.COMPLETED) {
            throw new IllegalStateException("只有已完成的项目才能归档");
        }
        this.status = ProjectStatus.ARCHIVED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新项目基本信息（仅草稿可编辑）
     */
    public void updateInfo(String projectName, Long templateId, Long scoringProfileId,
                           ScopeType scopeType, String scopeConfig,
                           TargetType targetType, LocalDate startDate, LocalDate endDate,
                           CycleType cycleType, String cycleConfig, String timeSlots,
                           Boolean skipHolidays, Long holidayCalendarId, String excludedDates,
                           AssignmentMode assignmentMode, Boolean reviewRequired,
                           Boolean autoPublish, Long updatedBy) {
        if (this.status != ProjectStatus.DRAFT) {
            throw new IllegalStateException("只有草稿状态的项目才能修改");
        }
        this.projectName = projectName;
        this.templateId = templateId;
        this.scoringProfileId = scoringProfileId;
        this.scopeType = scopeType;
        this.scopeConfig = scopeConfig;
        this.targetType = targetType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.cycleType = cycleType;
        this.cycleConfig = cycleConfig;
        this.timeSlots = timeSlots;
        this.skipHolidays = skipHolidays;
        this.holidayCalendarId = holidayCalendarId;
        this.excludedDates = excludedDates;
        this.assignmentMode = assignmentMode;
        this.reviewRequired = reviewRequired;
        this.autoPublish = autoPublish;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    @Override
    public Long getId() { return id; }

    @Override
    public void setId(Long id) { this.id = id; }

    public Long getTenantId() { return tenantId; }
    public Long getParentProjectId() { return parentProjectId; }
    public String getProjectCode() { return projectCode; }
    public String getProjectName() { return projectName; }
    public Long getTemplateId() { return templateId; }
    public Long getTemplateVersionId() { return templateVersionId; }
    public Long getScoringProfileId() { return scoringProfileId; }
    public ScopeType getScopeType() { return scopeType; }
    public String getScopeConfig() { return scopeConfig; }
    public TargetType getTargetType() { return targetType; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public CycleType getCycleType() { return cycleType; }
    public String getCycleConfig() { return cycleConfig; }
    public String getTimeSlots() { return timeSlots; }
    public Boolean getSkipHolidays() { return skipHolidays; }
    public Long getHolidayCalendarId() { return holidayCalendarId; }
    public String getExcludedDates() { return excludedDates; }
    public AssignmentMode getAssignmentMode() { return assignmentMode; }
    public Boolean getReviewRequired() { return reviewRequired; }
    public Boolean getAutoPublish() { return autoPublish; }
    public ProjectStatus getStatus() { return status; }
    public Long getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Long getUpdatedBy() { return updatedBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long tenantId;
        private Long parentProjectId;
        private String projectCode;
        private String projectName;
        private Long templateId;
        private Long templateVersionId;
        private Long scoringProfileId;
        private ScopeType scopeType;
        private String scopeConfig;
        private TargetType targetType;
        private LocalDate startDate;
        private LocalDate endDate;
        private CycleType cycleType;
        private String cycleConfig;
        private String timeSlots;
        private Boolean skipHolidays;
        private Long holidayCalendarId;
        private String excludedDates;
        private AssignmentMode assignmentMode;
        private Boolean reviewRequired;
        private Boolean autoPublish;
        private ProjectStatus status;
        private Long createdBy;
        private LocalDateTime createdAt;
        private Long updatedBy;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(Long tenantId) { this.tenantId = tenantId; return this; }
        public Builder parentProjectId(Long parentProjectId) { this.parentProjectId = parentProjectId; return this; }
        public Builder projectCode(String projectCode) { this.projectCode = projectCode; return this; }
        public Builder projectName(String projectName) { this.projectName = projectName; return this; }
        public Builder templateId(Long templateId) { this.templateId = templateId; return this; }
        public Builder templateVersionId(Long templateVersionId) { this.templateVersionId = templateVersionId; return this; }
        public Builder scoringProfileId(Long scoringProfileId) { this.scoringProfileId = scoringProfileId; return this; }
        public Builder scopeType(ScopeType scopeType) { this.scopeType = scopeType; return this; }
        public Builder scopeConfig(String scopeConfig) { this.scopeConfig = scopeConfig; return this; }
        public Builder targetType(TargetType targetType) { this.targetType = targetType; return this; }
        public Builder startDate(LocalDate startDate) { this.startDate = startDate; return this; }
        public Builder endDate(LocalDate endDate) { this.endDate = endDate; return this; }
        public Builder cycleType(CycleType cycleType) { this.cycleType = cycleType; return this; }
        public Builder cycleConfig(String cycleConfig) { this.cycleConfig = cycleConfig; return this; }
        public Builder timeSlots(String timeSlots) { this.timeSlots = timeSlots; return this; }
        public Builder skipHolidays(Boolean skipHolidays) { this.skipHolidays = skipHolidays; return this; }
        public Builder holidayCalendarId(Long holidayCalendarId) { this.holidayCalendarId = holidayCalendarId; return this; }
        public Builder excludedDates(String excludedDates) { this.excludedDates = excludedDates; return this; }
        public Builder assignmentMode(AssignmentMode assignmentMode) { this.assignmentMode = assignmentMode; return this; }
        public Builder reviewRequired(Boolean reviewRequired) { this.reviewRequired = reviewRequired; return this; }
        public Builder autoPublish(Boolean autoPublish) { this.autoPublish = autoPublish; return this; }
        public Builder status(ProjectStatus status) { this.status = status; return this; }
        public Builder createdBy(Long createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedBy(Long updatedBy) { this.updatedBy = updatedBy; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public InspProject build() { return new InspProject(this); }
    }
}
