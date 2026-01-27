package com.school.management.interfaces.rest.teaching;

import lombok.Data;

import java.time.LocalDate;

/**
 * 更新考试批次请求
 */
@Data
public class UpdateExamBatchRequest {

    private String batchName;

    private Integer examType;

    private LocalDate startDate;

    private LocalDate endDate;

    private String remark;
}
