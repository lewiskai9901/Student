package com.school.management.infrastructure.persistence.event;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 事件关联主体 Mapper
 */
@Mapper
public interface EntityEventRelationMapper extends BaseMapper<EntityEventRelationPO> {

    @Select("SELECT * FROM entity_event_relations WHERE event_id = #{eventId}")
    List<EntityEventRelationPO> selectByEventId(@Param("eventId") Long eventId);
}
