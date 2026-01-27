package com.school.management.interfaces.rest.teaching;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 创建考试安排请求
 */
@Data
public class CreateExamArrangementRequest {

    @NotNull(message = "考试批次ID不能为空")
    private Long batchId;

    @NotNull(message = "课程ID不能为空")
    private Long courseId;

    @NotNull(message = "考试日期不能为空")
    private LocalDate examDate;

    @NotNull(message = "开始时间不能为空")
    private LocalTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalTime endTime;

    private List<ExamRoomItem> examRooms;

    private String remark;

    @Data
    public static class ExamRoomItem {
        @NotNull(message = "教室ID不能为空")
        private Long classroomId;
        private Integer capacity;
        private List<Long> invigilatorIds;
    }
}
