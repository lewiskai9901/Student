package com.school.management.dto.export;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 导出模板DTO
 */
@Data
public class ExportTemplateDTO {

    private Long id;
    private Long planId;
    private String planName;
    private String templateName;
    private String description;
    private String outputFormat;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;

    /**
     * 筛选配置
     */
    private FilterConfig filterConfig;

    /**
     * 表格配置（向后兼容，使用第一个表格）
     */
    private TableConfig tableConfig;

    /**
     * 多表格配置
     */
    private List<TableConfig> tables;

    /**
     * 文档模板（富文本HTML）
     */
    private String documentTemplate;

    /**
     * 筛选配置内部类
     */
    @Data
    public static class FilterConfig {
        /**
         * 选中的扣分项ID列表
         */
        private List<Long> deductionItemIds;

        /**
         * 选中的轮次列表
         */
        private List<Integer> checkRounds;

        /**
         * 选中的类别ID列表
         */
        private List<Long> categoryIds;

        /**
         * 是否包含所有扣分项
         */
        private Boolean includeAllItems;
    }

    /**
     * 表格配置内部类
     */
    @Data
    public static class TableConfig {
        /**
         * 表格标题
         */
        private String title;

        /**
         * 是否显示标题
         */
        private Boolean showTitle;

        /**
         * 表格列配置
         */
        private List<ColumnConfig> columns;

        /**
         * 是否显示部门分组头
         */
        private Boolean showDepartmentHeader;

        /**
         * 是否显示年级分组头
         */
        private Boolean showGradeHeader;

        /**
         * 是否显示班级分组头
         */
        private Boolean showClassHeader;

        /**
         * 合并配置
         */
        private MergeConfig mergeConfig;

        // 样式配置
        private String borderColor;
        private String borderWidth;
        private String borderStyle;
        private String headerBgColor;
        private String headerTextColor;
        private String headerFontWeight;
        private String headerFontSize;
        private String cellPadding;
        private String fontSize;
        private String textAlign;
        private String verticalAlign;
        private String lineHeight;
        private Boolean zebraStripes;
        private String zebraColor;
    }

    /**
     * 合并配置
     */
    @Data
    public static class MergeConfig {
        /**
         * 是否启用合并
         */
        private Boolean enabled;

        /**
         * 分组层级字段列表（如：["orgUnitName", "gradeName", "className"]）
         * 这些列自动使用 rowspan 合并
         */
        private List<String> hierarchyLevels;

        /**
         * 是否将同组数据合并成一行（非层级列使用分隔符拼接）
         */
        private Boolean concatDataRows;

        /**
         * 数据分隔符（仅当 concatDataRows=true 时使用）
         */
        private String separator;
    }

    /**
     * 列配置
     */
    @Data
    public static class ColumnConfig {
        private String field;
        private String label;

        /**
         * 列别名（自定义显示名称，优先于label）
         */
        private String alias;

        private Integer width;
        private String align;

        /**
         * 合并类型：none=不合并, group=分组合并(rowspan), concat=数据合并(拼接)
         */
        private String mergeType;

        /**
         * 分隔符（仅当 mergeType=concat 时使用）
         */
        private String separator;

        /**
         * 分组优先级（仅当 mergeType=group 时使用，数字越小优先级越高）
         */
        private Integer groupOrder;

        /**
         * 参考分组字段（仅当 mergeType=concat 时使用，指定按哪个分组列进行数据合并）
         */
        private String groupByField;

        /**
         * 是否为组合字段
         */
        private Boolean isComposite;

        /**
         * 组合字段包含的字段列表
         */
        private List<String> compositeFields;

        /**
         * 组合字段的分隔符
         */
        private String compositeSeparator;
    }
}
