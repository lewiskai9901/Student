package com.school.management.application.teaching.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学年DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicYearDTO {

    private Long id;

    /**
     * 学年代码
     */
    private String yearCode;

    /**
     * 学年名称
     */
    private String yearName;

    /**
     * 学年开始日期
     */
    private LocalDate startDate;

    /**
     * 学年结束日期
     */
    private LocalDate endDate;

    /**
     * 是否当前学年
     */
    private Boolean isCurrent;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
