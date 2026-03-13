package com.school.management.domain.inspection.model.v7.compliance;

import com.school.management.domain.shared.AggregateRoot;

import java.time.LocalDateTime;

/**
 * 合规条款实体
 */
public class ComplianceClause extends AggregateRoot<Long> {

    private Long id;
    private Long standardId;
    private String clauseNumber;
    private String clauseTitle;
    private String clauseContent;
    private Long parentClauseId;
    private Integer sortOrder;
    private LocalDateTime createdAt;

    protected ComplianceClause() {
    }

    private ComplianceClause(Builder builder) {
        this.id = builder.id;
        this.standardId = builder.standardId;
        this.clauseNumber = builder.clauseNumber;
        this.clauseTitle = builder.clauseTitle;
        this.clauseContent = builder.clauseContent;
        this.parentClauseId = builder.parentClauseId;
        this.sortOrder = builder.sortOrder != null ? builder.sortOrder : 0;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
    }

    public static ComplianceClause create(Long standardId, String number, String title) {
        return builder()
                .standardId(standardId)
                .clauseNumber(number)
                .clauseTitle(title)
                .build();
    }

    public void update(String clauseTitle, String clauseContent, Long parentClauseId, Integer sortOrder) {
        this.clauseTitle = clauseTitle;
        this.clauseContent = clauseContent;
        this.parentClauseId = parentClauseId;
        this.sortOrder = sortOrder;
    }

    public static ComplianceClause reconstruct(Builder builder) {
        return new ComplianceClause(builder);
    }

    @Override
    public Long getId() { return id; }

    @Override
    public void setId(Long id) { this.id = id; }

    public Long getStandardId() { return standardId; }
    public String getClauseNumber() { return clauseNumber; }
    public String getClauseTitle() { return clauseTitle; }
    public String getClauseContent() { return clauseContent; }
    public Long getParentClauseId() { return parentClauseId; }
    public Integer getSortOrder() { return sortOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long standardId;
        private String clauseNumber;
        private String clauseTitle;
        private String clauseContent;
        private Long parentClauseId;
        private Integer sortOrder;
        private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder standardId(Long standardId) { this.standardId = standardId; return this; }
        public Builder clauseNumber(String clauseNumber) { this.clauseNumber = clauseNumber; return this; }
        public Builder clauseTitle(String clauseTitle) { this.clauseTitle = clauseTitle; return this; }
        public Builder clauseContent(String clauseContent) { this.clauseContent = clauseContent; return this; }
        public Builder parentClauseId(Long parentClauseId) { this.parentClauseId = parentClauseId; return this; }
        public Builder sortOrder(Integer sortOrder) { this.sortOrder = sortOrder; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public ComplianceClause build() { return new ComplianceClause(this); }
    }
}
