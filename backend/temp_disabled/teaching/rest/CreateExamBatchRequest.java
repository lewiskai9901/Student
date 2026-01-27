package com.school.management.interfaces.rest.teaching;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 创建考试批次请求
 */
@Data
public class CreateExamBatchRequest {

    @NotNull(message = "学期ID不能为空")
    private Long semesterId;

    @NotBlank(message = "批次名称不能为空")
    private String batchName;

    @NotNull(message = "考试类型不能为空")
    private Integer examType;

    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    private String remark;
}
