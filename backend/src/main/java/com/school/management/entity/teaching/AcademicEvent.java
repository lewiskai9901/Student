package com.school.management.entity.teaching;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 校历事件实体
 */
@Data
@TableName("academic_event")
public class AcademicEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 学年ID
     */
    private Long yearId;

    /**
     * 学期ID（可选）
     */
    private Long semesterId;

    /**
     * 事件名称
     */
    private String eventName;

    /**
     * 事件类型：1-开学, 2-放假, 3-考试, 4-活动, 5-其他
     */
    private Integer eventType;

    /**
     * 开始日期
     */
    private LocalDate startDate;

    /**
     * 结束日期
     */
    private LocalDate endDate;

    /**
     * 是否全天事件
     */
    private Boolean allDay;

    /**
     * 事件描述
     */
    private String description;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer deleted;
}
