package com.school.management.dto.inspector;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 打分人员DTO
 */
@Data
public class InspectorDTO {

    /**
     * 打分人员ID
     */
    private Long id;

    /**
     * 检查计划ID
     */
    private Long planId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户姓名
     */
    private String userName;

    /**
     * 用户账号
     */
    private String username;

    /**
     * 组织单元名称（原部门名称）
     */
    private String orgUnitName;

    /**
     * 状态：1启用 0禁用
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 权限配置列表
     */
    private List<PermissionDTO> permissions;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    // 手动添加getter/setter方法
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getOrgUnitName() {
        return orgUnitName;
    }

    public void setOrgUnitName(String orgUnitName) {
        this.orgUnitName = orgUnitName;
    }

    /**
     * 权限配置DTO
     */
    @Data
    public static class PermissionDTO {
        /**
         * 权限ID
         */
        private Long id;

        /**
         * 检查类别ID
         */
        private String categoryId;

        /**
         * 检查类别名称
         */
        private String categoryName;

        /**
         * 可检查的班级ID列表，null表示全部
         */
        private List<Long> classIds;

        /**
         * 可检查的班级名称列表（用于展示）
         */
        private List<String> classNames;
    }
}
