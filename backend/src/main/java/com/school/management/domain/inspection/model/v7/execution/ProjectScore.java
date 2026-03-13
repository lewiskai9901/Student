package com.school.management.domain.inspection.model.v7.execution;

import com.school.management.domain.shared.Entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 项目分数汇总
 * 每个项目每个周期日期一条记录，记录当日汇总得分和等级
 */
public class ProjectScore implements Entity<Long> {

    private Long id;
    private Long tenantId;
    private Long projectId;
    private LocalDate cycleDate;
    private BigDecimal score;
    private String grade;
    private Integer targetCount;
    private String detail; // JSON
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected ProjectScore() {
    }

    private ProjectScore(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId;
        this.projectId = builder.projectId;
        this.cycleDate = builder.cycleDate;
        this.score = builder.score;
        this.grade = builder.grade;
        this.targetCount = builder.targetCount != null ? builder.targetCount : 0;
        this.detail = builder.detail;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedAt = builder.updatedAt;
    }

    public static ProjectScore create(Long projectId, LocalDate cycleDate) {
        return builder()
                .projectId(projectId)
                .cycleDate(cycleDate)
                .build();
    }

    public static ProjectScore reconstruct(Builder builder) {
        return new ProjectScore(builder);
    }

    public void updateScore(BigDecimal score, String grade, Integer targetCount, String detail) {
        this.score = score;
        this.grade = grade;
        this.targetCount = targetCount;
        this.detail = detail;
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public Long getProjectId() { return projectId; }
    public LocalDate getCycleDate() { return cycleDate; }
    public BigDecimal getScore() { return score; }
    public String getGrade() { return grade; }
    public Integer getTargetCount() { return targetCount; }
    public String getDetail() { return detail; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long tenantId;
        private Long projectId;
        private LocalDate cycleDate;
        private BigDecimal score;
        private String grade;
        private Integer targetCount;
        private String detail;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(Long tenantId) { this.tenantId = tenantId; return this; }
        public Builder projectId(Long projectId) { this.projectId = projectId; return this; }
        public Builder cycleDate(LocalDate cycleDate) { this.cycleDate = cycleDate; return this; }
        public Builder score(BigDecimal score) { this.score = score; return this; }
        public Builder grade(String grade) { this.grade = grade; return this; }
        public Builder targetCount(Integer targetCount) { this.targetCount = targetCount; return this; }
        public Builder detail(String detail) { this.detail = detail; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public ProjectScore build() { return new ProjectScore(this); }
    }
}
