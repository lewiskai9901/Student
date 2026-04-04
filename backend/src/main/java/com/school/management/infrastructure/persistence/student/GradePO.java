package com.school.management.infrastructure.persistence.student;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Persistence object for grades.
 * Maps to the existing 'grades' table.
 */
@Data
@TableName("grades")
public class GradePO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 年级名称
     */
    private String gradeName;

    /**
     * 年级编码
     */
    private String gradeCode;

    /**
     * 入学年份
     */
    private Integer enrollmentYear;

    /**
     * 预计毕业年份
     */
    private Integer graduationYear;

    /**
     * 年级主任ID
     */
    private Long gradeDirectorId;

    /**
     * 年级辅导员ID
     */
    private Long gradeCounselorId;

    /**
     * 班级总数
     */
    private Integer totalClasses;

    /**
     * 学生总数
     */
    private Integer totalStudents;

    /**
     * 标准班级人数
     */
    private Integer standardClassSize;

    /**
     * 状态 (0=招生中, 1=在读, 2=已毕业, 3=停招)
     */
    private Integer status;

    /**
     * 排序号
     */
    private Integer sortOrder;

    /**
     * 备注
     */
    private String remarks;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private Long createdBy;

    private Long updatedBy;

    @TableLogic
    private Integer deleted;
}
