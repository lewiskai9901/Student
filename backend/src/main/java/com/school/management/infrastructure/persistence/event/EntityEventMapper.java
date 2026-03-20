package com.school.management.infrastructure.persistence.event;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 实体事件 Mapper
 */
@Mapper
public interface EntityEventMapper extends BaseMapper<EntityEventPO> {

    @Select("SELECT * FROM entity_events WHERE subject_type = #{subjectType} AND subject_id = #{subjectId} " +
            "AND deleted = 0 ORDER BY occurred_at DESC LIMIT #{limit}")
    List<EntityEventPO> selectBySubject(@Param("subjectType") String subjectType,
                                         @Param("subjectId") Long subjectId,
                                         @Param("limit") int limit);

    @Select("SELECT e.* FROM entity_events e " +
            "INNER JOIN entity_event_relations r ON r.event_id = e.id " +
            "WHERE r.related_type = #{relatedType} AND r.related_id = #{relatedId} " +
            "AND e.deleted = 0 ORDER BY e.occurred_at DESC LIMIT #{limit}")
    List<EntityEventPO> selectByRelated(@Param("relatedType") String relatedType,
                                         @Param("relatedId") Long relatedId,
                                         @Param("limit") int limit);

    @Select("SELECT event_type, event_category, COUNT(*) AS cnt " +
            "FROM entity_events WHERE subject_type = #{subjectType} AND subject_id = #{subjectId} " +
            "AND deleted = 0 GROUP BY event_category, event_type")
    List<Map<String, Object>> countGroupByType(@Param("subjectType") String subjectType,
                                                @Param("subjectId") Long subjectId);
}
