package com.school.management.infrastructure.persistence.place;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 空间预订记录持久化对象
 */
@Data
@TableName("place_bookings")
public class UniversalPlaceBookingPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 空间ID
     */
    private Long placeId;

    /**
     * 预订人ID
     */
    private Long bookerId;

    /**
     * 预订人名称
     */
    private String bookerName;

    /**
     * 预订标题
     */
    private String title;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 参与人（JSON数组）
     */
    private String attendees;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建人
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新人
     */
    private Long updatedBy;

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

    // ==================== 非数据库字段（关联查询用） ====================

    /**
     * 空间名称
     */
    @TableField(exist = false)
    private String placeName;

    /**
     * 空间类型
     */
    @TableField(exist = false)
    private String placeTypeCode;
}
