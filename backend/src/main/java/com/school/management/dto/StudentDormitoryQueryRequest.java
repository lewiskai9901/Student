package com.school.management.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 学生住宿记录查询请求DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StudentDormitoryQueryRequest extends BaseQueryRequest {

    /**
     * 学生ID
     */
    private Long studentId;

    /**
     * 学生学号
     */
    private String studentNo;

    /**
     * 学生姓名
     */
    private String studentName;

    /**
     * 宿舍ID
     */
    private Long dormitoryId;

    /**
     * 楼宇ID
     */
    private Long buildingId;

    /**
     * 楼层
     */
    private Integer floorNumber;

    /**
     * 状态: 1在住 2已退宿 3调换中
     */
    private Integer status;

    /**
     * 入住日期开始
     */
    private LocalDate checkInDateStart;

    /**
     * 入住日期结束
     */
    private LocalDate checkInDateEnd;
}
