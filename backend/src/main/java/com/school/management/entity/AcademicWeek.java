package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 教学周实体类
 *
 * @author system
 * @since 2.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("academic_weeks")
public class AcademicWeek extends BaseEntity {

    /**
     * 学期ID
     */
    private Long semesterId;

    /**
     * 学期名称(冗余)
     */
    private String semesterName;

    /**
     * 周次(第几周)
     */
    private Integer weekNumber;

    /**
     * 周名称(如:第1周)
     */
    private String weekName;

    /**
     * 开始日期(周一)
     */
    private LocalDate startDate;

    /**
     * 结束日期(周日)
     */
    private LocalDate endDate;

    /**
     * 是否当前周:1-是 0-否
     */
    private Integer isCurrent;

    /**
     * 状态:1-正常 0-停用
     */
    private Integer status;
}
