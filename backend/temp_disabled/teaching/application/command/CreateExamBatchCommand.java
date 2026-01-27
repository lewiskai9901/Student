package com.school.management.application.teaching.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * 创建考试批次命令
 */
@Data
@Builder
public class CreateExamBatchCommand {

    @NotNull(message = "学期ID不能为空")
    private Long semesterId;

    @NotBlank(message = "批次名称不能为空")
    private String batchName;

    /**
     * 考试类型: 1-期中考试 2-期末考试 3-补考 4-结业考试
     */
    @NotNull(message = "考试类型不能为空")
    private Integer examType;

    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    private String remark;

    private Long operatorId;
}
