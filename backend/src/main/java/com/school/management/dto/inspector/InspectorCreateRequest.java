package com.school.management.dto.inspector;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 添加打分人员请求
 */
@Data
public class InspectorCreateRequest {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 备注
     */
    private String remark;

    /**
     * 权限配置列表
     */
    @NotEmpty(message = "权限配置不能为空")
    private List<PermissionConfig> permissions;

    /**
     * 权限配置
     */
    @Data
    public static class PermissionConfig {
        /**
         * 检查类别ID
         */
        @NotNull(message = "类别ID不能为空")
        private String categoryId;

        /**
         * 检查类别名称
         */
        private String categoryName;

        /**
         * 可检查的班级ID列表，null或空表示计划范围内全部
         */
        private List<Long> classIds;
    }
}
