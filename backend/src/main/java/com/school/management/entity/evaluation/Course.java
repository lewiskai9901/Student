package com.school.management.entity.evaluation;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 课程实体类
 *
 * @author Claude
 * @since 2025-11-28
 */
@Data
@TableName("courses")
public class Course implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 课程编码
     */
    private String courseCode;

    /**
     * 课程名称
     */
    private String courseName;

    /**
     * 课程类型: 1必修, 2选修, 3实践
     */
    private Integer courseType;

    /**
     * 学分
     */
    private BigDecimal credit;

    /**
     * 总学时
     */
    private Integer totalHours;

    /**
     * 开课组织单元
     */
    private Long orgUnitId;

    /**
     * 课程描述
     */
    private String description;

    /**
     * 状态: 1启用, 0禁用
     */
    private Integer status;

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

    // ==================== 课程类型常量 ====================

    public static final int TYPE_REQUIRED = 1;    // 必修
    public static final int TYPE_ELECTIVE = 2;    // 选修
    public static final int TYPE_PRACTICE = 3;    // 实践
}
