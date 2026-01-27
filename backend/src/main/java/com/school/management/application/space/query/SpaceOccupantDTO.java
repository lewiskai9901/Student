package com.school.management.application.space.query;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 场所占用者DTO
 */
@Data
public class SpaceOccupantDTO {

    private Long id;
    private Long spaceId;
    private String spaceName;
    private String occupantType;
    private Long occupantId;
    private String occupantName;
    private String occupantNo;
    private Integer positionNo;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer status;
    private String statusText;
    private String remark;
    private LocalDateTime createdAt;

    // 额外信息（学生/教师特有）
    private String className;
    private String departmentName;
    private Integer gender;
    private String phone;
}
