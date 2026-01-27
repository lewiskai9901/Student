package com.school.management.interfaces.rest.inspection.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTOs for teacher dashboard endpoints.
 */
public class TeacherDashboardResponse {

    @Data
    public static class Overview {
        private Long classId;
        private BigDecimal weeklyDeduction;
        private BigDecimal weeklyBonus;
        private int recordCount;
    }

    @Data
    public static class DeductionDetail {
        private Long id;
        private Long sessionId;
        private String itemName;
        private String categoryName;
        private String spaceType;
        private String spaceName;
        private Integer personCount;
        private BigDecimal deductionAmount;
        private String remark;
        private LocalDateTime createdAt;
    }

    @Data
    public static class TopIssue {
        private String issueName;
        private int occurrenceCount;
        private BigDecimal totalDeduction;
        private String categoryName;
    }

    @Data
    public static class StudentViolation {
        private Long studentId;
        private String studentName;
        private int violationCount;
        private BigDecimal totalDeduction;
        private List<String> violationTypes;
    }

    @Data
    public static class ImprovementData {
        private BigDecimal currentDeduction;
        private BigDecimal previousDeduction;
        private BigDecimal change;
        private BigDecimal changePercent;
        private boolean improved;
    }
}
