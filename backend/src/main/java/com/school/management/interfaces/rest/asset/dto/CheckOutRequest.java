package com.school.management.interfaces.rest.asset.dto;

import lombok.Data;

/**
 * 学生退宿请求DTO
 */
@Data
public class CheckOutRequest {

    /**
     * 学生ID
     */
    private Long studentId;

    /**
     * 学生姓名
     */
    private String studentName;

    /**
     * 床位号
     */
    private Integer bedNumber;
}
