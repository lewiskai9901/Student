package com.school.management.application.teaching.query;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 考试教室查询DTO
 */
@Data
@Builder
public class ExamRoomDTO {

    private Long id;

    private Long arrangementId;

    private Long classroomId;

    private String classroomName;

    private String buildingName;

    /**
     * 容纳人数
     */
    private Integer capacity;

    /**
     * 实际考生数
     */
    private Integer actualCount;

    /**
     * 监考教师列表
     */
    private List<InvigilatorDTO> invigilators;

    @Data
    @Builder
    public static class InvigilatorDTO {
        private Long id;
        private Long teacherId;
        private String teacherName;
        private Boolean isMain;
    }
}
