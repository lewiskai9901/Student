package com.school.management.infrastructure.persistence.event;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.infrastructure.access.DataPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 实体事件 Mapper
 *
 * 数据权限说明：实体事件无 org_unit_id 字段，数据权限基于 created_by 过滤。
 * 超管和具有全局权限的角色可查看所有事件；普通用户只能查看自己创建的事件。
 * 后续如需按组织过滤，应在 entity_events 表中添加 org_unit_id 字段并更新注解。
 */
@Mapper
public interface EntityEventMapper extends BaseMapper<EntityEventPO> {

    /**
     * 按主体查询事件时间线 — 应用数据权限过滤（基于 created_by）
     * TODO: entity_events 表暂无 org_unit_id，过滤效果有限；后续添加该字段后更新注解
     */
    @DataPermission(module = "inspection_record", orgUnitField = "created_by", creatorField = "created_by")
    @Select("SELECT * FROM entity_events WHERE subject_type = #{subjectType} AND subject_id = #{subjectId} " +
            "AND deleted = 0 ORDER BY occurred_at DESC LIMIT #{limit}")
    List<EntityEventPO> selectBySubject(@Param("subjectType") String subjectType,
                                         @Param("subjectId") Long subjectId,
                                         @Param("limit") int limit);

    /**
     * 按关联主体查询事件时间线 — 应用数据权限过滤
     */
    @DataPermission(module = "inspection_record", orgUnitField = "created_by", creatorField = "created_by")
    @Select("SELECT e.* FROM entity_events e " +
            "INNER JOIN entity_event_relations r ON r.event_id = e.id " +
            "WHERE r.related_type = #{relatedType} AND r.related_id = #{relatedId} " +
            "AND e.deleted = 0 ORDER BY e.occurred_at DESC LIMIT #{limit}")
    List<EntityEventPO> selectByRelated(@Param("relatedType") String relatedType,
                                         @Param("relatedId") Long relatedId,
                                         @Param("limit") int limit);

    // 统计方法不加数据权限 — 统计本身已经按 subject 过滤，权限由调用方保证
    @Select("SELECT event_type, event_category, COUNT(*) AS cnt " +
            "FROM entity_events WHERE subject_type = #{subjectType} AND subject_id = #{subjectId} " +
            "AND deleted = 0 GROUP BY event_category, event_type")
    List<Map<String, Object>> countGroupByType(@Param("subjectType") String subjectType,
                                                @Param("subjectId") Long subjectId);
}
