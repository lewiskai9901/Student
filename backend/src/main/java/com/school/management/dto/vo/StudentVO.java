package com.school.management.dto.vo;

import com.school.management.dto.student.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生视图对象
 * 用于API响应，采用组合模式组织学生各类信息
 *
 * @author system
 * @version 2.0.0
 * @since 2024-12-31
 */
@Data
public class StudentVO {

    /**
     * 学生ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 基本信息
     */
    private StudentBasicInfoDTO basicInfo;

    /**
     * 联系信息
     */
    private StudentContactInfoDTO contactInfo;

    /**
     * 家庭信息
     */
    private StudentFamilyInfoDTO familyInfo;

    /**
     * 学业信息
     */
    private StudentAcademicInfoDTO academicInfo;

    /**
     * 宿舍信息
     */
    private StudentDormitoryInfoDTO dormitoryInfo;

    /**
     * 补充信息
     */
    private StudentSupplementaryInfoDTO supplementaryInfo;

    // ==================== 关联显示信息 ====================

    /**
     * 班级名称
     */
    private String className;

    /**
     * 年级名称
     */
    private String gradeName;

    /**
     * 专业名称
     */
    private String majorName;

    /**
     * 宿舍楼号
     */
    private String buildingNo;

    /**
     * 宿舍房间号
     */
    private String roomNo;

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
