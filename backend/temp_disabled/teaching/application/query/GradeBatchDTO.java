package com.school.management.application.teaching.query;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 成绩批次查询DTO
 */
@Data
@Builder
public class GradeBatchDTO {

    private Long id;
    private Long semesterId;
    private String semesterName;
    private Long courseId;
    private String courseCode;
    private String courseName;
    private Long classId;
    private String className;
    private String batchName;
    private Integer gradeType;
    private String gradeTypeName;
    private String compositions;
    private Integer status;
    private String statusName;
    private LocalDateTime deadline;
    private Integer totalCount;
    private Integer recordedCount;
    private Long createdBy;
    private LocalDateTime createdAt;

    public static String getGradeTypeName(Integer type) {
        if (type == null) return "";
        return switch (type) { case 1 -> "百分制"; case 2 -> "五级制"; case 3 -> "二级制"; default -> ""; };
    }

    public static String getStatusName(Integer status) {
        if (status == null) return "";
        return switch (status) { case 0 -> "录入中"; case 1 -> "已提交"; case 2 -> "已审核"; case 3 -> "已发布"; default -> ""; };
    }
}
