package com.school.management.interfaces.rest.teaching;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class RecordGradeRequest {
    private Long batchId;
    @NotNull(message = "学生ID不能为空")
    private Long studentId;
    private BigDecimal totalScore;
    private List<GradeItemRequest> items;
    private String remark;

    @Data
    public static class GradeItemRequest {
        private String itemName;
        private BigDecimal score;
    }
}
