package com.school.management.application.myclass.query;

import lombok.Data;
import lombok.Builder;

/**
 * 班级学生列表项 DTO
 */
@Data
@Builder
public class MyClassStudentDTO {
    private Long id;
    private String studentNo;
    private String name;
    private String gender;
    private String phone;
    private String dormitoryName;
    private String bedNo;
    private String status;
}
