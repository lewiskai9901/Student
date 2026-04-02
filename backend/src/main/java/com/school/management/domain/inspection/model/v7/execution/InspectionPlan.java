package com.school.management.domain.inspection.model.v7.execution;

import com.school.management.domain.shared.AggregateRoot;

import java.time.LocalDateTime;

/**
 * 检查计划 — 排期从项目层管理
 * 一个项目可有多个检查计划，每个计划关联一个模板（rootSectionId）。
 *
 * V66 多模板支持：每个计划通过 rootSectionId 绑定自己的模板（根分区）。
 * sectionIds 用于在该模板下进一步指定检查哪些一级分区（为空则覆盖全部）。
 */
public class InspectionPlan extends AggregateRoot<Long> {

    private Long id;
    private Long tenantId;
    private Long projectId;
    private String planName;
    private Long rootSectionId;        // V66: 该计划使用的模板（根分区ID），不可空
    private String sectionIds;         // JSON: 关联的一级分区ID列表（为空则覆盖全部）
    private String scheduleMode;       // REGULAR / ON_DEMAND
    private String cycleType;          // DAILY / WEEKLY / MONTHLY
    private Integer frequency;         // 每周期执行次数
    private String scheduleDays;       // JSON: [1,3,5] 周几
    private String timeSlots;          // JSON: ["07:00-08:00"]
    private Boolean skipHolidays;
    private String inspectorIds;       // JSON: 指定检查员ID列表，空=项目全员可领取
    private Boolean isEnabled;
    private Integer sortOrder;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected InspectionPlan() {
    }

    private InspectionPlan(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId;
        this.projectId = builder.projectId;
        this.planName = builder.planName;
        this.rootSectionId = builder.rootSectionId;
        this.sectionIds = builder.sectionIds;
        this.scheduleMode = builder.scheduleMode != null ? builder.scheduleMode : "REGULAR";
        this.cycleType = builder.cycleType != null ? builder.cycleType : "DAILY";
        this.frequency = builder.frequency != null ? builder.frequency : 1;
        this.scheduleDays = builder.scheduleDays;
        this.timeSlots = builder.timeSlots;
        this.skipHolidays = builder.skipHolidays != null ? builder.skipHolidays : false;
        this.inspectorIds = builder.inspectorIds;
        this.isEnabled = builder.isEnabled != null ? builder.isEnabled : true;
        this.sortOrder = builder.sortOrder != null ? builder.sortOrder : 0;
        this.createdBy = builder.createdBy;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedAt = builder.updatedAt;
    }

    public static InspectionPlan create(Long projectId, String planName, String sectionIds, Long createdBy) {
        if (projectId == null) {
            throw new IllegalArgumentException("projectId 不能为空");
        }
        if (planName == null || planName.isBlank()) {
            throw new IllegalArgumentException("planName 不能为空");
        }
        return builder()
                .projectId(projectId)
                .planName(planName)
                .sectionIds(sectionIds)
                .createdBy(createdBy)
                .build();
    }

    public static InspectionPlan reconstruct(Builder builder) {
        return new InspectionPlan(builder);
    }

    public void updateInspectorIds(String inspectorIds) {
        this.inspectorIds = inspectorIds;
        this.updatedAt = LocalDateTime.now();
    }

    public void update(String planName, Long rootSectionId, String sectionIds, String scheduleMode,
                       String cycleType, Integer frequency, String scheduleDays,
                       String timeSlots, Boolean skipHolidays, Boolean isEnabled,
                       Integer sortOrder) {
        if (planName != null) this.planName = planName;
        if (rootSectionId != null) this.rootSectionId = rootSectionId;
        if (sectionIds != null) this.sectionIds = sectionIds;
        if (scheduleMode != null) this.scheduleMode = scheduleMode;
        if (cycleType != null) this.cycleType = cycleType;
        if (frequency != null) this.frequency = frequency;
        if (scheduleDays != null) this.scheduleDays = scheduleDays;
        if (timeSlots != null) this.timeSlots = timeSlots;
        if (skipHolidays != null) this.skipHolidays = skipHolidays;
        if (isEnabled != null) this.isEnabled = isEnabled;
        if (sortOrder != null) this.sortOrder = sortOrder;
        this.updatedAt = LocalDateTime.now();
    }

    public void enable() {
        this.isEnabled = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void disable() {
        this.isEnabled = false;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isOnDemand() {
        return "ON_DEMAND".equals(this.scheduleMode);
    }

    // Getters
    @Override
    public Long getId() { return id; }
    @Override
    public void setId(Long id) { this.id = id; }

    public Long getTenantId() { return tenantId; }
    public Long getProjectId() { return projectId; }
    public String getPlanName() { return planName; }
    public Long getRootSectionId() { return rootSectionId; }
    public String getSectionIds() { return sectionIds; }
    public String getScheduleMode() { return scheduleMode; }
    public String getCycleType() { return cycleType; }
    public Integer getFrequency() { return frequency; }
    public String getScheduleDays() { return scheduleDays; }
    public String getTimeSlots() { return timeSlots; }
    public Boolean getSkipHolidays() { return skipHolidays; }
    public String getInspectorIds() { return inspectorIds; }
    public Boolean getIsEnabled() { return isEnabled; }
    public Integer getSortOrder() { return sortOrder; }
    public Long getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long tenantId;
        private Long projectId;
        private String planName;
        private Long rootSectionId;
        private String sectionIds;
        private String scheduleMode;
        private String cycleType;
        private Integer frequency;
        private String scheduleDays;
        private String timeSlots;
        private Boolean skipHolidays;
        private String inspectorIds;
        private Boolean isEnabled;
        private Integer sortOrder;
        private Long createdBy;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(Long tenantId) { this.tenantId = tenantId; return this; }
        public Builder projectId(Long projectId) { this.projectId = projectId; return this; }
        public Builder planName(String planName) { this.planName = planName; return this; }
        public Builder rootSectionId(Long rootSectionId) { this.rootSectionId = rootSectionId; return this; }
        public Builder sectionIds(String sectionIds) { this.sectionIds = sectionIds; return this; }
        public Builder scheduleMode(String scheduleMode) { this.scheduleMode = scheduleMode; return this; }
        public Builder cycleType(String cycleType) { this.cycleType = cycleType; return this; }
        public Builder frequency(Integer frequency) { this.frequency = frequency; return this; }
        public Builder scheduleDays(String scheduleDays) { this.scheduleDays = scheduleDays; return this; }
        public Builder timeSlots(String timeSlots) { this.timeSlots = timeSlots; return this; }
        public Builder skipHolidays(Boolean skipHolidays) { this.skipHolidays = skipHolidays; return this; }
        public Builder inspectorIds(String inspectorIds) { this.inspectorIds = inspectorIds; return this; }
        public Builder isEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; return this; }
        public Builder sortOrder(Integer sortOrder) { this.sortOrder = sortOrder; return this; }
        public Builder createdBy(Long createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public InspectionPlan build() { return new InspectionPlan(this); }
    }
}
