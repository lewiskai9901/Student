package com.school.management.domain.event.repository;

import com.school.management.domain.event.model.EntityEvent;

import java.util.List;
import java.util.Map;

/**
 * 事件仓储接口
 */
public interface EntityEventRepository {

    EntityEvent save(EntityEvent event);

    /**
     * 按主体查询时间线
     */
    List<EntityEvent> findBySubject(String subjectType, Long subjectId, int limit);

    /**
     * 按关联主体查询时间线
     */
    List<EntityEvent> findByRelated(String relatedType, Long relatedId, int limit);

    /**
     * 统计主体各类事件数量
     */
    List<Map<String, Object>> countBySubjectGroupByType(String subjectType, Long subjectId);
}
