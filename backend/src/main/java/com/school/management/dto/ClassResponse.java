package com.school.management.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.Year;

/**
 * 班级响应DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
public class ClassResponse {

    /**
     * 班级ID
     */
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
     * 年级ID
     */
    private Long gradeId;

    /**
     * 所属部门ID
     */
    private Long departmentId;

    /**
     * 部门名称
     */
    private String departmentName;

    /**
     * 专业ID
     */
    private Long majorId;

    /**
     * 专业名称
     */
    private String majorName;

    /**
     * 专业方向ID
     */
    private Long majorDirectionId;

    /**
     * 专业方向名称
     */
    private String majorDirectionName;

    /**
     * 年级(格式化后的年级信息)
     */
    private String grade;

    /**
     * 班主任ID
     */
    private Long teacherId;

    /**
     * 班主任姓名
     */
    private String teacherName;

    /**
     * 副班主任ID
     */
    private Long assistantTeacherId;

    /**
     * 副班主任姓名
     */
    private String assistantTeacherName;

    /**
     * 学生数量
     */
    private Integer studentCount;

    /**
     * 最大学生数
     */
    private Integer maxStudents;

    /**
     * 教室位置
     */
    private String classroomLocation;

    /**
     * 入学年份
     */
    private Year enrollmentYear;

    /**
     * 毕业年份
     */
    private Year graduationYear;

    /**
     * 班级类型: 1普通班 2重点班 3实验班
     */
    private Integer classType;

    /**
     * 班级类型名称
     */
    private String classTypeName;

    /**
     * 状态: 1启用 0禁用
     */
    private Integer status;

    /**
     * 状态名称
     */
    private String statusName;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}