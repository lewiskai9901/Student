package com.school.management.dto.student;

import lombok.Data;

/**
 * 学生宿舍信息DTO
 * 包含：宿舍ID、床位号等住宿相关信息
 *
 * @author system
 * @version 2.0.0
 * @since 2024-12-31
 */
@Data
public class StudentDormitoryInfoDTO {

    /**
     * 宿舍ID
     */
    private Long dormitoryId;

    /**
     * 床位号
     */
    private String bedNumber;
}
