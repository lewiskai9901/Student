package com.school.management.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户响应DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户响应")
public class UserResponse {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "性别: 1-男 2-女")
    private Integer gender;

    @Schema(description = "性别描述")
    private String genderName;

    @Schema(description = "用户状态: 1-启用 0-禁用")
    private Integer status;

    @Schema(description = "状态描述")
    private String statusName;

    @Schema(description = "组织单元ID")
    private Long orgUnitId;

    @Schema(description = "组织单元名称")
    private String orgUnitName;

    @Schema(description = "班级ID")
    private Long classId;

    @Schema(description = "班级名称")
    private String className;

    @Schema(description = "工号")
    private String employeeNo;

    @Schema(description = "用户类型: 1-学生 2-教师 3-管理员")
    private Integer userType;

    @Schema(description = "用户类型描述")
    private String userTypeName;

    @Schema(description = "角色列表")
    private List<RoleInfo> roles;

    @Schema(description = "角色名称列表")
    private List<String> roleNames;

    @Schema(description = "权限列表")
    private List<String> permissions;

    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginTime;

    @Schema(description = "最后登录IP")
    private String lastLoginIp;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "更新人")
    private String updateBy;

    @Schema(description = "备注")
    private String remarks;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "角色信息")
    public static class RoleInfo {

        @Schema(description = "角色ID")
        private Long id;

        @Schema(description = "角色代码")
        private String code;

        @Schema(description = "角色名称")
        private String name;

        @Schema(description = "角色描述")
        private String description;
    }
}