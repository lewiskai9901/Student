package com.school.management.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 学生调换宿舍请求DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
public class StudentChangeDormitoryRequest {

    /**
     * 学生ID
     */
    @NotNull(message = "学生ID不能为空")
    private Long studentId;

    /**
     * 新宿舍ID
     */
    @NotNull(message = "新宿舍ID不能为空")
    private Long newDormitoryId;

    /**
     * 新床位号
     */
    private String newBedNumber;

    /**
     * 调换日期(默认当天)
     */
    private LocalDate changeDate;

    /**
     * 调换原因
     */
    private String reason;
}
