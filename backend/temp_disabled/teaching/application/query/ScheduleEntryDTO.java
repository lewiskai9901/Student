package com.school.management.application.teaching.query;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 课表条目查询DTO
 */
@Data
@Builder
public class ScheduleEntryDTO {

    private Long id;

    private Long scheduleId;

    private Long taskId;

    private Long semesterId;

    private String semesterName;

    /**
     * 课程信息
     */
    private Long courseId;

    private String courseCode;

    private String courseName;

    /**
     * 班级信息
     */
    private Long classId;

    private String className;

    /**
     * 教师信息
     */
    private String teacherNames;

    /**
     * 教室信息
     */
    private Long classroomId;

    private String classroomName;

    private String buildingName;

    /**
     * 星期: 1-7
     */
    private Integer weekday;

    private String weekdayName;

    /**
     * 节次
     */
    private Integer slot;

    private String slotName;

    /**
     * 起始周
     */
    private Integer startWeek;

    /**
     * 结束周
     */
    private Integer endWeek;

    /**
     * 周类型: 0-每周 1-单周 2-双周
     */
    private Integer weekType;

    private String weekTypeName;

    /**
     * 周范围描述
     */
    private String weekRangeDesc;

    private LocalDateTime createdAt;

    /**
     * 获取星期名称
     */
    public static String getWeekdayName(Integer weekday) {
        if (weekday == null) return "";
        return switch (weekday) {
            case 1 -> "周一";
            case 2 -> "周二";
            case 3 -> "周三";
            case 4 -> "周四";
            case 5 -> "周五";
            case 6 -> "周六";
            case 7 -> "周日";
            default -> "";
        };
    }

    /**
     * 获取周类型名称
     */
    public static String getWeekTypeName(Integer weekType) {
        if (weekType == null) return "每周";
        return switch (weekType) {
            case 0 -> "每周";
            case 1 -> "单周";
            case 2 -> "双周";
            default -> "";
        };
    }

    /**
     * 获取周范围描述
     */
    public static String getWeekRangeDesc(Integer startWeek, Integer endWeek, Integer weekType) {
        if (startWeek == null || endWeek == null) return "";
        String range = startWeek + "-" + endWeek + "周";
        if (weekType != null && weekType > 0) {
            range += "(" + getWeekTypeName(weekType) + ")";
        }
        return range;
    }
}
