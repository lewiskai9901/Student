package com.school.management.application.teaching.query;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 学生成绩查询DTO
 */
@Data
@Builder
public class StudentGradeDTO {

    private Long id;
    private Long batchId;
    private Long studentId;
    private String studentCode;
    private String studentName;
    private Long courseId;
    private String courseCode;
    private String courseName;
    private BigDecimal credits;
    private BigDecimal totalScore;
    private String gradeLevel;
    private BigDecimal gradePoint;
    private Integer status;
    private String statusName;
    private String remark;
    private List<GradeItemDTO> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class GradeItemDTO {
        private Long id;
        private String itemName;
        private BigDecimal score;
        private Integer weight;
    }

    public static String getStatusName(Integer status) {
        if (status == null) return "";
        return switch (status) { case 0 -> "待录入"; case 1 -> "已录入"; case 2 -> "已确认"; default -> ""; };
    }
}
