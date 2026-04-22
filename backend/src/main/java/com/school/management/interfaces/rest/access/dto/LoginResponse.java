package com.school.management.interfaces.rest.access.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private UserInfo userInfo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long userId;
        private String username;
        private String realName;
        private String phone;
        private String email;
        private String avatar;
        private Integer gender;
        private Integer status;
        private List<String> roles;
        /**
         * #7 用户端角色禁用提示: 扩展 roles 为 [{code, name, industry, pluginEnabled, status}] ,
         * 前端优先用此字段渲染以标注"插件禁用中"角标. roles 保留为兼容字段.
         * 这里**不过滤** plugin_enabled=0 的角色, 让用户明确知道自己有哪些被软失效的角色.
         */
        private List<Map<String, Object>> roleDetails;
        private List<String> permissions;
        private Long orgUnitId;
        private Long tenantId;
        /** 用户类型编码（TEACHER / STUDENT / …），前端用于登录落地页决策。 */
        private String userTypeCode;
    }
}
