package com.school.management.infrastructure.persistence.event;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 事件类型 Mapper
 */
@Mapper
public interface EntityEventTypeMapper extends BaseMapper<EntityEventTypePO> {

    @Select("SELECT * FROM entity_event_types WHERE category_code = #{categoryCode} AND deleted = 0 ORDER BY sort_order")
    List<EntityEventTypePO> selectByCategory(@Param("categoryCode") String categoryCode);

    @Select("SELECT * FROM entity_event_types WHERE is_enabled = 1 AND deleted = 0 ORDER BY category_code, sort_order")
    List<EntityEventTypePO> selectEnabled();
}
