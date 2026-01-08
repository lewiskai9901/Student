package com.school.management.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 学生退宿请求DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
public class StudentCheckOutRequest {

    /**
     * 学生ID
     */
    @NotNull(message = "学生ID不能为空")
    private Long studentId;

    /**
     * 退宿日期(默认当天)
     */
    private LocalDate checkOutDate;

    /**
     * 退宿原因
     */
    private String reason;
}
