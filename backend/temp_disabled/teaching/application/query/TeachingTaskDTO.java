package com.school.management.application.teaching.query;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 教学任务查询DTO
 */
@Data
@Builder
public class TeachingTaskDTO {

    private Long id;

    private Long semesterId;

    private String semesterName;

    private Long courseId;

    private String courseCode;

    private String courseName;

    private Long classId;

    private String className;

    private Long classroomId;

    private String classroomName;

    private Integer weeklyHours;

    private Integer startWeek;

    private Integer endWeek;

    private String examType;

    private String remark;

    /**
     * 状态: 0-待分配 1-已分配 2-进行中 3-已完成
     */
    private Integer status;

    private String statusName;

    private Long createdBy;

    private LocalDateTime createdAt;

    private Long updatedBy;

    private LocalDateTime updatedAt;

    /**
     * 教师列表
     */
    private List<TaskTeacherDTO> teachers;

    /**
     * 获取状态名称
     */
    public static String getStatusName(Integer status) {
        if (status == null) return "";
        return switch (status) {
            case 0 -> "待分配";
            case 1 -> "已分配";
            case 2 -> "进行中";
            case 3 -> "已完成";
            default -> "";
        };
    }
}
