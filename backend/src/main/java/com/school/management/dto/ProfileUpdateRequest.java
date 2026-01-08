package com.school.management.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 个人资料更新请求
 *
 * @author system
 * @since 1.0.0
 */
@Data
@Schema(description = "个人资料更新请求")
public class ProfileUpdateRequest {

    @Schema(description = "真实姓名")
    @Size(max = 50, message = "姓名长度不能超过50个字符")
    private String realName;

    @Schema(description = "手机号")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "邮箱")
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;

    @Schema(description = "头像URL")
    @Size(max = 255, message = "头像URL长度不能超过255个字符")
    private String avatar;

    @Schema(description = "性别: 1男 2女")
    private Integer gender;
}
