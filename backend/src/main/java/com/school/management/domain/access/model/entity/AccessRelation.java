package com.school.management.domain.access.model.entity;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 统一访问关系实体 (Zanzibar Simplified)
 * 表达: subject 对 resource 拥有 relation 关系
 */
@Data
@Builder
public class AccessRelation {

    private Long id;

    /** 资源类型: place, org_unit, student, class... */
    private String resourceType;

    /** 资源ID */
    private Long resourceId;

    /** 关系: owner, manager, user, member, viewer, responsible, occupant */
    private String relation;

    /** 主体类型: org_unit, user */
    private String subjectType;

    /** 主体ID */
    private Long subjectId;

    /** 是否包含主体的子级组织 */
    @Builder.Default
    private boolean includeChildren = false;

    /** 1=只读, 2=读写 */
    @Builder.Default
    private int accessLevel = 1;

    /** 扩展字段 */
    private Map<String, Object> metadata;

    private String remark;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ---------- 便捷查询方法 ----------

    public boolean isOrgSubject() {
        return "org_unit".equals(subjectType);
    }

    public boolean isUserSubject() {
        return "user".equals(subjectType);
    }

    public boolean isReadWrite() {
        return accessLevel >= 2;
    }

    @SuppressWarnings("unchecked")
    public <T> T getMetaValue(String key) {
        if (metadata == null) return null;
        return (T) metadata.get(key);
    }

    public boolean getBooleanMeta(String key) {
        Object val = getMetaValue(key);
        if (val instanceof Boolean) return (Boolean) val;
        if (val instanceof Number) return ((Number) val).intValue() != 0;
        return false;
    }
}
