package com.school.management.dto.export;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 创建/更新导出模板请求
 */
@Data
public class ExportTemplateRequest {

    /**
     * 模板ID（更新时使用）
     */
    private Long id;

    /**
     * 模板名称
     */
    @NotBlank(message = "模板名称不能为空")
    private String templateName;

    /**
     * 模板描述
     */
    private String description;

    /**
     * 输出格式：PDF/WORD/EXCEL
     */
    @NotBlank(message = "输出格式不能为空")
    private String outputFormat;

    /**
     * 筛选配置
     */
    @NotNull(message = "筛选配置不能为空")
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
     * 分组方式
     */
    private String groupBy;

    /**
     * 排序字段
     */
    private String sortBy;

    /**
     * 文档模板（富文本HTML）
     */
    private String documentTemplate;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 状态
     */
    private Integer status;

    @Data
    public static class FilterConfig {
        private List<Long> deductionItemIds;
        private List<Integer> checkRounds;
        private List<Long> categoryIds;
        private Boolean includeAllItems;
    }

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

        private List<ColumnConfig> columns;
        private Boolean showDepartmentHeader;
        private Boolean showGradeHeader;
        private Boolean showClassHeader;

        // 边框样式
        private String borderColor;
        private String borderWidth;
        private String borderStyle;

        // 表头样式
        private String headerBgColor;
        private String headerTextColor;
        private String headerFontWeight;
        private String headerFontSize;

        // 单元格样式
        private String cellPadding;
        private String fontSize;
        private String textAlign;
        private String verticalAlign;
        private String lineHeight;

        // 行样式
        private String rowHeight;
        private Boolean zebraStripes;
        private String zebraColor;
        private Boolean hoverHighlight;
        private String hoverColor;

        // 合并配置
        private MergeConfig mergeConfig;
    }

    @Data
    public static class MergeConfig {
        private Boolean enabled;
        private List<String> hierarchyLevels;
        private Boolean concatDataRows;
        private String separator;
    }

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
        private String mergeType;
        private String separator;
        private Integer groupOrder;
        private String groupByField;
    }
}
