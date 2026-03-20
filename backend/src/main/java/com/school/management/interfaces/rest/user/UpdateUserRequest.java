package com.school.management.interfaces.rest.user;

import jakarta.validation.constraints.Pattern;
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

    private String email;

    private String employeeNo;

    private Integer gender;

    private LocalDate birthDate;

    private String idCard;

    private Long orgUnitId;

    private String userTypeCode;

    private List<Long> roleIds;
}
