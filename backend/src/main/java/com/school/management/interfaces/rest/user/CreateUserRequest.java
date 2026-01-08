package com.school.management.interfaces.rest.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 创建用户请求 DTO
 */
@Data
public class CreateUserRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    private String password;

    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    private String phone;

    private String email;

    private String employeeNo;

    private Integer gender;

    private LocalDate birthDate;

    private String idCard;

    private Long departmentId;

    private Integer userType;

    private List<Long> roleIds;
}
