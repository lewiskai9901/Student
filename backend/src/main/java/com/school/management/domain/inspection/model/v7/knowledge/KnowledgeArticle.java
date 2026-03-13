package com.school.management.domain.inspection.model.v7.knowledge;

import com.school.management.domain.shared.AggregateRoot;

import java.time.LocalDateTime;

/**
 * V7 知识库文章聚合根
 * 支持从整改案例自动创建，全文搜索，置顶等功能。
 */
public class KnowledgeArticle extends AggregateRoot<Long> {

    private Long id;
    private Long tenantId;
    private String title;
    private String content;
    private String category;
    private String tags;
    private String relatedItemCodes; // JSON array
    private Long sourceCaseId;
    private Integer viewCount;
    private Integer helpfulCount;
    private Boolean isPinned;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected KnowledgeArticle() {
    }

    private KnowledgeArticle(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId != null ? builder.tenantId : 0L;
        this.title = builder.title;
        this.content = builder.content;
        this.category = builder.category;
        this.tags = builder.tags;
        this.relatedItemCodes = builder.relatedItemCodes;
        this.sourceCaseId = builder.sourceCaseId;
        this.viewCount = builder.viewCount != null ? builder.viewCount : 0;
        this.helpfulCount = builder.helpfulCount != null ? builder.helpfulCount : 0;
        this.isPinned = builder.isPinned != null ? builder.isPinned : false;
        this.createdBy = builder.createdBy;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedAt = builder.updatedAt;
    }

    public static KnowledgeArticle create(String title, String content, Long createdBy) {
        return builder()
                .title(title)
                .content(content)
                .createdBy(createdBy)
                .build();
    }

    public static KnowledgeArticle reconstruct(Builder builder) {
        return new KnowledgeArticle(builder);
    }

    /**
     * 更新文章内容
     */
    public void update(String title, String content, String category,
                       String tags, String relatedItemCodes) {
        this.title = title;
        this.content = content;
        this.category = category;
        this.tags = tags;
        this.relatedItemCodes = relatedItemCodes;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 增加浏览计数
     */
    public void incrementViewCount() {
        this.viewCount = this.viewCount + 1;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 增加有用计数
     */
    public void incrementHelpfulCount() {
        this.helpfulCount = this.helpfulCount + 1;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 置顶
     */
    public void pin() {
        this.isPinned = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 取消置顶
     */
    public void unpin() {
        this.isPinned = false;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    @Override
    public Long getId() { return id; }

    @Override
    public void setId(Long id) { this.id = id; }

    public Long getTenantId() { return tenantId; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getCategory() { return category; }
    public String getTags() { return tags; }
    public String getRelatedItemCodes() { return relatedItemCodes; }
    public Long getSourceCaseId() { return sourceCaseId; }
    public Integer getViewCount() { return viewCount; }
    public Integer getHelpfulCount() { return helpfulCount; }
    public Boolean getIsPinned() { return isPinned; }
    public Long getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long tenantId;
        private String title;
        private String content;
        private String category;
        private String tags;
        private String relatedItemCodes;
        private Long sourceCaseId;
        private Integer viewCount;
        private Integer helpfulCount;
        private Boolean isPinned;
        private Long createdBy;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(Long tenantId) { this.tenantId = tenantId; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder content(String content) { this.content = content; return this; }
        public Builder category(String category) { this.category = category; return this; }
        public Builder tags(String tags) { this.tags = tags; return this; }
        public Builder relatedItemCodes(String relatedItemCodes) { this.relatedItemCodes = relatedItemCodes; return this; }
        public Builder sourceCaseId(Long sourceCaseId) { this.sourceCaseId = sourceCaseId; return this; }
        public Builder viewCount(Integer viewCount) { this.viewCount = viewCount; return this; }
        public Builder helpfulCount(Integer helpfulCount) { this.helpfulCount = helpfulCount; return this; }
        public Builder isPinned(Boolean isPinned) { this.isPinned = isPinned; return this; }
        public Builder createdBy(Long createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public KnowledgeArticle build() { return new KnowledgeArticle(this); }
    }
}
