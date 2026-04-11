package com.school.management.domain.access.repository;

import com.school.management.domain.access.model.entity.AccessRelation;

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
}
