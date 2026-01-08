package com.school.management.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 学生入住请求DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
public class StudentCheckInRequest {

    /**
     * 学生ID
     */
    @NotNull(message = "学生ID不能为空")
    private Long studentId;

    /**
     * 宿舍ID
     */
    @NotNull(message = "宿舍ID不能为空")
    private Long dormitoryId;

    /**
     * 床位号
     */
    private String bedNumber;

    /**
     * 入住日期(默认当天)
     */
    private LocalDate checkInDate;

    /**
     * 备注
     */
    private String remark;
}
