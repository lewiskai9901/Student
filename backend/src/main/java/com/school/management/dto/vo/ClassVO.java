package com.school.management.dto.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 班级视图对象
 * 用于API响应，包含班级基本信息和关联显示信息
 *
 * @author system
 * @version 2.0.0
 * @since 2024-12-31
 */
@Data
public class ClassVO {

    // ==================== 基本信息 ====================

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
     * 部门ID
     */
    private Long departmentId;

    /**
     * 班主任ID
     */
    private Long teacherId;

    /**
     * 副班主任ID
     */
    private Long assistantTeacherId;

    /**
     * 学生人数
     */
    private Integer studentCount;

    /**
     * 状态(1正常 0停用)
     */
    private Integer status;

    // ==================== 关联显示信息 ====================

    /**
     * 年级名称
     */
    private String gradeName;

    /**
     * 专业名称
     */
    private String majorName;

    /**
     * 专业方向名称
     */
    private String majorDirectionName;

    /**
     * 部门名称
     */
    private String departmentName;

    /**
     * 班主任姓名
     */
    private String teacherName;

    /**
     * 副班主任姓名
     */
    private String assistantTeacherName;

    // ==================== 时间信息 ====================

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
