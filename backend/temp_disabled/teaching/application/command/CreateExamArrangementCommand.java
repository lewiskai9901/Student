package com.school.management.application.teaching.command;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 创建考试安排命令
 */
@Data
@Builder
public class CreateExamArrangementCommand {

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

    /**
     * 考试教室列表
     */
    private List<ExamRoomItem> examRooms;

    private String remark;

    private Long operatorId;

    @Data
    @Builder
    public static class ExamRoomItem {
        @NotNull(message = "教室ID不能为空")
        private Long classroomId;

        /**
         * 容纳人数
         */
        private Integer capacity;

        /**
         * 监考教师ID列表
         */
        private List<Long> invigilatorIds;
    }
}
