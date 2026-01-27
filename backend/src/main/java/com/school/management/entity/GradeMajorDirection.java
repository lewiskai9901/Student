package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学年开设专业方向实体类
 * 记录每个学年开设的专业方向
 */
@Data
@TableName("grade_major_directions")
public class GradeMajorDirection {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 学年(如:2024)
     */
    private Integer academicYear;

    /**
     * 专业方向ID
     */
    private Long majorDirectionId;

    /**
     * 备注
     */
    private String remarks;

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
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 更新人ID
     */
    private Long updatedBy;

    /**
     * 删除标志(0=未删除,1=已删除)
     */
    @TableLogic
    private Integer deleted;

    // ========== 非数据库字段 ==========

    /**
     * 方向名称(非数据库字段)
     */
    @TableField(exist = false)
    private String directionName;

    /**
     * 专业名称(非数据库字段)
     */
    @TableField(exist = false)
    private String majorName;

    /**
     * 层次(非数据库字段)
     */
    @TableField(exist = false)
    private String level;

    /**
     * 学制(非数据库字段)
     */
    @TableField(exist = false)
    private Integer years;

    /**
     * 是否分段注册(非数据库字段)
     */
    @TableField(exist = false)
    private Integer isSegmented;

    /**
     * 第一阶段层次(非数据库字段)
     */
    @TableField(exist = false)
    private String phase1Level;

    /**
     * 第一阶段年数(非数据库字段)
     */
    @TableField(exist = false)
    private Integer phase1Years;

    /**
     * 第二阶段层次(非数据库字段)
     */
    @TableField(exist = false)
    private String phase2Level;

    /**
     * 第二阶段年数(非数据库字段)
     */
    @TableField(exist = false)
    private Integer phase2Years;

    /**
     * 方向编码(非数据库字段)
     */
    @TableField(exist = false)
    private String directionCode;

    /**
     * 专业ID(非数据库字段)
     */
    @TableField(exist = false)
    private Long majorId;

    /**
     * 专业编码(非数据库字段)
     */
    @TableField(exist = false)
    private String majorCode;

    /**
     * 所属组织单元ID(非数据库字段,来自majors表的org_unit_id)
     */
    @TableField(exist = false)
    private Long orgUnitId;
}
