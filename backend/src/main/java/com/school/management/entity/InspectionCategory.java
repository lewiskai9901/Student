package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 检查类别实体
 *
 * @author system
 * @since 2.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inspection_categories")
public class InspectionCategory extends BaseEntity {

    /**
     * 检查目标ID
     */
    private Long targetId;

    /**
     * 检查批次ID
     */
    private Long sessionId;

    /**
     * 量化类型ID
     */
    private Long typeId;

    /**
     * 类别名称
     */
    private String typeName;

    /**
     * 类别代码
     */
    private String typeCode;

    /**
     * 该类别总扣分
     */
    private BigDecimal categoryDeductions;

    /**
     * 扣分项数量
     */
    private Integer itemCount;

    /**
     * 证据图片
     */
    private String evidenceImages;

    /**
     * 备注
     */
    private String remarks;

    /**
     * 逻辑删除标志 - inspection_categories表中不存在此字段
     */
    @TableField(exist = false)
    private Integer deleted;
}
