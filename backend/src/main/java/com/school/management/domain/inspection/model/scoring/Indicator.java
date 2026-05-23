package com.school.management.domain.inspection.model.scoring;

import com.school.management.domain.shared.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 评价指标 — 聚合根 (评级引擎完美架构, 2026-05-23).
 *
 * <p>升级要点:
 * <ul>
 *   <li>{@code sourceSectionIds} 取代单一 {@code sourceSectionId}, 支持跨分区组合</li>
 *   <li>{@code triggerMode}: TIME_WINDOW(默认) / COUNT / MANUAL 三种触发模式</li>
 *   <li>{@code countThreshold}: COUNT 模式累计 N 次触发</li>
 *   <li>{@code weightsBySection}: 跨分区加权 {sectionId: weight}</li>
 *   <li>{@code rankDirection}: ASC=越小越好 / DESC=越大越好 / null=不排名</li>
 *   <li>{@code latePolicy} / {@code submissionDateField} 政策化逾期与归属</li>
 * </ul>
 *
 * <p>{@code sourceSectionId} 单字段保留过渡期向后兼容, 不再生效.
 */
public class Indicator implements Entity<Long> {

    private Long id;
    private Long tenantId;
    private Long projectId;
    private Long parentIndicatorId;
    private String name;
    private String indicatorType; // LEAF | COMPOSITE

    // LEAF fields
    /** @deprecated 单分区字段, 用 {@link #sourceSectionIds} 替代. 仍写入 PO 取首元素. */
    @Deprecated
    private Long sourceSectionId;
    /** 多分区组合(单分区也写成单元素 list). 至少 1 个. */
    private List<Long> sourceSectionIds = new ArrayList<>();
    private String sourceAggregation; // AVG | MAX | MIN | LATEST | SUM

    // COMPOSITE fields
    private String compositeAggregation; // WEIGHTED_AVG | SUM | AVG | MIN | MAX
    private MissingPolicy missingPolicy; // 详见 MissingPolicy

    // Normalization
    private String normalization;       // NONE | RELATION_COUNT | FIXED_VALUE | PERCENTAGE
    private String normalizationConfig; // JSON config

    // 评级引擎完美架构新字段
    private TriggerMode triggerMode;             // TIME_WINDOW(默认) / COUNT / MANUAL
    private Integer countThreshold;              // COUNT 模式累计阈值
    private Map<Long, BigDecimal> weightsBySection = new LinkedHashMap<>(); // 跨分区加权
    private RankDirection rankDirection;         // null=不排名
    private LatePolicy latePolicy;               // 默认 REVISE_ORIGINAL
    private SubmissionDateField submissionDateField; // 默认 taskDate

    private String evaluationPeriod; // PER_TASK | DAILY | WEEKLY | MONTHLY
    private Long gradeSchemeId;
    private String evaluationMethod;    // SCORE_RANGE | PERCENT_RANGE | RANK_COUNT | RANK_PERCENT
    private String gradeThresholds;     // JSON: [{"gradeCode":"RED","value":90},...]
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected Indicator() {
    }

    private Indicator(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId;
        this.projectId = builder.projectId;
        this.parentIndicatorId = builder.parentIndicatorId;
        this.name = builder.name;
        this.indicatorType = builder.indicatorType;
        this.sourceSectionId = builder.sourceSectionId;
        // sourceSectionIds: builder 显式 list > 单 sectionId 兜底
        if (builder.sourceSectionIds != null && !builder.sourceSectionIds.isEmpty()) {
            this.sourceSectionIds = new ArrayList<>(builder.sourceSectionIds);
        } else if (builder.sourceSectionId != null) {
            this.sourceSectionIds = new ArrayList<>();
            this.sourceSectionIds.add(builder.sourceSectionId);
        } else {
            this.sourceSectionIds = new ArrayList<>();
        }
        this.sourceAggregation = builder.sourceAggregation;
        this.compositeAggregation = builder.compositeAggregation;
        this.missingPolicy = builder.missingPolicy != null ? builder.missingPolicy : MissingPolicy.IGNORE;
        this.normalization = builder.normalization != null ? builder.normalization : "NONE";
        this.normalizationConfig = builder.normalizationConfig;
        this.triggerMode = builder.triggerMode != null ? builder.triggerMode : TriggerMode.TIME_WINDOW;
        this.countThreshold = builder.countThreshold;
        this.weightsBySection = builder.weightsBySection != null
                ? new LinkedHashMap<>(builder.weightsBySection) : new LinkedHashMap<>();
        this.rankDirection = builder.rankDirection;
        this.latePolicy = builder.latePolicy != null ? builder.latePolicy : LatePolicy.REVISE_ORIGINAL;
        this.submissionDateField = builder.submissionDateField != null
                ? builder.submissionDateField : SubmissionDateField.taskDate;
        this.evaluationPeriod = builder.evaluationPeriod != null ? builder.evaluationPeriod : "PER_TASK";
        this.gradeSchemeId = builder.gradeSchemeId;
        this.evaluationMethod = builder.evaluationMethod != null ? builder.evaluationMethod : "PERCENT_RANGE";
        this.gradeThresholds = builder.gradeThresholds;
        this.sortOrder = builder.sortOrder != null ? builder.sortOrder : 0;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedAt = builder.updatedAt;
    }

    // ── Factory ──────────────────────────────────────────────

    public static Indicator createLeaf(Long projectId, String name,
                                       Long sourceSectionId, String sourceAggregation,
                                       String evaluationPeriod, Long gradeSchemeId) {
        Indicator ind = new Indicator();
        ind.projectId = projectId;
        ind.name = name;
        ind.indicatorType = "LEAF";
        ind.sourceSectionId = sourceSectionId;
        ind.sourceSectionIds = new ArrayList<>();
        if (sourceSectionId != null) ind.sourceSectionIds.add(sourceSectionId);
        ind.sourceAggregation = sourceAggregation != null ? sourceAggregation : "AVG";
        ind.evaluationPeriod = evaluationPeriod != null ? evaluationPeriod : "PER_TASK";
        ind.gradeSchemeId = gradeSchemeId;
        ind.sortOrder = 0;
        ind.missingPolicy = MissingPolicy.IGNORE;
        ind.triggerMode = TriggerMode.TIME_WINDOW;
        ind.latePolicy = LatePolicy.REVISE_ORIGINAL;
        ind.submissionDateField = SubmissionDateField.taskDate;
        ind.createdAt = LocalDateTime.now();
        return ind;
    }

    public static Indicator createComposite(Long projectId, String name,
                                            String compositeAggregation, String missingPolicy,
                                            String evaluationPeriod, Long gradeSchemeId) {
        Indicator ind = new Indicator();
        ind.projectId = projectId;
        ind.name = name;
        ind.indicatorType = "COMPOSITE";
        ind.compositeAggregation = compositeAggregation != null ? compositeAggregation : "AVG";
        ind.missingPolicy = MissingPolicy.fromString(missingPolicy);
        ind.evaluationPeriod = evaluationPeriod != null ? evaluationPeriod : "WEEKLY";
        ind.gradeSchemeId = gradeSchemeId;
        ind.sortOrder = 0;
        ind.triggerMode = TriggerMode.TIME_WINDOW;
        ind.latePolicy = LatePolicy.REVISE_ORIGINAL;
        ind.submissionDateField = SubmissionDateField.taskDate;
        ind.createdAt = LocalDateTime.now();
        ind.sourceSectionIds = new ArrayList<>();
        return ind;
    }

    public static Indicator reconstruct(Builder builder) {
        return new Indicator(builder);
    }

    // ── Commands ─────────────────────────────────────────────

    public void update(String name, String evaluationPeriod,
                       Long gradeSchemeId, Long sourceSectionId, String sourceAggregation,
                       String compositeAggregation, String missingPolicy,
                       String normalization, String normalizationConfig,
                       String evaluationMethod, String gradeThresholds,
                       Integer sortOrder) {
        this.name = name;
        this.evaluationPeriod = evaluationPeriod;
        this.gradeSchemeId = gradeSchemeId;
        this.sourceSectionId = sourceSectionId;
        if (sourceSectionId != null) {
            this.sourceSectionIds = new ArrayList<>();
            this.sourceSectionIds.add(sourceSectionId);
        }
        this.sourceAggregation = sourceAggregation;
        this.compositeAggregation = compositeAggregation;
        this.missingPolicy = MissingPolicy.fromString(missingPolicy);
        this.normalization = normalization;
        this.normalizationConfig = normalizationConfig;
        this.evaluationMethod = evaluationMethod;
        this.gradeThresholds = gradeThresholds;
        if (sortOrder != null) {
            this.sortOrder = sortOrder;
        }
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 评级引擎完美架构 (2026-05-23): 更新评级引擎全套配置.
     *
     * <p>业务规则:
     * <ul>
     *   <li>{@code sourceSectionIds} 至少 1 个</li>
     *   <li>{@code weightsBySection} 的 key 必须是 sourceSectionIds 的子集</li>
     *   <li>{@code triggerMode==COUNT} 时 countThreshold 必须 >= 1</li>
     * </ul>
     */
    public void updateEvaluationConfig(List<Long> sourceSectionIds,
                                       TriggerMode triggerMode,
                                       Integer countThreshold,
                                       Map<Long, BigDecimal> weightsBySection,
                                       RankDirection rankDirection,
                                       MissingPolicy missingPolicy,
                                       LatePolicy latePolicy,
                                       SubmissionDateField submissionDateField,
                                       Long gradeSchemeId,
                                       String evaluationPeriod) {
        validateEvaluationConfig(sourceSectionIds, triggerMode, countThreshold, weightsBySection);
        this.sourceSectionIds = new ArrayList<>(sourceSectionIds);
        if (!sourceSectionIds.isEmpty()) this.sourceSectionId = sourceSectionIds.get(0);
        this.triggerMode = triggerMode != null ? triggerMode : TriggerMode.TIME_WINDOW;
        this.countThreshold = countThreshold;
        this.weightsBySection = weightsBySection != null
                ? new LinkedHashMap<>(weightsBySection) : new LinkedHashMap<>();
        this.rankDirection = rankDirection;
        this.missingPolicy = missingPolicy != null ? missingPolicy : MissingPolicy.IGNORE;
        this.latePolicy = latePolicy != null ? latePolicy : LatePolicy.REVISE_ORIGINAL;
        this.submissionDateField = submissionDateField != null
                ? submissionDateField : SubmissionDateField.taskDate;
        if (gradeSchemeId != null) this.gradeSchemeId = gradeSchemeId;
        if (evaluationPeriod != null) this.evaluationPeriod = evaluationPeriod;
        this.updatedAt = LocalDateTime.now();
    }

    /** 业务规则校验, 外部 (Application Service / 数据迁移) 也可调用做预校验. */
    public static void validateEvaluationConfig(List<Long> sourceSectionIds,
                                                TriggerMode triggerMode,
                                                Integer countThreshold,
                                                Map<Long, BigDecimal> weightsBySection) {
        if (sourceSectionIds == null || sourceSectionIds.isEmpty()) {
            throw new IllegalArgumentException("sourceSectionIds 至少 1 个");
        }
        for (Long id : sourceSectionIds) {
            if (id == null) {
                throw new IllegalArgumentException("sourceSectionIds 不能包含 null");
            }
        }
        if (weightsBySection != null && !weightsBySection.isEmpty()) {
            for (Long k : weightsBySection.keySet()) {
                if (!sourceSectionIds.contains(k)) {
                    throw new IllegalArgumentException(
                            "weightsBySection 的 key " + k
                                    + " 必须是 sourceSectionIds " + sourceSectionIds + " 子集");
                }
            }
        }
        if (triggerMode == TriggerMode.COUNT) {
            if (countThreshold == null || countThreshold < 1) {
                throw new IllegalArgumentException(
                        "COUNT 触发模式 countThreshold 必须 >= 1, 当前: " + countThreshold);
            }
        }
    }

    public void setParentIndicatorId(Long parentIndicatorId) {
        this.parentIndicatorId = parentIndicatorId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
        this.updatedAt = LocalDateTime.now();
    }

    // ── Queries ──────────────────────────────────────────────

    public boolean isLeaf() {
        return "LEAF".equals(indicatorType);
    }

    public boolean isComposite() {
        return "COMPOSITE".equals(indicatorType);
    }

    public boolean isRoot() {
        return parentIndicatorId == null;
    }

    // ── Getters ──────────────────────────────────────────────

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Long getParentIndicatorId() {
        return parentIndicatorId;
    }

    public String getName() {
        return name;
    }

    public String getIndicatorType() {
        return indicatorType;
    }

    /** @deprecated 用 {@link #getSourceSectionIds()}; 仍保留为旧 PO 列首元素 */
    @Deprecated
    public Long getSourceSectionId() {
        return sourceSectionId;
    }

    public List<Long> getSourceSectionIds() {
        return Collections.unmodifiableList(sourceSectionIds);
    }

    public String getSourceAggregation() {
        return sourceAggregation;
    }

    public String getCompositeAggregation() {
        return compositeAggregation;
    }

    /** 返回 enum; 旧返回 String 的 getter 已 deprecate. */
    public MissingPolicy getMissingPolicy() {
        return missingPolicy;
    }

    /** @deprecated 历史 String 接口, 新代码用 {@link #getMissingPolicy()} */
    @Deprecated
    public String getMissingPolicyName() {
        return missingPolicy != null ? missingPolicy.name() : null;
    }

    public String getNormalization() { return normalization; }
    public String getNormalizationConfig() { return normalizationConfig; }

    public TriggerMode getTriggerMode() { return triggerMode; }
    public Integer getCountThreshold() { return countThreshold; }
    public Map<Long, BigDecimal> getWeightsBySection() {
        return Collections.unmodifiableMap(weightsBySection);
    }
    public RankDirection getRankDirection() { return rankDirection; }
    public LatePolicy getLatePolicy() { return latePolicy; }
    public SubmissionDateField getSubmissionDateField() { return submissionDateField; }

    public String getEvaluationPeriod() {
        return evaluationPeriod;
    }

    public Long getGradeSchemeId() {
        return gradeSchemeId;
    }

    public String getEvaluationMethod() {
        return evaluationMethod;
    }

    public String getGradeThresholds() {
        return gradeThresholds;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // ── Builder ──────────────────────────────────────────────

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long tenantId;
        private Long projectId;
        private Long parentIndicatorId;
        private String name;
        private String indicatorType;
        private Long sourceSectionId;
        private List<Long> sourceSectionIds;
        private String sourceAggregation;
        private String compositeAggregation;
        private MissingPolicy missingPolicy;
        private String normalization;
        private String normalizationConfig;
        private TriggerMode triggerMode;
        private Integer countThreshold;
        private Map<Long, BigDecimal> weightsBySection;
        private RankDirection rankDirection;
        private LatePolicy latePolicy;
        private SubmissionDateField submissionDateField;
        private String evaluationPeriod;
        private Long gradeSchemeId;
        private String evaluationMethod;
        private String gradeThresholds;
        private Integer sortOrder;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(Long tenantId) { this.tenantId = tenantId; return this; }
        public Builder projectId(Long projectId) { this.projectId = projectId; return this; }
        public Builder parentIndicatorId(Long parentIndicatorId) { this.parentIndicatorId = parentIndicatorId; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder indicatorType(String indicatorType) { this.indicatorType = indicatorType; return this; }
        public Builder sourceSectionId(Long sourceSectionId) { this.sourceSectionId = sourceSectionId; return this; }
        public Builder sourceSectionIds(List<Long> sourceSectionIds) {
            this.sourceSectionIds = sourceSectionIds; return this;
        }
        public Builder sourceAggregation(String sourceAggregation) { this.sourceAggregation = sourceAggregation; return this; }
        public Builder compositeAggregation(String compositeAggregation) { this.compositeAggregation = compositeAggregation; return this; }
        /** 接受 String 形式 missingPolicy (兼容旧 builder 调用); 内部解析为 enum. */
        public Builder missingPolicy(String missingPolicy) {
            this.missingPolicy = MissingPolicy.fromString(missingPolicy); return this;
        }
        public Builder missingPolicy(MissingPolicy missingPolicy) {
            this.missingPolicy = missingPolicy; return this;
        }
        public Builder normalization(String normalization) { this.normalization = normalization; return this; }
        public Builder normalizationConfig(String normalizationConfig) { this.normalizationConfig = normalizationConfig; return this; }
        public Builder triggerMode(TriggerMode triggerMode) { this.triggerMode = triggerMode; return this; }
        public Builder countThreshold(Integer countThreshold) { this.countThreshold = countThreshold; return this; }
        public Builder weightsBySection(Map<Long, BigDecimal> weightsBySection) {
            this.weightsBySection = weightsBySection; return this;
        }
        public Builder rankDirection(RankDirection rankDirection) { this.rankDirection = rankDirection; return this; }
        public Builder latePolicy(LatePolicy latePolicy) { this.latePolicy = latePolicy; return this; }
        public Builder submissionDateField(SubmissionDateField submissionDateField) {
            this.submissionDateField = submissionDateField; return this;
        }
        public Builder evaluationPeriod(String evaluationPeriod) { this.evaluationPeriod = evaluationPeriod; return this; }
        public Builder gradeSchemeId(Long gradeSchemeId) { this.gradeSchemeId = gradeSchemeId; return this; }
        public Builder evaluationMethod(String evaluationMethod) { this.evaluationMethod = evaluationMethod; return this; }
        public Builder gradeThresholds(String gradeThresholds) { this.gradeThresholds = gradeThresholds; return this; }
        public Builder sortOrder(Integer sortOrder) { this.sortOrder = sortOrder; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public Indicator build() {
            return new Indicator(this);
        }
    }
}
