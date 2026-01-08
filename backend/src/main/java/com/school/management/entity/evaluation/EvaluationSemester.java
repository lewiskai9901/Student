package com.school.management.entity.evaluation;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学期实体类（综测模块）
 *
 * @author Claude
 * @since 2025-11-28
 */
@Data
@TableName("semesters")
public class EvaluationSemester implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 学期编码: 2024-2025-1
     */
    private String semesterCode;

    /**
     * 学期名称
     */
    private String semesterName;

    /**
     * 学年: 2024-2025
     */
    private String academicYear;

    /**
     * 学期类型: 1第一学期, 2第二学期
     */
    private Integer semesterType;

    /**
     * 开始日期
     */
    private LocalDate startDate;

    /**
     * 结束日期
     */
    private LocalDate endDate;

    /**
     * 是否当前学期
     */
    private Integer isCurrent;

    /**
     * 状态: 1启用, 0禁用
     */
    private Integer status;

    /**
     * 描述
     */
    private String description;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer deleted;

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
}
