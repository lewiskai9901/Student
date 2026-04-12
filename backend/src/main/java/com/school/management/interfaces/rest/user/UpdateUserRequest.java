package com.school.management.interfaces.rest.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 更新用户请求 DTO
 */
@Data
public class UpdateUserRequest {

    private String realName;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱不能超过100字符")
    private String email;

    private String employeeNo;

    private Integer gender;

    private LocalDate birthDate;

    private String idCard;

    private Long orgUnitId;

    private String userTypeCode;

    private List<Long> roleIds;
}
