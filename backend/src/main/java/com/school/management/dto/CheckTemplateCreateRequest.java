package com.school.management.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 创建检查模板请求DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
public class CheckTemplateCreateRequest {

    /**
     * 模板名称
     */
    @NotBlank(message = "模板名称不能为空")
    private String templateName;

    /**
     * 模板编码（可选，不填则自动生成）
     */
    private String templateCode;

    /**
     * 模板描述
     */
    private String description;

    /**
     * 总轮次数
     */
    private Integer totalRounds;

    /**
     * 轮次名称列表
     */
    private List<String> roundNames;

    /**
     * 是否默认模板 0否 1是
     */
    private Integer isDefault;

    /**
     * 状态 0禁用 1启用
     */
    private Integer status;

    /**
     * 检查类别列表
     */
    @NotNull(message = "检查类别不能为空")
    private List<TemplateCategoryItem> categories;

    @Data
    public static class TemplateCategoryItem {
        /**
         * 检查类别ID
         */
        @NotNull(message = "类别ID不能为空")
        private Long categoryId;

        /**
         * 关联类型 0不关联 1关联宿舍 2关联教室
         */
        private Integer linkType;

        /**
         * 排序
         */
        private Integer sortOrder;

        /**
         * 是否必检 0否 1是
         */
        private Integer isRequired;

        /**
         * 检查轮次，默认1（已废弃）
         */
        private Integer checkRounds;

        /**
         * 参与的轮次，如"1,3"或列表[1,3]
         */
        private String participatedRounds;
    }
}
