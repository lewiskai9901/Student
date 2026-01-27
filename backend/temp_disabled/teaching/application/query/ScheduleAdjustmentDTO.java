package com.school.management.application.teaching.query;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 调课记录查询DTO
 */
@Data
@Builder
public class ScheduleAdjustmentDTO {

    private Long id;

    private Long entryId;

    /**
     * 调整类型: 1-调课 2-停课 3-补课 4-代课
     */
    private Integer adjustType;

    private String adjustTypeName;

    /**
     * 课程信息
     */
    private Long courseId;

    private String courseName;

    /**
     * 班级信息
     */
    private Long classId;

    private String className;

    /**
     * 原教师
     */
    private String originalTeacherName;

    /**
     * 原日期
     */
    private LocalDate originalDate;

    private Integer originalWeekday;

    private String originalWeekdayName;

    /**
     * 原节次
     */
    private Integer originalSlot;

    /**
     * 原教室
     */
    private String originalClassroomName;

    /**
     * 新日期
     */
    private LocalDate newDate;

    private Integer newWeekday;

    private String newWeekdayName;

    /**
     * 新节次
     */
    private Integer newSlot;

    /**
     * 新教室
     */
    private Long newClassroomId;

    private String newClassroomName;

    /**
     * 代课教师
     */
    private Long substituteTeacherId;

    private String substituteTeacherName;

    /**
     * 调课原因
     */
    private String reason;

    /**
     * 状态: 0-待审批 1-已批准 2-已拒绝 3-已取消
     */
    private Integer status;

    private String statusName;

    /**
     * 申请人
     */
    private Long applicantId;

    private String applicantName;

    /**
     * 审批人
     */
    private Long approverId;

    private String approverName;

    private LocalDateTime approvedAt;

    private String approvalRemark;

    private LocalDateTime createdAt;

    /**
     * 获取调整类型名称
     */
    public static String getAdjustTypeName(Integer type) {
        if (type == null) return "";
        return switch (type) {
            case 1 -> "调课";
            case 2 -> "停课";
            case 3 -> "补课";
            case 4 -> "代课";
            default -> "";
        };
    }

    /**
     * 获取状态名称
     */
    public static String getStatusName(Integer status) {
        if (status == null) return "";
        return switch (status) {
            case 0 -> "待审批";
            case 1 -> "已批准";
            case 2 -> "已拒绝";
            case 3 -> "已取消";
            default -> "";
        };
    }
}
