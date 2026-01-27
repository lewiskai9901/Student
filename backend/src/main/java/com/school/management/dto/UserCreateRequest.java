package com.school.management.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

/**
 * 用户创建请求DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
@Schema(description = "用户创建请求")
public class UserCreateRequest {

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$", message = "用户名只能包含字母、数字、下划线，长度4-20位")
    @Schema(description = "用户名", example = "zhangsan")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{8,20}$",
             message = "密码必须包含大小写字母和数字，长度8-20位")
    @Schema(description = "密码", example = "Password123")
    private String password;

    @NotBlank(message = "真实姓名不能为空")
    @Schema(description = "真实姓名", example = "张三")
    private String realName;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱", example = "zhangsan@example.com")
    private String email;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "性别: 1-男 2-女", example = "1")
    private Integer gender;

    @Schema(description = "用户状态: 1-启用 0-禁用", example = "1")
    private Integer status = 1;

    @Schema(description = "组织单元ID")
    private Long orgUnitId;

    @Schema(description = "班级ID")
    private Long classId;

    @Schema(description = "工号")
    private String employeeNo;

    @Schema(description = "用户类型: 1-学生 2-教师 3-管理员", example = "1")
    private Integer userType;

    @Schema(description = "角色ID列表")
    private List<Long> roleIds;

    @Schema(description = "备注")
    private String remarks;
}