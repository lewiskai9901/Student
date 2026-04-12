package com.school.management.interfaces.rest.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 创建用户请求 DTO
 */
@Data
public class CreateUserRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 30, message = "用户名长度须为3-30位")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]*$", message = "用户名只能包含英文字母、数字和下划线，且必须以字母开头")
    private String username;

    private String password;

    @NotBlank(message = "真实姓名不能为空")
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

    private Long placeId;

    private String userTypeCode;

    private List<Long> roleIds;
}
