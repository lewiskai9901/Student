package com.school.management.application.teaching.query;

import lombok.Builder;
import lombok.Data;

/**
 * 排课冲突DTO
 */
@Data
@Builder
public class ScheduleConflictDTO {

    /**
     * 冲突类型: TEACHER-教师冲突 CLASS-班级冲突 CLASSROOM-教室冲突
     */
    private String conflictType;

    private String conflictTypeName;

    /**
     * 冲突资源ID
     */
    private Long resourceId;

    /**
     * 冲突资源名称
     */
    private String resourceName;

    /**
     * 星期
     */
    private Integer weekday;

    private String weekdayName;

    /**
     * 节次
     */
    private Integer slot;

    /**
     * 周范围
     */
    private Integer startWeek;

    private Integer endWeek;

    /**
     * 冲突的课表条目ID
     */
    private Long conflictingEntryId;

    /**
     * 冲突描述
     */
    private String description;

    /**
     * 获取冲突类型名称
     */
    public static String getConflictTypeName(String type) {
        if (type == null) return "";
        return switch (type) {
            case "TEACHER" -> "教师冲突";
            case "CLASS" -> "班级冲突";
            case "CLASSROOM" -> "教室冲突";
            default -> type;
        };
    }
}
