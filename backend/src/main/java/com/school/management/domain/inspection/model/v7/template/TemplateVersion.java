package com.school.management.domain.inspection.model.v7.template;

import com.school.management.domain.shared.Entity;

import java.time.LocalDateTime;

/**
 * V7 模板版本快照（不可变实体）
 * 发布时创建，记录完整的模板结构和评分配置快照
 */
public class TemplateVersion implements Entity<Long> {

    private Long id;
    private Long tenantId;
    private Long templateId;
    private Integer version;
    private String structureSnapshot;        // JSON: 完整 sections+items 树
    private String scoringProfileSnapshot;   // JSON: 评分配置快照
    private Long createdBy;
    private LocalDateTime createdAt;

    protected TemplateVersion() {
    }

    private TemplateVersion(Long templateId, Integer version,
                            String structureSnapshot, String scoringProfileSnapshot,
                            Long createdBy) {
        this.templateId = templateId;
        this.version = version;
        this.structureSnapshot = structureSnapshot;
        this.scoringProfileSnapshot = scoringProfileSnapshot;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
    }

    public static TemplateVersion create(Long templateId, Integer version,
                                         String structureSnapshot, String scoringProfileSnapshot,
                                         Long createdBy) {
        return new TemplateVersion(templateId, version, structureSnapshot, scoringProfileSnapshot, createdBy);
    }

    /**
     * 从持久化数据重建
     */
    public static TemplateVersion reconstruct(Long id, Long templateId, Integer version,
                                              String structureSnapshot, String scoringProfileSnapshot,
                                              Long createdBy, LocalDateTime createdAt) {
        TemplateVersion tv = new TemplateVersion();
        tv.id = id;
        tv.templateId = templateId;
        tv.version = version;
        tv.structureSnapshot = structureSnapshot;
        tv.scoringProfileSnapshot = scoringProfileSnapshot;
        tv.createdBy = createdBy;
        tv.createdAt = createdAt;
        return tv;
    }

    // Getters only — immutable after creation
    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public Integer getVersion() {
        return version;
    }

    public String getStructureSnapshot() {
        return structureSnapshot;
    }

    public String getScoringProfileSnapshot() {
        return scoringProfileSnapshot;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }
}
