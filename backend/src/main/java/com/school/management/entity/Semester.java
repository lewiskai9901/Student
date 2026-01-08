package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学期实体类
 *
 * @author system
 * @since 2.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("semesters")
public class Semester extends BaseEntity {

    /**
     * 学期名称
     */
    private String semesterName;

    /**
     * 学期编码(如:2024-2025-2)
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
     * 开始年份(如:2024)
     */
    @TableField(exist = false)
    private Integer startYear;

    /**
     * 学期类型:1-第一学期 2-第二学期
     */
    @TableField(exist = false)
    private Integer semesterType;

    /**
     * 是否当前学期:1-是 0-否
     */
    private Integer isCurrent;

    /**
     * 状态:1-正常 0-已结束
     */
    private Integer status;
}
