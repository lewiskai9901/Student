package com.school.management.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 用户数据范围请求DTO
 *
 * @author system
 * @version 3.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDataScopeRequest {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 范围类型: DEPARTMENT-部门, GRADE-年级, CLASS-班级
     */
    @NotBlank(message = "范围类型不能为空")
    private String scopeType;

    /**
     * 范围ID
     */
    @NotNull(message = "范围ID不能为空")
    private Long scopeId;

    /**
     * 是否包含下级: 1-是, 0-否
     */
    private Integer includeChildren = 1;

    /**
     * 批量添加请求
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchAdd {

        /**
         * 用户ID
         */
        @NotNull(message = "用户ID不能为空")
        private Long userId;

        /**
         * 数据范围列表
         */
        @NotEmpty(message = "数据范围列表不能为空")
        private List<ScopeItem> scopes;
    }

    /**
     * 范围项
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScopeItem {

        /**
         * 范围类型
         */
        @NotBlank(message = "范围类型不能为空")
        private String scopeType;

        /**
         * 范围ID
         */
        @NotNull(message = "范围ID不能为空")
        private Long scopeId;

        /**
         * 是否包含下级
         */
        private Integer includeChildren = 1;
    }

    /**
     * 批量删除请求
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchDelete {

        /**
         * ID列表
         */
        @NotEmpty(message = "ID列表不能为空")
        private List<Long> ids;
    }
}
