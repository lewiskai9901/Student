package com.school.management.domain.access.repository;

import com.school.management.domain.access.model.entity.AccessRelation;
import com.school.management.domain.access.model.valueobject.AccessLevel;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 统一访问关系仓储接口
 */
public interface AccessRelationRepository {

    Optional<AccessRelation> findById(Long id);

    List<AccessRelation> findByResource(String resourceType, Long resourceId);

    List<AccessRelation> findByResourceAndRelation(String resourceType, Long resourceId, String relation);

    List<AccessRelation> findBySubject(String subjectType, Long subjectId);

    List<AccessRelation> findBySubjectAndResourceType(String subjectType, Long subjectId, String resourceType);

    /**
     * 核心查询：检查 subject 是否对 resource 有某种关系
     */
    boolean exists(String resourceType, Long resourceId, String relation, String subjectType, Long subjectId);

    /**
     * 查询某 subject（及其子级组织）能访问的所有 resource IDs
     */
    List<Long> findAccessibleResourceIds(String resourceType, String subjectType, Long subjectId);

    /**
     * 查询某 subject（及其子级组织）+ include_children 能访问的所有 resource IDs
     * @param orgUnitIds 用户所属组织及下级组织ID列表
     */
    List<Long> findAccessibleResourceIdsByOrgUnits(String resourceType, List<Long> orgUnitIds);

    AccessRelation save(AccessRelation relation);

    void update(AccessRelation relation);

    void deleteById(Long id);

    void deleteByResource(String resourceType, Long resourceId);

    /**
     * 删除指定资源与指定主体之间的所有关系记录
     */
    void deleteByResourceAndSubject(String resourceType, Long resourceId, String subjectType, Long subjectId);

    /**
     * 删除指定主体的所有关系记录
     */
    void deleteBySubject(String subjectType, Long subjectId);

    int batchSave(List<AccessRelation> relations);

    int batchDeleteByIds(List<Long> ids);

    /**
     * 列出关系(按筛选),用于管理 UI 分页浏览
     * 任意字段为 null 表示不过滤该条件。
     */
    List<AccessRelation> listFiltered(String resourceType, String subjectType, String relation);

    // ═══════════════════════════════════════════════════════════════
    // Zanzibar 6 API 底层查询 (W1.3 — 从 AuthorizationService 收回的 17 条 SQL)
    // 这些方法直接走 JdbcTemplate, 不经 MyBatis Plus 实体映射, 因为:
    //   - 含 NOW() 时间窗谓词
    //   - 含 INSERT … SELECT 跨表归档
    //   - 返回轻量投影 (id list / triple), 不是完整聚合根
    // ═══════════════════════════════════════════════════════════════

    /**
     * 检查直接关系是否存在(不展开 implied 推导).
     * 仅返回在 [validFrom, validTo) 时间窗内、未删的记录.
     */
    boolean checkDirectActive(String subjectType, Long subjectId,
                               String relation,
                               String resourceType, Long resourceId,
                               LocalDateTime at);

    /**
     * 列出某 subject 在指定时间点所有活跃的直接关系(用于 implied BFS 起点).
     * 返回的 record 含 resource_type/resource_id/relation 三元组.
     */
    List<DirectRelationRef> findActiveDirectRelationsBySubject(
            String subjectType, Long subjectId, LocalDateTime at);

    /**
     * 资源 + 关系 → 所有活跃 subject (含 access_level 和有效期).
     */
    List<SubjectRefPO> expandSubjects(String resourceType, Long resourceId, String relation);

    /**
     * 资源 + 关系 → 所有活跃 subject id (轻量,只回 id).
     * @param subjectTypeFilter null 不过滤,否则只返指定类型的
     */
    List<Long> findActiveSubjectIds(String resourceType, Long resourceId,
                                     String relation, String subjectTypeFilter);

    /**
     * Subject + 关系 + 资源类型 → 所有活跃资源(用于 lookup).
     * 含 access_level / 有效期.
     */
    List<RelationEdgeRef> lookupActiveResources(String subjectType, Long subjectId,
                                                  String relation, String resourceType);

    /**
     * 某资源上所有活跃关系 (lookup expandAll 配套, 不限 relation).
     */
    List<RelationEdgeRef> expandAllOnResource(String resourceType, Long resourceId);

    /**
     * 某 subject 所有活跃关系 (lookupAll 配套, 不限 resource_type).
     */
    List<RelationEdgeRef> lookupAllForSubject(String subjectType, Long subjectId);

    /**
     * 校验 (relation_code, from_type, to_type) 是否在 relation_types 已注册且启用.
     */
    boolean isRelationRegistered(String relation, String fromType, String toType);

    /**
     * 幂等检查: 同样 5 元组的活跃关系是否已存在,返回 id.
     */
    Optional<Long> findActiveByTuple(String resourceType, Long resourceId,
                                      String relation,
                                      String subjectType, Long subjectId);

    /**
     * 直接 INSERT,返回 id.
     * 用于 grant flow,绕过领域聚合(因为 grant 走的不是 entity-driven 流).
     */
    Long insertDirect(InsertDirectCommand cmd);

    /**
     * 查找所有匹配 5 元组的活跃关系 id (revoke 时找到要软删的目标).
     */
    List<Long> findActiveIdsByTuple(String resourceType, Long resourceId,
                                     String relation,
                                     String subjectType, Long subjectId);

    /**
     * 把指定关系归档到 history 表 + 软删原表.
     * 必须在事务内调用 (调用方加 @Transactional).
     *
     * <p>Phase 7 W7.3: 旧签名 default 委托到带 IP/UA 的新签名, 兼容既有调用方.
     */
    default void archiveAndSoftDelete(Long id, String reason, Long actorId) {
        archiveAndSoftDelete(id, reason, actorId, null, null);
    }

    /**
     * 同上 + HTTP context (operator_ip / user_agent), 写 history.operator_*.
     * Phase 7 W7.3 新增.
     */
    void archiveAndSoftDelete(Long id, String reason, Long actorId,
                               String operatorIp, String userAgent);

    // ═══════════════════════════════════════════════════════════════
    // Zanzibar 查询投影 records
    // ═══════════════════════════════════════════════════════════════

    /** Subject 的活跃直接关系三元组 (resourceType + resourceId + relation). */
    record DirectRelationRef(String resourceType, Long resourceId, String relation) {}

    /** Subject 投影 (用于 expand). */
    record SubjectRefPO(String subjectType, Long subjectId, AccessLevel accessLevel,
                         LocalDateTime validFrom, LocalDateTime validTo) {}

    /** 关系边 (lookup 用 — subject 与 resource 双侧 + relation + access_level + 有效期). */
    record RelationEdgeRef(String relation,
                            String subjectType, Long subjectId,
                            String resourceType, Long resourceId,
                            AccessLevel accessLevel,
                            LocalDateTime validFrom, LocalDateTime validTo) {}

    /** Grant 直插 INSERT 命令对象 (字段太多, 用 record 而非长签名). */
    record InsertDirectCommand(String resourceType, Long resourceId, String relation,
                                String subjectType, Long subjectId,
                                boolean includeChildren,
                                AccessLevel accessLevel,
                                LocalDateTime validFrom, LocalDateTime validTo,
                                String metadataJson, String remark,
                                Long tenantId, Long createdBy) {}
}
