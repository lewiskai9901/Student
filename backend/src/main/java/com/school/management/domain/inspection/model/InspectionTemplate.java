package com.school.management.domain.inspection.model;

import com.school.management.domain.inspection.event.InspectionTemplateCreatedEvent;
import com.school.management.domain.shared.AggregateRoot;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * InspectionTemplate Aggregate Root.
 * Represents a template for quantification checks with categories and deduction items.
 */
public class InspectionTemplate extends AggregateRoot<Long> {

    private Long id;
    private String templateCode;
    private String templateName;
    private String description;
    private TemplateScope scope;
    private Long applicableOrgUnitId;
    private Boolean isDefault;
    private Integer currentVersion;
    private TemplateStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;

    private List<InspectionCategory> categories;

    // For JPA/MyBatis
    protected InspectionTemplate() {
        this.categories = new ArrayList<>();
    }

    private InspectionTemplate(Builder builder) {
        this.id = builder.id;
        this.templateCode = Objects.requireNonNull(builder.templateCode, "templateCode cannot be null");
        this.templateName = Objects.requireNonNull(builder.templateName, "templateName cannot be null");
        this.description = builder.description;
        this.scope = builder.scope != null ? builder.scope : TemplateScope.GLOBAL;
        this.applicableOrgUnitId = builder.applicableOrgUnitId;
        this.isDefault = builder.isDefault != null ? builder.isDefault : false;
        this.currentVersion = builder.currentVersion != null ? builder.currentVersion : 1;
        this.status = builder.status != null ? builder.status : TemplateStatus.DRAFT;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        this.createdBy = builder.createdBy;
        this.categories = builder.categories != null ? new ArrayList<>(builder.categories) : new ArrayList<>();

        validate();
    }

    /**
     * Factory method to create a new inspection template.
     */
    public static InspectionTemplate create(String templateCode, String templateName,
                                            String description, TemplateScope scope,
                                            Long applicableOrgUnitId, Long createdBy) {
        InspectionTemplate template = builder()
            .templateCode(templateCode)
            .templateName(templateName)
            .description(description)
            .scope(scope)
            .applicableOrgUnitId(applicableOrgUnitId)
            .createdBy(createdBy)
            .build();

        template.registerEvent(new InspectionTemplateCreatedEvent(template));
        return template;
    }

    /**
     * Adds a category to this template.
     */
    public void addCategory(InspectionCategory category) {
        if (status != TemplateStatus.DRAFT) {
            throw new IllegalStateException("Cannot modify a non-draft template");
        }
        this.categories.add(category);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Removes a category from this template.
     */
    public void removeCategory(Long categoryId) {
        if (status != TemplateStatus.DRAFT) {
            throw new IllegalStateException("Cannot modify a non-draft template");
        }
        this.categories.removeIf(c -> c.getId().equals(categoryId));
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Publishes the template, making it available for use.
     */
    public void publish() {
        if (this.categories.isEmpty()) {
            throw new IllegalStateException("Cannot publish template without categories");
        }
        this.status = TemplateStatus.PUBLISHED;
        this.currentVersion++;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Archives the template.
     */
    public void archive() {
        this.status = TemplateStatus.ARCHIVED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Sets this template as the default for its scope.
     */
    public void setAsDefault() {
        if (this.status != TemplateStatus.PUBLISHED) {
            throw new IllegalStateException("Only published templates can be set as default");
        }
        this.isDefault = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Updates template information.
     */
    public void updateInfo(String templateName, String description) {
        if (templateName != null && !templateName.isBlank()) {
            this.templateName = templateName;
        }
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Calculates the total base score for this template.
     */
    public int calculateTotalBaseScore() {
        return categories.stream()
            .mapToInt(InspectionCategory::getBaseScore)
            .sum();
    }

    private void validate() {
        if (templateCode == null || templateCode.isBlank()) {
            throw new IllegalArgumentException("Template code cannot be empty");
        }
        if (templateCode.length() > 50) {
            throw new IllegalArgumentException("Template code cannot exceed 50 characters");
        }
        if (templateName == null || templateName.isBlank()) {
            throw new IllegalArgumentException("Template name cannot be empty");
        }
        if (templateName.length() > 100) {
            throw new IllegalArgumentException("Template name cannot exceed 100 characters");
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

    public String getTemplateCode() {
        return templateCode;
    }

    public String getTemplateName() {
        return templateName;
    }

    public String getDescription() {
        return description;
    }

    public TemplateScope getScope() {
        return scope;
    }

    public Long getApplicableOrgUnitId() {
        return applicableOrgUnitId;
    }

    public Boolean isDefault() {
        return isDefault;
    }

    public Integer getCurrentVersion() {
        return currentVersion;
    }

    public TemplateStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public List<InspectionCategory> getCategories() {
        return Collections.unmodifiableList(categories);
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String templateCode;
        private String templateName;
        private String description;
        private TemplateScope scope;
        private Long applicableOrgUnitId;
        private Boolean isDefault;
        private Integer currentVersion;
        private TemplateStatus status;
        private Long createdBy;
        private List<InspectionCategory> categories;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder templateCode(String templateCode) {
            this.templateCode = templateCode;
            return this;
        }

        public Builder templateName(String templateName) {
            this.templateName = templateName;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder scope(TemplateScope scope) {
            this.scope = scope;
            return this;
        }

        public Builder applicableOrgUnitId(Long applicableOrgUnitId) {
            this.applicableOrgUnitId = applicableOrgUnitId;
            return this;
        }

        public Builder isDefault(Boolean isDefault) {
            this.isDefault = isDefault;
            return this;
        }

        public Builder currentVersion(Integer currentVersion) {
            this.currentVersion = currentVersion;
            return this;
        }

        public Builder status(TemplateStatus status) {
            this.status = status;
            return this;
        }

        public Builder createdBy(Long createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public Builder categories(List<InspectionCategory> categories) {
            this.categories = categories;
            return this;
        }

        public InspectionTemplate build() {
            return new InspectionTemplate(this);
        }
    }
}
