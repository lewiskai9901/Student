package com.school.management.infrastructure.persistence.event;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 事件关联主体持久化对象
 */
@Data
@TableName("entity_event_relations")
public class EntityEventRelationPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long eventId;
    private String relatedType;
    private Long relatedId;
    private String relatedName;
    private String relation;
}
