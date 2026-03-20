package com.school.management.domain.inspection.model.v6;

import com.school.management.domain.shared.AggregateRoot;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * V6检查项目聚合根
 * 代表一个完整的检查项目，包含模板、范围、周期等配置
 */
public class InspectionProject extends AggregateRoot<Long> {

    private Long id;
    private String projectCode;
    private String projectName;
    private String description;

    // 模板相关
    private Long templateId;
    private String templateSnapshot; // JSON

    // 检查范围
    private ScopeType scopeType;
    private String scopeConfig; // JSON - 选中的ID列表

    // 时间周期
    private LocalDate startDate;
    private LocalDate endDate;
    private CycleType cycleType;
    private String cycleConfig; // JSON
    private String timeSlots; // JSON
    private boolean skipHolidays;
    private String excludedDates; // JSON

    // 策略配置
    private SharedPlaceStrategy sharedPlaceStrategy;
    private String scoreDistributionMode;
    private InspectorAssignmentMode inspectorAssignmentMode;
    private String defaultInspectors; // JSON

    // 状态
    private ProjectStatus status;
    private LocalDateTime publishedAt;
    private LocalDateTime pausedAt;
    private LocalDateTime completedAt;

    // 统计
    private Integer totalTasks;
    private Integer completedTasks;

    // 审计
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    protected InspectionProject() {
    }

    private InspectionProject(Builder builder) {
        this.id = builder.id;
        this.projectCode = builder.projectCode;
        this.projectName = builder.projectName;
        this.description = builder.description;
        this.templateId = builder.templateId;
        this.templateSnapshot = builder.templateSnapshot;
        this.scopeType = builder.scopeType != null ? builder.scopeType : ScopeType.ORG;
        this.scopeConfig = builder.scopeConfig;
        this.startDate = builder.startDate;
        this.endDate = builder.endDate;
        this.cycleType = builder.cycleType != null ? builder.cycleType : CycleType.DAILY;
        this.cycleConfig = builder.cycleConfig;
        this.timeSlots = builder.timeSlots;
        this.skipHolidays = builder.skipHolidays;
        this.excludedDates = builder.excludedDates;
        this.sharedPlaceStrategy = builder.sharedPlaceStrategy != null ? builder.sharedPlaceStrategy : SharedPlaceStrategy.RATIO;
        this.scoreDistributionMode = builder.scoreDistributionMode;
        this.inspectorAssignmentMode = builder.inspectorAssignmentMode != null ? builder.inspectorAssignmentMode : InspectorAssignmentMode.FREE;
        this.defaultInspectors = builder.defaultInspectors;
        this.status = builder.status != null ? builder.status : ProjectStatus.DRAFT;
        this.publishedAt = builder.publishedAt;
        this.pausedAt = builder.pausedAt;
        this.completedAt = builder.completedAt;
        this.totalTasks = builder.totalTasks != null ? builder.totalTasks : 0;
        this.completedTasks = builder.completedTasks != null ? builder.completedTasks : 0;
        this.createdBy = builder.createdBy;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedBy = builder.updatedBy;
        this.updatedAt = builder.updatedAt;
    }

    /**
     * 创建新项目（草稿状态）
     */
    public static InspectionProject create(String projectCode, String projectName, Long templateId, Long createdBy) {
        return builder()
                .projectCode(projectCode)
                .projectName(projectName)
                .templateId(templateId)
                .status(ProjectStatus.DRAFT)
                .createdBy(createdBy)
                .build();
    }

    /**
     * 发布项目
     */
    public void publish(String templateSnapshot) {
        if (this.status != ProjectStatus.DRAFT) {
            throw new IllegalStateException("只有草稿状态的项目才能发布");
        }
        validateForPublish();
        this.templateSnapshot = templateSnapshot;
        this.status = ProjectStatus.ACTIVE;
        this.publishedAt = LocalDateTime.now();
    }

    /**
     * 暂停项目
     */
    public void pause() {
        if (this.status != ProjectStatus.ACTIVE) {
            throw new IllegalStateException("只有进行中的项目才能暂停");
        }
        this.status = ProjectStatus.PAUSED;
        this.pausedAt = LocalDateTime.now();
    }

    /**
     * 恢复项目
     */
    public void resume() {
        if (this.status != ProjectStatus.PAUSED) {
            throw new IllegalStateException("只有暂停的项目才能恢复");
        }
        this.status = ProjectStatus.ACTIVE;
        this.pausedAt = null;
    }

    /**
     * 完成项目
     */
    public void complete() {
        if (this.status != ProjectStatus.ACTIVE && this.status != ProjectStatus.PAUSED) {
            throw new IllegalStateException("只有进行中或暂停的项目才能完成");
        }
        this.status = ProjectStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * 归档项目
     */
    public void archive() {
        if (this.status != ProjectStatus.COMPLETED) {
            throw new IllegalStateException("只有已完成的项目才能归档");
        }
        this.status = ProjectStatus.ARCHIVED;
    }

    /**
     * 更新配置（仅草稿状态可用）
     */
    public void updateConfig(ScopeType scopeType, String scopeConfig,
                             LocalDate startDate, LocalDate endDate,
                             CycleType cycleType, String cycleConfig,
                             String timeSlots, boolean skipHolidays,
                             SharedPlaceStrategy sharedPlaceStrategy,
                             InspectorAssignmentMode inspectorAssignmentMode) {
        if (this.status != ProjectStatus.DRAFT) {
            throw new IllegalStateException("只有草稿状态的项目才能修改配置");
        }
        this.scopeType = scopeType;
        this.scopeConfig = scopeConfig;
        this.startDate = startDate;
        this.endDate = endDate;
        this.cycleType = cycleType;
        this.cycleConfig = cycleConfig;
        this.timeSlots = timeSlots;
        this.skipHolidays = skipHolidays;
        this.sharedPlaceStrategy = sharedPlaceStrategy;
        this.inspectorAssignmentMode = inspectorAssignmentMode;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 增加已完成任务数
     */
    public void incrementCompletedTasks() {
        this.completedTasks = (this.completedTasks != null ? this.completedTasks : 0) + 1;
    }

    private void validateForPublish() {
        if (projectName == null || projectName.isBlank()) {
            throw new IllegalArgumentException("项目名称不能为空");
        }
        if (templateId == null) {
            throw new IllegalArgumentException("必须选择检查模板");
        }
        if (startDate == null) {
            throw new IllegalArgumentException("必须设置开始日期");
        }
        if (scopeConfig == null || scopeConfig.isBlank()) {
            throw new IllegalArgumentException("必须设置检查范围");
        }
    }

    // Getters
    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProjectCode() {
        return projectCode;
    }

    public String getProjectName() {
        return projectName;
    }

    void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 更新项目描述
     */
    public void updateDescription(String description) {
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    void setDescription(String description) {
        this.description = description;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public String getTemplateSnapshot() {
        return templateSnapshot;
    }

    public ScopeType getScopeType() {
        return scopeType;
    }

    public String getScopeConfig() {
        return scopeConfig;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public CycleType getCycleType() {
        return cycleType;
    }

    public String getCycleConfig() {
        return cycleConfig;
    }

    public String getTimeSlots() {
        return timeSlots;
    }

    public boolean isSkipHolidays() {
        return skipHolidays;
    }

    public String getExcludedDates() {
        return excludedDates;
    }

    public SharedPlaceStrategy getSharedPlaceStrategy() {
        return sharedPlaceStrategy;
    }

    public String getScoreDistributionMode() {
        return scoreDistributionMode;
    }

    public InspectorAssignmentMode getInspectorAssignmentMode() {
        return inspectorAssignmentMode;
    }

    public String getDefaultInspectors() {
        return defaultInspectors;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public LocalDateTime getPausedAt() {
        return pausedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public Integer getTotalTasks() {
        return totalTasks;
    }

    void setTotalTasks(Integer totalTasks) {
        this.totalTasks = totalTasks;
    }

    public Integer getCompletedTasks() {
        return completedTasks;
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
        private String projectCode;
        private String projectName;
        private String description;
        private Long templateId;
        private String templateSnapshot;
        private ScopeType scopeType;
        private String scopeConfig;
        private LocalDate startDate;
        private LocalDate endDate;
        private CycleType cycleType;
        private String cycleConfig;
        private String timeSlots;
        private boolean skipHolidays = true;
        private String excludedDates;
        private SharedPlaceStrategy sharedPlaceStrategy;
        private String scoreDistributionMode;
        private InspectorAssignmentMode inspectorAssignmentMode;
        private String defaultInspectors;
        private ProjectStatus status;
        private LocalDateTime publishedAt;
        private LocalDateTime pausedAt;
        private LocalDateTime completedAt;
        private Integer totalTasks;
        private Integer completedTasks;
        private Long createdBy;
        private LocalDateTime createdAt;
        private Long updatedBy;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder projectCode(String projectCode) { this.projectCode = projectCode; return this; }
        public Builder projectName(String projectName) { this.projectName = projectName; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder templateId(Long templateId) { this.templateId = templateId; return this; }
        public Builder templateSnapshot(String templateSnapshot) { this.templateSnapshot = templateSnapshot; return this; }
        public Builder scopeType(ScopeType scopeType) { this.scopeType = scopeType; return this; }
        public Builder scopeConfig(String scopeConfig) { this.scopeConfig = scopeConfig; return this; }
        public Builder startDate(LocalDate startDate) { this.startDate = startDate; return this; }
        public Builder endDate(LocalDate endDate) { this.endDate = endDate; return this; }
        public Builder cycleType(CycleType cycleType) { this.cycleType = cycleType; return this; }
        public Builder cycleConfig(String cycleConfig) { this.cycleConfig = cycleConfig; return this; }
        public Builder timeSlots(String timeSlots) { this.timeSlots = timeSlots; return this; }
        public Builder skipHolidays(boolean skipHolidays) { this.skipHolidays = skipHolidays; return this; }
        public Builder excludedDates(String excludedDates) { this.excludedDates = excludedDates; return this; }
        public Builder sharedPlaceStrategy(SharedPlaceStrategy sharedPlaceStrategy) { this.sharedPlaceStrategy = sharedPlaceStrategy; return this; }
        public Builder scoreDistributionMode(String scoreDistributionMode) { this.scoreDistributionMode = scoreDistributionMode; return this; }
        public Builder inspectorAssignmentMode(InspectorAssignmentMode inspectorAssignmentMode) { this.inspectorAssignmentMode = inspectorAssignmentMode; return this; }
        public Builder defaultInspectors(String defaultInspectors) { this.defaultInspectors = defaultInspectors; return this; }
        public Builder status(ProjectStatus status) { this.status = status; return this; }
        public Builder publishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; return this; }
        public Builder pausedAt(LocalDateTime pausedAt) { this.pausedAt = pausedAt; return this; }
        public Builder completedAt(LocalDateTime completedAt) { this.completedAt = completedAt; return this; }
        public Builder totalTasks(Integer totalTasks) { this.totalTasks = totalTasks; return this; }
        public Builder completedTasks(Integer completedTasks) { this.completedTasks = completedTasks; return this; }
        public Builder createdBy(Long createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedBy(Long updatedBy) { this.updatedBy = updatedBy; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public InspectionProject build() {
            return new InspectionProject(this);
        }
    }
}
