package com.school.management.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

/**
 * 用户更新请求DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
@Schema(description = "用户更新请求")
public class UserUpdateRequest {

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
    private Integer status;

    @Schema(description = "部门ID")
    private Long departmentId;

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