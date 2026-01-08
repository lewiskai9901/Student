package com.school.management.dto.quickentry;

import lombok.Data;

/**
 * 快捷录入 - 学生搜索结果DTO
 *
 * @author system
 * @since 1.0.7
 */
@Data
public class QuickEntryStudentDTO {

    /**
     * 学生ID (students表的id)
     */
    private Long id;

    /**
     * 用户ID (关联users表)
     */
    private Long userId;

    /**
     * 学号
     */
    private String studentNo;

    /**
     * 姓名
     */
    private String realName;

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 班级名称
     */
    private String className;

    /**
     * 专业名称
     */
    private String majorName;

    /**
     * 院系名称
     */
    private String departmentName;

    /**
     * 头像URL
     */
    private String avatar;
}
