package com.school.management.infrastructure.persistence.space;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 空间占用记录持久化对象
 */
@Data
@TableName("space_occupants")
public class UniversalSpaceOccupantPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 空间ID
     */
    private Long spaceId;

    /**
     * 占用者类型
     */
    private String occupantType;

    /**
     * 占用者ID
     */
    private Long occupantId;

    /**
     * 占用者名称
     */
    private String occupantName;

    /**
     * 位置号
     */
    private String positionNo;

    /**
     * 入住时间
     */
    private LocalDateTime checkInTime;

    /**
     * 退出时间
     */
    private LocalDateTime checkOutTime;

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
    private String spaceName;
}
