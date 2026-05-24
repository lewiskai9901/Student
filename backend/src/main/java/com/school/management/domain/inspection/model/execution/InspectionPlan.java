package com.school.management.domain.inspection.model.execution;

import com.school.management.domain.shared.AggregateRoot;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 检查计划 — 排期从项目层管理.
 *
 * <p>评级引擎完美架构 (2026-05-23): 撤销 scoringProfileId — 评分配置与调度组解耦,
 * 调度组只负责"何时/谁/哪些分区"; ratersPerTarget 仍保留 (那是真调度问题).
 */
public class InspectionPlan extends AggregateRoot<Long> {

    private Long tenantId;
    private Long projectId;
    private String planName;
    private Long rootSectionId;        // V66: 该计划使用的模板（根分区ID），不可空
    /**
     * 分区列表 (V20260524_4: 旧 JSON String → List&lt;Long&gt; 由 insp_plan_sections 关系表持久化).
     * 空集合 = 覆盖项目全部一级分区. getSectionIds() 返回 JSON 串供旧消费方兼容.
     */
    private List<Long> sectionIdList = new ArrayList<>();
    private String scheduleMode;       // REGULAR / ON_DEMAND
    private String cycleType;          // DAILY / WEEKLY / MONTHLY
    private Integer frequency;         // 每周期执行次数
    private String scheduleDays;       // JSON: [1,3,5] 周几
    private String timeSlots;          // JSON: ["07:00-08:00"] (V20260524_6: 支持跨日 "22:00-02:00")
    /** V20260524_6: RRULE 周期 (RFC 5545 子集). 非空时 scheduler 用 RecurrenceRule 计算, 忽略 cycleType/frequency/scheduleDays. */
    private String rrule;
    private Boolean skipHolidays;
    /**
     * 检查员列表 (V20260524_2 重构: 原 JSON String → List&lt;Long&gt; 由 insp_plan_inspectors 关系表持久化).
     * 空集合 = 项目全员可领取; getInspectorIds() 仍返回 JSON 串供旧消费方兼容使用.
     */
    private List<Long> inspectorUserIds = new ArrayList<>();
    /**
     * 检查员指派策略 (V20260524_3, smell A 修复):
     *   SPECIFIC = 限定到 inspectorUserIds 列出的人; OPEN_TO_ALL = 项目全员可领取.
     * 默认 OPEN_TO_ALL (兼容旧建无显式策略的 plan).
     */
    private AssignStrategy assignStrategy = AssignStrategy.OPEN_TO_ALL;
    /**
     * 每个检查目标的检查员份数 (1=单人评分, >1=多人评分). 默认 1.
     * 多人评分的合并算法由所引用 ScoringProfile.multiRaterMode 决定（按分区查 ScoringProfile）.
     */
    private Integer ratersPerTarget;
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
        this.sectionIdList = builder.sectionIdList != null
                ? new ArrayList<>(builder.sectionIdList)
                : (builder.sectionIds != null
                        ? parseInspectorIdsJson(builder.sectionIds) // 同样的数字解析规则
                        : new ArrayList<>());
        this.scheduleMode = builder.scheduleMode != null ? builder.scheduleMode : "REGULAR";
        this.cycleType = builder.cycleType != null ? builder.cycleType : "DAILY";
        this.frequency = builder.frequency != null ? builder.frequency : 1;
        this.scheduleDays = builder.scheduleDays;
        this.timeSlots = builder.timeSlots;
        this.rrule = builder.rrule;
        this.skipHolidays = builder.skipHolidays != null ? builder.skipHolidays : false;
        this.inspectorUserIds = builder.inspectorUserIds != null
                ? new ArrayList<>(builder.inspectorUserIds)
                : (builder.inspectorIds != null
                        ? parseInspectorIdsJson(builder.inspectorIds)
                        : new ArrayList<>());
        this.assignStrategy = builder.assignStrategy != null
                ? builder.assignStrategy
                : (this.inspectorUserIds.isEmpty() ? AssignStrategy.OPEN_TO_ALL : AssignStrategy.SPECIFIC);
        this.ratersPerTarget = builder.ratersPerTarget != null ? builder.ratersPerTarget : 1;
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

    /** 旧接口: 接受 JSON 字符串 (向后兼容). */
    public void updateInspectorIds(String inspectorIds) {
        this.inspectorUserIds = parseInspectorIdsJson(inspectorIds);
        this.updatedAt = LocalDateTime.now();
    }

    /** 新接口: 直接更新 user_id 列表. 空集合表示"项目全员可领取". */
    public void updateInspectorUserIds(List<Long> userIds) {
        this.inspectorUserIds = userIds != null ? new ArrayList<>(userIds) : new ArrayList<>();
        this.updatedAt = LocalDateTime.now();
    }

    /** 简易 JSON 数组解析 [1,2,3] → [1L,2L,3L]. 容错: 解析失败或空返回空列表. */
    private static List<Long> parseInspectorIdsJson(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        List<Long> result = new ArrayList<>();
        Matcher m = Pattern.compile("\\d+").matcher(json);
        while (m.find()) {
            try { result.add(Long.parseLong(m.group())); } catch (NumberFormatException ignored) { /* skip */ }
        }
        return result;
    }

    public void update(String planName, Long rootSectionId, String sectionIds, String scheduleMode,
                       String cycleType, Integer frequency, String scheduleDays,
                       String timeSlots, Boolean skipHolidays, Boolean isEnabled,
                       Integer sortOrder) {
        if (planName != null) this.planName = planName;
        if (rootSectionId != null) this.rootSectionId = rootSectionId;
        if (sectionIds != null) this.sectionIdList = parseInspectorIdsJson(sectionIds);
        if (scheduleMode != null) this.scheduleMode = scheduleMode;
        if (cycleType != null) this.cycleType = cycleType;
        if (frequency != null) this.frequency = frequency;
        if (scheduleDays != null) this.scheduleDays = scheduleDays;
        if (timeSlots != null) this.timeSlots = timeSlots;
        // rrule 由专门 setter 处理 (避免 update 签名再加参数)
        if (skipHolidays != null) this.skipHolidays = skipHolidays;
        if (isEnabled != null) this.isEnabled = isEnabled;
        if (sortOrder != null) this.sortOrder = sortOrder;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 评级引擎完美架构 (2026-05-23): 撤销 scoringProfileId — 仅保留 raters 维度.
     */
    public void updateRatersPerTarget(int ratersPerTarget) {
        if (ratersPerTarget < 1) {
            throw new IllegalArgumentException("ratersPerTarget 必须 >= 1, 当前值: " + ratersPerTarget);
        }
        this.ratersPerTarget = ratersPerTarget;
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
    public Long getTenantId() { return tenantId; }
    public Long getProjectId() { return projectId; }
    public String getPlanName() { return planName; }
    public Long getRootSectionId() { return rootSectionId; }
    /** 旧接口兼容: 返回 JSON 串 ["101","102"] (Jackson Long-as-string). */
    public String getSectionIds() {
        if (sectionIdList == null || sectionIdList.isEmpty()) return null;
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < sectionIdList.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append('"').append(sectionIdList.get(i)).append('"');
        }
        return sb.append("]").toString();
    }

    /** 主接口: 返回不可变 section_id 列表. */
    public List<Long> getSectionIdList() {
        return Collections.unmodifiableList(sectionIdList != null ? sectionIdList : new ArrayList<>());
    }
    public String getScheduleMode() { return scheduleMode; }
    public String getCycleType() { return cycleType; }
    public Integer getFrequency() { return frequency; }
    public String getScheduleDays() { return scheduleDays; }
    public String getTimeSlots() { return timeSlots; }

    /**
     * V20260524_6: 解析时段 JSON 为 TimeSlot 值对象列表.
     * 跨日 slot (如 22:00→02:00) 由 TimeSlot.isCrossDay() 自动识别.
     */
    public List<TimeSlot> getParsedTimeSlots() {
        return TimeSlot.parseList(timeSlots);
    }

    public String getRrule() { return rrule; }
    public void setRrule(String rrule) { this.rrule = rrule; this.updatedAt = LocalDateTime.now(); }
    public RecurrenceRule getParsedRrule() { return RecurrenceRule.parse(rrule); }
    public Boolean getSkipHolidays() { return skipHolidays; }
    /**
     * 旧接口兼容: 返回 JSON 串 ["1","2","3"] (Jackson Long-as-string 契约).
     * 空列表返回 null (与旧"未设置=全员"语义一致).
     * 新代码请用 {@link #getInspectorUserIds()}.
     */
    public String getInspectorIds() {
        if (inspectorUserIds == null || inspectorUserIds.isEmpty()) return null;
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < inspectorUserIds.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append('"').append(inspectorUserIds.get(i)).append('"');
        }
        return sb.append("]").toString();
    }

    /** 主接口: 返回不可变 user_id 列表. */
    public List<Long> getInspectorUserIds() {
        return Collections.unmodifiableList(inspectorUserIds != null ? inspectorUserIds : new ArrayList<>());
    }

    public AssignStrategy getAssignStrategy() {
        return assignStrategy != null ? assignStrategy : AssignStrategy.OPEN_TO_ALL;
    }

    /** smell A 修复: 显式切换指派策略, 同时校验 inspectorUserIds 与策略一致. */
    public void updateAssignStrategy(AssignStrategy strategy) {
        if (strategy == null) throw new IllegalArgumentException("assignStrategy 不能为空");
        this.assignStrategy = strategy;
        this.updatedAt = LocalDateTime.now();
        assertAssignStrategyInvariant();
    }

    /**
     * smell A: 不变量校验 — assignStrategy 与 inspectorUserIds 必须一致.
     *   SPECIFIC 必须有 ≥1 人; OPEN_TO_ALL 必须无人.
     */
    public void assertAssignStrategyInvariant() {
        AssignStrategy s = getAssignStrategy();
        boolean hasInspectors = inspectorUserIds != null && !inspectorUserIds.isEmpty();
        if (s == AssignStrategy.SPECIFIC && !hasInspectors) {
            throw new com.school.management.domain.inspection.exception.InvalidPlanStateException(
                "调度组指派策略=SPECIFIC 时必须指定至少 1 个检查员; 若要项目全员可领取请改成 OPEN_TO_ALL.");
        }
        if (s == AssignStrategy.OPEN_TO_ALL && hasInspectors) {
            throw new com.school.management.domain.inspection.exception.InvalidPlanStateException(
                "调度组指派策略=OPEN_TO_ALL 时不应指定具体检查员; 若要限定到名单请改成 SPECIFIC.");
        }
    }

    /**
     * smell B: 不变量校验 — scheduleMode 与其他调度字段必须一致.
     *   ON_DEMAND 时 cycleType/frequency/scheduleDays/timeSlots 应为空 (设了被忽略, 提醒人工修正).
     *   REGULAR 时 cycleType 必填.
     */
    public void assertScheduleModeInvariant() {
        String mode = this.scheduleMode != null ? this.scheduleMode : "REGULAR";
        if ("ON_DEMAND".equals(mode)) {
            if (cycleType != null || frequency != null || scheduleDays != null || timeSlots != null
                    || (rrule != null && !rrule.isBlank())) {
                throw new com.school.management.domain.inspection.exception.InvalidPlanStateException(
                    "ON_DEMAND 调度组不应设置 cycleType / frequency / scheduleDays / timeSlots / rrule; " +
                    "若要按周期触发请改成 REGULAR.");
            }
        } else if ("REGULAR".equals(mode)) {
            // V20260524_6: rrule 非空时不需要 cycleType (rrule 自带 FREQ)
            boolean hasRrule = rrule != null && !rrule.isBlank();
            if (!hasRrule && (cycleType == null || cycleType.isBlank())) {
                throw new com.school.management.domain.inspection.exception.InvalidPlanStateException(
                    "REGULAR 调度组必须指定 cycleType (DAILY / WEEKLY / MONTHLY) 或 rrule 表达式.");
            }
            // V20260524_6: rrule 非空时校验格式合法
            if (hasRrule && com.school.management.domain.inspection.model.execution.RecurrenceRule.parse(rrule) == null) {
                throw new com.school.management.domain.inspection.exception.InvalidPlanStateException(
                    "rrule 格式不合法: " + rrule + " (示例: FREQ=MONTHLY;BYDAY=2FR)");
            }
        } else {
            throw new com.school.management.domain.inspection.exception.InvalidPlanStateException(
                "未知 scheduleMode: " + mode + " (应为 REGULAR 或 ON_DEMAND)");
        }
    }
    public Integer getRatersPerTarget() { return ratersPerTarget == null ? 1 : ratersPerTarget; }
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
        private List<Long> sectionIdList;
        private String scheduleMode;
        private String cycleType;
        private Integer frequency;
        private String scheduleDays;
        private String timeSlots;
        private String rrule;
        private Boolean skipHolidays;
        private String inspectorIds;
        private List<Long> inspectorUserIds;
        private AssignStrategy assignStrategy;
        private Integer ratersPerTarget;
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
        public Builder sectionIdList(List<Long> ids) { this.sectionIdList = ids; return this; }
        public Builder scheduleMode(String scheduleMode) { this.scheduleMode = scheduleMode; return this; }
        public Builder cycleType(String cycleType) { this.cycleType = cycleType; return this; }
        public Builder frequency(Integer frequency) { this.frequency = frequency; return this; }
        public Builder scheduleDays(String scheduleDays) { this.scheduleDays = scheduleDays; return this; }
        public Builder timeSlots(String timeSlots) { this.timeSlots = timeSlots; return this; }
        public Builder rrule(String rrule) { this.rrule = rrule; return this; }
        public Builder skipHolidays(Boolean skipHolidays) { this.skipHolidays = skipHolidays; return this; }
        public Builder inspectorIds(String inspectorIds) { this.inspectorIds = inspectorIds; return this; }
        public Builder inspectorUserIds(List<Long> userIds) { this.inspectorUserIds = userIds; return this; }
        public Builder assignStrategy(AssignStrategy s) { this.assignStrategy = s; return this; }
        public Builder ratersPerTarget(Integer ratersPerTarget) { this.ratersPerTarget = ratersPerTarget; return this; }
        public Builder isEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; return this; }
        public Builder sortOrder(Integer sortOrder) { this.sortOrder = sortOrder; return this; }
        public Builder createdBy(Long createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public InspectionPlan build() { return new InspectionPlan(this); }
    }
}
