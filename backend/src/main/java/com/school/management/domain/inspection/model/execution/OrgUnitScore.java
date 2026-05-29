package com.school.management.domain.inspection.model.execution;

import com.school.management.domain.shared.Entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 组织单元分数汇总 (规模公平性 Stage 5)
 *
 * <p>每个 (project, orgUnit, cycleDate) 一条记录, 记录该组织单元在该周期日的
 * roll-up 得分. 组织树自底向上用 MEAN (均值) 逐层滚动:
 * <ul>
 *   <li>叶子组织分 = 该组织下 submission 的均分, {@code sourceCount} = submission 数</li>
 *   <li>父组织分   = 直接子组织分的均值,        {@code childCount}  = 子组织数</li>
 * </ul>
 * 用均值而非求和: 求和会让子组织多的部门吃亏, 均值让数量自动抵消, 实现规模公平.
 */
public class OrgUnitScore implements Entity<Long> {

    private Long id;
    private Long tenantId;
    private Long projectId;
    private Long orgUnitId;
    private LocalDate cycleDate;
    private BigDecimal score;
    private String grade;
    private Integer childCount;
    private Integer sourceCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected OrgUnitScore() {
    }

    private OrgUnitScore(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId;
        this.projectId = builder.projectId;
        this.orgUnitId = builder.orgUnitId;
        this.cycleDate = builder.cycleDate;
        this.score = builder.score;
        this.grade = builder.grade;
        this.childCount = builder.childCount != null ? builder.childCount : 0;
        this.sourceCount = builder.sourceCount != null ? builder.sourceCount : 0;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedAt = builder.updatedAt;
    }

    public static OrgUnitScore create(Long projectId, Long orgUnitId, LocalDate cycleDate) {
        return builder()
                .projectId(projectId)
                .orgUnitId(orgUnitId)
                .cycleDate(cycleDate)
                .build();
    }

    public static OrgUnitScore reconstruct(Builder builder) {
        return new OrgUnitScore(builder);
    }

    public void updateScore(BigDecimal score, String grade, Integer childCount, Integer sourceCount) {
        this.score = score;
        this.grade = grade;
        this.childCount = childCount != null ? childCount : 0;
        this.sourceCount = sourceCount != null ? sourceCount : 0;
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public Long getProjectId() { return projectId; }
    public Long getOrgUnitId() { return orgUnitId; }
    public LocalDate getCycleDate() { return cycleDate; }
    public BigDecimal getScore() { return score; }
    public String getGrade() { return grade; }
    public Integer getChildCount() { return childCount; }
    public Integer getSourceCount() { return sourceCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long tenantId;
        private Long projectId;
        private Long orgUnitId;
        private LocalDate cycleDate;
        private BigDecimal score;
        private String grade;
        private Integer childCount;
        private Integer sourceCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(Long tenantId) { this.tenantId = tenantId; return this; }
        public Builder projectId(Long projectId) { this.projectId = projectId; return this; }
        public Builder orgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; return this; }
        public Builder cycleDate(LocalDate cycleDate) { this.cycleDate = cycleDate; return this; }
        public Builder score(BigDecimal score) { this.score = score; return this; }
        public Builder grade(String grade) { this.grade = grade; return this; }
        public Builder childCount(Integer childCount) { this.childCount = childCount; return this; }
        public Builder sourceCount(Integer sourceCount) { this.sourceCount = sourceCount; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public OrgUnitScore build() { return new OrgUnitScore(this); }
    }
}
