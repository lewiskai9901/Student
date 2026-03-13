package com.school.management.domain.inspection.model.v7.execution;

import com.school.management.domain.shared.Entity;

import java.time.LocalDateTime;

/**
 * V7 检查员池
 */
public class ProjectInspector implements Entity<Long> {

    private Long id;
    private Long tenantId;
    private Long projectId;
    private Long userId;
    private String userName;
    private InspectorRole role;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected ProjectInspector() {
    }

    private ProjectInspector(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId;
        this.projectId = builder.projectId;
        this.userId = builder.userId;
        this.userName = builder.userName;
        this.role = builder.role != null ? builder.role : InspectorRole.INSPECTOR;
        this.isActive = builder.isActive != null ? builder.isActive : true;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedAt = builder.updatedAt;
    }

    public static ProjectInspector create(Long projectId, Long userId,
                                          String userName, InspectorRole role) {
        return builder()
                .projectId(projectId)
                .userId(userId)
                .userName(userName)
                .role(role)
                .build();
    }

    public static ProjectInspector reconstruct(Builder builder) {
        return new ProjectInspector(builder);
    }

    public void activate() {
        this.isActive = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.isActive = false;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    @Override
    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public Long getTenantId() { return tenantId; }
    public Long getProjectId() { return projectId; }
    public Long getUserId() { return userId; }
    public String getUserName() { return userName; }
    public InspectorRole getRole() { return role; }
    public Boolean getIsActive() { return isActive; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long tenantId;
        private Long projectId;
        private Long userId;
        private String userName;
        private InspectorRole role;
        private Boolean isActive;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(Long tenantId) { this.tenantId = tenantId; return this; }
        public Builder projectId(Long projectId) { this.projectId = projectId; return this; }
        public Builder userId(Long userId) { this.userId = userId; return this; }
        public Builder userName(String userName) { this.userName = userName; return this; }
        public Builder role(InspectorRole role) { this.role = role; return this; }
        public Builder isActive(Boolean isActive) { this.isActive = isActive; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public ProjectInspector build() { return new ProjectInspector(this); }
    }
}
