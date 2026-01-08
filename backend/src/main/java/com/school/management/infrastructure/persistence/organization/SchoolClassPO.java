package com.school.management.infrastructure.persistence.organization;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Persistence object for classes.
 * Maps to the existing 'classes' table.
 */
@Data
@TableName("classes")
public class SchoolClassPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 班级名称
     */
    private String className;

    /**
     * 班级编码
     */
    private String classCode;

    /**
     * 年级
     */
    private Integer gradeLevel;

    /**
     * 所属部门ID
     */
    private Long departmentId;

    /**
     * 年级ID
     */
    private Long gradeId;

    /**
     * 专业ID
     */
    private Long majorId;

    /**
     * 专业方向ID
     */
    private Long majorDirectionId;

    /**
     * 班主任ID
     */
    private Long teacherId;

    /**
     * 副班主任ID
     */
    private Long assistantTeacherId;

    /**
     * 学生数量
     */
    private Integer studentCount;

    /**
     * 教室位置
     */
    private String classroomLocation;

    /**
     * 入学年份
     */
    private Integer enrollmentYear;

    /**
     * 毕业年份
     */
    private Integer graduationYear;

    /**
     * 班级类型: 1普通班 2重点班 3实验班
     */
    private Integer classType;

    /**
     * 状态: 1启用 0禁用
     */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
