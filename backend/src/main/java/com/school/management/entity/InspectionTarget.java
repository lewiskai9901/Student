package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 检查目标实体
 *
 * @author system
 * @since 2.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inspection_targets")
public class InspectionTarget extends BaseEntity {

    /**
     * 检查批次ID
     */
    private Long sessionId;

    /**
     * 目标类型:class-班级,dorm-宿舍
     */
    private String targetType;

    /**
     * 目标ID(班级ID或宿舍ID)
     */
    private Long targetId;

    /**
     * 目标名称
     */
    private String targetName;

    /**
     * 该目标总扣分
     */
    private BigDecimal totalDeductions;

    /**
     * 检查类别数
     */
    private Integer categoryCount;

    /**
     * 扣分项数量
     */
    private Integer itemCount;

    /**
     * 备注
     */
    private String remarks;

    /**
     * 逻辑删除标志 - inspection_targets表中不存在此字段
     */
    @TableField(exist = false)
    private Integer deleted;
}
