package com.school.management.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 检查记录发布事件
 *
 * @author system
 * @since 3.2.0
 */
@Getter
public class CheckRecordPublishedEvent extends ApplicationEvent {

    /**
     * 检查记录ID
     */
    private final Long checkRecordId;

    /**
     * 检查计划ID
     */
    private final Long checkPlanId;

    public CheckRecordPublishedEvent(Object source, Long checkRecordId, Long checkPlanId) {
        super(source);
        this.checkRecordId = checkRecordId;
        this.checkPlanId = checkPlanId;
    }
}
