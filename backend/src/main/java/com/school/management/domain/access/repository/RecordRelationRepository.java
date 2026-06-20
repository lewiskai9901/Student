package com.school.management.domain.access.repository;

import com.school.management.domain.access.model.entity.RecordRelation;

import java.util.List;

/**
 * 记录↔主体关系仓储 (统一锚定 R4 地基)。
 *
 * <p>{@code record_relations} 表的读写口。与 {@code AccessRelationRepository} (纯主体图) 分立。
 * 引擎将来出 {@code RECORD_RELATION} 子条件 ({@code id IN (SELECT record_id FROM record_relations
 * WHERE ...)}) 的 Java 侧对应即 {@link #findRecordIdsBySubject}。
 */
public interface RecordRelationRepository {

    /**
     * upsert 一条记录关系 (按唯一键 resource+record+relation+subject+tenant; 撞键则重激活+更新)。
     * @return 该行 id
     */
    Long save(RecordRelation relation);

    /**
     * 某主体经某关系可见的记录 id 集 (有效期内、未软删)。
     * 引擎 RECORD_RELATION 子查询的 Java 侧对应。
     */
    List<Long> findRecordIdsBySubject(String resourceCode, String relationCode,
                                      String subjectType, Long subjectId, Long tenantId);

    /** 某记录在某关系下的所有主体 (写路径/审计用)。 */
    List<RecordRelation> findByRecordAndRelation(String resourceCode, Long recordId,
                                                 String relationCode, Long tenantId);

    /** 软删一条 (resource+record+relation+subject)。返回受影响行数。 */
    int softDelete(String resourceCode, Long recordId, String relationCode,
                   String subjectType, Long subjectId, Long tenantId);
}
