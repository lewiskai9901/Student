package com.school.management.domain.inspection.model.evaluation;

import com.school.management.domain.shared.AggregateRoot;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 评级结果聚合根 — 评级引擎完美架构 (2026-05-23).
 *
 * <p>由 IndicatorEvaluationService 计算后落库. 状态机:
 * <pre>
 *   DRAFT ──publish()──▶ PUBLISHED ──supersedeWith()──▶ SUPERSEDED
 *                                              │
 *                                              └▶ 新 DRAFT result (revisionOf=原 published.id)
 * </pre>
 *
 * <p>关键字段:
 * <ul>
 *   <li>{@code periodKey} 标识评估周期, 形如 "2026-W22" / "COUNT#7" / "MANUAL:2026-05-01_2026-05-31"</li>
 *   <li>{@code revisionOf} 指向前一个 PUBLISHED 版本, 形成修订链</li>
 *   <li>{@code orgUnitId} 数据权限边界 (反查 indicator.project.org_unit_id)</li>
 *   <li>{@code sourceSubmissionIds} / {@code sourceSectionIds} 溯源</li>
 * </ul>
 */
public class IndicatorResult extends AggregateRoot<Long> {

    private Long tenantId;
    private Long orgUnitId;
    private Long indicatorId;
    private Long targetId;
    private String targetName;
    private String periodKey;
    private BigDecimal value;
    private Integer rankPosition;
    private String grade;
    private ResultStatus status;
    private LocalDateTime computedAt;
    private LocalDateTime publishedAt;
    private Long revisionOf;
    private List<Long> sourceSubmissionIds = new ArrayList<>();
    private List<Long> sourceSectionIds = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected IndicatorResult() {}

    private IndicatorResult(Builder b) {
        this.id = b.id;
        this.tenantId = b.tenantId;
        this.orgUnitId = b.orgUnitId;
        this.indicatorId = b.indicatorId;
        this.targetId = b.targetId;
        this.targetName = b.targetName;
        this.periodKey = b.periodKey;
        this.value = b.value;
        this.rankPosition = b.rankPosition;
        this.grade = b.grade;
        this.status = b.status != null ? b.status : ResultStatus.DRAFT;
        this.computedAt = b.computedAt != null ? b.computedAt : LocalDateTime.now();
        this.publishedAt = b.publishedAt;
        this.revisionOf = b.revisionOf;
        this.sourceSubmissionIds = b.sourceSubmissionIds != null
                ? new ArrayList<>(b.sourceSubmissionIds) : new ArrayList<>();
        this.sourceSectionIds = b.sourceSectionIds != null
                ? new ArrayList<>(b.sourceSectionIds) : new ArrayList<>();
        this.createdAt = b.createdAt;
        this.updatedAt = b.updatedAt;
    }

    /** 工厂: 计算引擎产出新的 DRAFT 结果. */
    public static IndicatorResult draft(Long indicatorId, Long targetId, String targetName,
                                        String periodKey, BigDecimal value,
                                        Integer rankPosition, String grade,
                                        List<Long> sourceSubmissionIds,
                                        List<Long> sourceSectionIds) {
        if (indicatorId == null) throw new IllegalArgumentException("indicatorId 必填");
        if (targetId == null) throw new IllegalArgumentException("targetId 必填");
        if (periodKey == null || periodKey.isBlank()) {
            throw new IllegalArgumentException("periodKey 必填");
        }
        return builder()
                .indicatorId(indicatorId)
                .targetId(targetId)
                .targetName(targetName)
                .periodKey(periodKey)
                .value(value)
                .rankPosition(rankPosition)
                .grade(grade)
                .status(ResultStatus.DRAFT)
                .computedAt(LocalDateTime.now())
                .sourceSubmissionIds(sourceSubmissionIds)
                .sourceSectionIds(sourceSectionIds)
                .build();
    }

    public static IndicatorResult reconstruct(Builder b) {
        return new IndicatorResult(b);
    }

    // ── Commands ─────────────────────────────────────────────

    /** DRAFT → PUBLISHED. */
    public void publish() {
        if (this.status != ResultStatus.DRAFT) {
            throw new IllegalStateException(
                    "只有 DRAFT 状态可以发布, 当前: " + this.status);
        }
        this.status = ResultStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 用新 DRAFT 结果取代当前 PUBLISHED 结果: 当前置为 SUPERSEDED,
     * 返回新 DRAFT (revisionOf=当前 id), 由调用方负责保存新结果.
     */
    public IndicatorResult supersedeWith(BigDecimal newValue, Integer newRank, String newGrade,
                                         List<Long> newSourceSubmissionIds,
                                         List<Long> newSourceSectionIds) {
        if (this.status != ResultStatus.PUBLISHED) {
            throw new IllegalStateException(
                    "只能对 PUBLISHED 结果生成 supersede 链, 当前: " + this.status);
        }
        this.status = ResultStatus.SUPERSEDED;
        this.updatedAt = LocalDateTime.now();
        return builder()
                .indicatorId(this.indicatorId)
                .targetId(this.targetId)
                .targetName(this.targetName)
                .periodKey(this.periodKey)
                .value(newValue)
                .rankPosition(newRank)
                .grade(newGrade)
                .status(ResultStatus.DRAFT)
                .computedAt(LocalDateTime.now())
                .revisionOf(this.id)
                .orgUnitId(this.orgUnitId)
                .tenantId(this.tenantId)
                .sourceSubmissionIds(newSourceSubmissionIds)
                .sourceSectionIds(newSourceSectionIds)
                .build();
    }

    public void setOrgUnitId(Long orgUnitId) {
        this.orgUnitId = orgUnitId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    // ── Getters ──────────────────────────────────────────────

    public Long getTenantId() { return tenantId; }
    public Long getOrgUnitId() { return orgUnitId; }
    public Long getIndicatorId() { return indicatorId; }
    public Long getTargetId() { return targetId; }
    public String getTargetName() { return targetName; }
    public String getPeriodKey() { return periodKey; }
    public BigDecimal getValue() { return value; }
    public Integer getRankPosition() { return rankPosition; }
    public String getGrade() { return grade; }
    public ResultStatus getStatus() { return status; }
    public LocalDateTime getComputedAt() { return computedAt; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public Long getRevisionOf() { return revisionOf; }
    public List<Long> getSourceSubmissionIds() {
        return Collections.unmodifiableList(sourceSubmissionIds);
    }
    public List<Long> getSourceSectionIds() {
        return Collections.unmodifiableList(sourceSectionIds);
    }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // ── Builder ──────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long tenantId;
        private Long orgUnitId;
        private Long indicatorId;
        private Long targetId;
        private String targetName;
        private String periodKey;
        private BigDecimal value;
        private Integer rankPosition;
        private String grade;
        private ResultStatus status;
        private LocalDateTime computedAt;
        private LocalDateTime publishedAt;
        private Long revisionOf;
        private List<Long> sourceSubmissionIds;
        private List<Long> sourceSectionIds;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(Long tenantId) { this.tenantId = tenantId; return this; }
        public Builder orgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; return this; }
        public Builder indicatorId(Long indicatorId) { this.indicatorId = indicatorId; return this; }
        public Builder targetId(Long targetId) { this.targetId = targetId; return this; }
        public Builder targetName(String targetName) { this.targetName = targetName; return this; }
        public Builder periodKey(String periodKey) { this.periodKey = periodKey; return this; }
        public Builder value(BigDecimal value) { this.value = value; return this; }
        public Builder rankPosition(Integer rankPosition) { this.rankPosition = rankPosition; return this; }
        public Builder grade(String grade) { this.grade = grade; return this; }
        public Builder status(ResultStatus status) { this.status = status; return this; }
        public Builder computedAt(LocalDateTime computedAt) { this.computedAt = computedAt; return this; }
        public Builder publishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; return this; }
        public Builder revisionOf(Long revisionOf) { this.revisionOf = revisionOf; return this; }
        public Builder sourceSubmissionIds(List<Long> ids) { this.sourceSubmissionIds = ids; return this; }
        public Builder sourceSectionIds(List<Long> ids) { this.sourceSectionIds = ids; return this; }
        public Builder createdAt(LocalDateTime v) { this.createdAt = v; return this; }
        public Builder updatedAt(LocalDateTime v) { this.updatedAt = v; return this; }

        public IndicatorResult build() {
            return new IndicatorResult(this);
        }
    }
}
