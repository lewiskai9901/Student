package com.school.management.application.teaching.command;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 录入成绩命令
 */
@Data
@Builder
public class RecordGradeCommand {

    @NotNull(message = "成绩批次ID不能为空")
    private Long batchId;

    @NotNull(message = "学生ID不能为空")
    private Long studentId;

    /**
     * 总成绩
     */
    private BigDecimal totalScore;

    /**
     * 分项成绩
     */
    private List<GradeItem> items;

    /**
     * 备注
     */
    private String remark;

    private Long operatorId;

    @Data
    @Builder
    public static class GradeItem {
        private String itemName;
        private BigDecimal score;
    }
}
