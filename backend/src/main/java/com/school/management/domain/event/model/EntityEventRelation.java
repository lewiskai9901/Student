package com.school.management.domain.event.model;

import lombok.Builder;
import lombok.Getter;

/**
 * 事件关联主体
 */
@Getter
@Builder
public class EntityEventRelation {

    private Long id;
    private Long eventId;
    private String relatedType;
    private Long relatedId;
    private String relatedName;
    private String relation;
}
