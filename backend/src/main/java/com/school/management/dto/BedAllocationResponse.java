package com.school.management.dto;

import lombok.Data;

/**
 * 床位分配响应DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
public class BedAllocationResponse {

    /**
     * 床位号
     */
    private String bedNumber;

    /**
     * 学生ID
     */
    private Long studentId;

    /**
     * 学号
     */
    private String studentNo;

    /**
     * 学生姓名
     */
    private String studentName;

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 班级名称
     */
    private String className;

    /**
     * 班主任ID
     */
    private Long teacherId;

    /**
     * 班主任姓名
     */
    private String teacherName;

    /**
     * 是否已分配
     */
    private Boolean isAssigned;
}
