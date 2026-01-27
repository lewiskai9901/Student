package com.school.management.interfaces.rest.teaching;

import lombok.Data;

import java.time.LocalDate;

/**
 * 创建学年请求
 */
@Data
public class CreateAcademicYearRequest {

    /**
     * 学年代码，如2025-2026
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
}
