package com.school.management.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 登录响应DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "登录响应")
public class LoginResponse {

    @Schema(description = "访问令牌")
    private String accessToken;

    @Schema(description = "刷新令牌")
    private String refreshToken;

    @Schema(description = "令牌类型", example = "Bearer")
    private String tokenType = "Bearer";

    @Schema(description = "访问令牌过期时间（秒）")
    private Long expiresIn;

    @Schema(description = "用户信息")
    private UserInfo userInfo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "用户信息")
    public static class UserInfo {

        @Schema(description = "用户ID")
        private Long userId;

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

        @Schema(description = "用户状态: 1-启用 0-禁用")
        private Integer status;

        @Schema(description = "角色列表")
        private List<String> roles;

        @Schema(description = "权限列表")
        private List<String> permissions;

        @Schema(description = "组织单元信息")
        private OrgUnitInfo orgUnit;

        @Schema(description = "班级信息")
        private ClassInfo classInfo;

        @Schema(description = "用户分配的班级列表（班主任/副班主任等）")
        private List<AssignedClass> assignedClasses;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @Schema(description = "组织单元信息")
        public static class OrgUnitInfo {

            @Schema(description = "组织单元ID")
            private Long orgUnitId;

            @Schema(description = "组织单元名称")
            private String orgUnitName;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @Schema(description = "班级信息")
        public static class ClassInfo {

            @Schema(description = "班级ID")
            private Long classId;

            @Schema(description = "班级名称")
            private String className;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @Schema(description = "分配的班级信息")
        public static class AssignedClass {

            @Schema(description = "班级ID")
            private Long id;

            @Schema(description = "班级名称")
            private String className;

            @Schema(description = "角色: HEAD_TEACHER/DEPUTY_HEAD_TEACHER/SUBJECT_TEACHER/COUNSELOR")
            private String role;
        }
    }
}