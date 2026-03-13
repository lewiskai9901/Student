package com.school.management.infrastructure.persistence.relation;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户-场所关系持久化对象
 */
@Data
@TableName("user_space_relations")
public class UserSpaceRelationPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 场所ID
     */
    private Long spaceId;

    /**
     * 关系类型
     */
    private String relationType;

    /**
     * 位置编码
     */
    private String positionCode;

    /**
     * 位置名称
     */
    private String positionName;

    /**
     * 是否主要场所
     */
    private Boolean isPrimary;

    /**
     * 是否有使用权
     */
    private Boolean canUse;

    /**
     * 是否有管理权
     */
    private Boolean canManage;

    /**
     * 开始日期
     */
    private LocalDate startDate;

    /**
     * 结束日期
     */
    private LocalDate endDate;

    /**
     * 费用金额
     */
    private BigDecimal feeAmount;

    /**
     * 是否已缴费
     */
    private Boolean feePaid;

    /**
     * 排序号
     */
    private Integer sortOrder;

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
