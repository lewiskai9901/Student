package com.school.management.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学生住宿记录响应DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
public class StudentDormitoryResponse {

    /**
     * 记录ID
     */
    private Long id;

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
     * 班级名称
     */
    private String className;

    /**
     * 宿舍ID
     */
    private Long dormitoryId;

    /**
     * 楼宇ID
     */
    private Long buildingId;

    /**
     * 楼号
     */
    private String buildingNo;

    /**
     * 楼宇名称
     */
    private String buildingName;

    /**
     * 房间号
     */
    private String dormitoryNo;

    /**
     * 楼层
     */
    private Integer floorNumber;

    /**
     * 床位号
     */
    private String bedNumber;

    /**
     * 入住日期
     */
    private LocalDate checkInDate;

    /**
     * 退宿日期
     */
    private LocalDate checkOutDate;

    /**
     * 状态: 1在住 2已退宿 3调换中
     */
    private Integer status;

    /**
     * 状态名称
     */
    private String statusName;

    /**
     * 变动原因
     */
    private String changeReason;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
