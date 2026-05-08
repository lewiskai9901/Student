package com.school.management.domain.access.model.entity;

import com.school.management.domain.access.model.valueobject.AccessLevel;
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

    /**
     * 是否包含主体的子级组织.
     *
     * @deprecated 已废弃 — 关系传递性由 {@code RelationTypeDef.isTransitive} +
     *   {@code impliedRelations} 声明 (类型级单一真相); BFS 不读此字段.
     *   详见 ADR-002. 字段保留作数据兼容, V26 大版本计划 DROP.
     */
    @Deprecated
    @Builder.Default
    private boolean includeChildren = false;

    /** 访问级别: READ_ONLY / FULL / OWNER */
    @Builder.Default
    private AccessLevel accessLevel = AccessLevel.FULL;

    /** 扩展字段 */
    private Map<String, Object> metadata;

    /** 关系开始生效时间. null 表示从创建时立即生效. */
    private LocalDateTime validFrom;

    /** 关系到期时间. null 表示永久有效(直到 revoke). */
    private LocalDateTime validTo;

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
        return accessLevel != null && accessLevel.isReadWrite();
    }

    /** 在指定时间点是否仍生效(未到期 + 已开始 + 未删). */
    public boolean isActiveAt(LocalDateTime at) {
        if (at == null) at = LocalDateTime.now();
        if (validFrom != null && at.isBefore(validFrom)) return false;
        if (validTo != null && !at.isBefore(validTo)) return false;
        return true;
    }

    /** 当前时刻是否生效. */
    public boolean isCurrentlyActive() {
        return isActiveAt(LocalDateTime.now());
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
