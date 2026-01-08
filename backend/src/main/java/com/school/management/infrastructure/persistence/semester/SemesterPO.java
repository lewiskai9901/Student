package com.school.management.infrastructure.persistence.semester;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学期持久化对象
 */
@Data
@TableName("semesters")
public class SemesterPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 学期名称
     */
    private String semesterName;

    /**
     * 学期编码 (如: 2024-2025-2)
     */
    private String semesterCode;

    /**
     * 学期开始日期
     */
    private LocalDate startDate;

    /**
     * 学期结束日期
     */
    private LocalDate endDate;

    /**
     * 开始年份 (如: 2024)
     */
    private Integer startYear;

    /**
     * 学期类型: 1第一学期 2第二学期
     */
    private Integer semesterType;

    /**
     * 是否当前学期: 1是 0否
     */
    private Integer isCurrent;

    /**
     * 状态: 1正常 0已结束
     */
    private Integer status;

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
     * 创建人
     */
    private Long createdBy;

    /**
     * 更新人
     */
    private Long updatedBy;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer deleted;
}
