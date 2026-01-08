package com.school.management.interfaces.rest.user;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 更新用户请求 DTO
 */
@Data
public class UpdateUserRequest {

    private String realName;

    private String phone;

    private String email;

    private String employeeNo;

    private Integer gender;

    private LocalDate birthDate;

    private String idCard;

    private Long departmentId;

    private List<Long> roleIds;
}
