package com.school.management.application.teaching.query;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 考试安排查询DTO
 */
@Data
@Builder
public class ExamArrangementDTO {

    private Long id;

    private Long batchId;

    private String batchName;

    private Long courseId;

    private String courseCode;

    private String courseName;

    private LocalDate examDate;

    private String examDateStr;

    private Integer weekday;

    private String weekdayName;

    private LocalTime startTime;

    private LocalTime endTime;

    private String timeRange;

    /**
     * 状态: 0-待考 1-进行中 2-已完成
     */
    private Integer status;

    private String statusName;

    private String remark;

    /**
     * 考试教室列表
     */
    private List<ExamRoomDTO> examRooms;

    /**
     * 参考学生数
     */
    private Integer studentCount;

    /**
     * 获取状态名称
     */
    public static String getStatusName(Integer status) {
        if (status == null) return "";
        return switch (status) {
            case 0 -> "待考";
            case 1 -> "进行中";
            case 2 -> "已完成";
            default -> "";
        };
    }
}
