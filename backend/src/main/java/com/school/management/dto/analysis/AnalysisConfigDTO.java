package com.school.management.dto.analysis;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 统计分析配置DTO - 重构版本
 * 支持高度可配置的统计分析
 */
@Data
@Schema(description = "统计分析配置")
public class AnalysisConfigDTO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "检查计划ID", required = true)
    @NotNull(message = "检查计划ID不能为空")
    private Long planId;

    @Schema(description = "检查计划名称")
    private String planName;

    @Schema(description = "配置名称", required = true)
    @NotBlank(message = "配置名称不能为空")
    @Size(max = 100, message = "配置名称不能超过100字符")
    private String configName;

    @Schema(description = "配置描述")
    @Size(max = 500, message = "描述不能超过500字符")
    private String configDesc;

    // ==================== 范围配置 ====================

    @Schema(description = "范围类型: time-时间范围, record-记录选择, mixed-混合")
    private String scopeType = "time";

    @Schema(description = "范围配置(Map形式，支持startDate, endDate, recordIds等)")
    private Map<String, Object> scopeConfig;

    // ==================== 目标配置 ====================

    @Schema(description = "目标类型: all-全部, department-部门, grade-年级, custom-自定义")
    private String targetType = "all";

    @Schema(description = "目标配置(Map形式，支持departmentIds, gradeIds, classIds等)")
    private Map<String, Object> targetConfig;

    // ==================== 更新模式 ====================

    @Schema(description = "更新模式: static-静态, dynamic-动态")
    private String updateMode = "static";

    @Schema(description = "是否自动刷新")
    private Boolean autoRefresh = false;

    @Schema(description = "刷新间隔(分钟)")
    private Integer refreshInterval;

    @Schema(description = "上次刷新时间")
    private LocalDateTime lastRefreshTime;

    // ==================== 处理策略 ====================

    @Schema(description = "缺检策略: avg-平均扣分, weighted-加权平均, full_only-仅全覆盖, penalty-缺检惩罚, exempt-缺检豁免")
    private String missingStrategy = "avg";

    @Schema(description = "缺检策略配置")
    private Map<String, Object> missingStrategyConfig;

    // ==================== 类别映射 ====================

    @Schema(description = "类别映射列表")
    private List<CategoryMappingDTO> categoryMappings;

    // ==================== 布局配置 ====================

    @Schema(description = "布局配置")
    private Map<String, Object> layoutConfig;

    // ==================== 状态 ====================

    @Schema(description = "是否启用")
    private Boolean isEnabled = true;

    @Schema(description = "是否公开")
    private Boolean isPublic = false;

    @Schema(description = "是否默认配置")
    private Boolean isDefault = false;

    @Schema(description = "排序序号")
    private Integer sortOrder = 0;

    // ==================== 指标配置 ====================

    @Schema(description = "指标配置列表")
    private List<AnalysisMetricDTO> metrics;

    // ==================== 旧系统兼容字段 ====================
    // 以下字段用于兼容旧版本的AnalysisConfigServiceImpl

    @Schema(description = "配置名称(旧版兼容)")
    private String name;

    @Schema(description = "目标名称(旧版兼容)")
    private String targetName;

    @Schema(description = "报告章节(旧版兼容)")
    private List<String> reportSections;

    @Schema(description = "模板ID(旧版兼容)")
    private Long templateId;

    @Schema(description = "类别ID(旧版兼容)")
    private Long categoryId;

    @Schema(description = "扣分项ID(旧版兼容)")
    private Long deductionItemId;

    @Schema(description = "检查记录ID(旧版兼容)")
    private Long checkRecordId;

    @Schema(description = "部门ID(旧版兼容)")
    private Long departmentId;

    @Schema(description = "班级ID(旧版兼容)")
    private Long classId;

    @Schema(description = "时间范围类型(旧版兼容)")
    private String timeRangeType;

    @Schema(description = "固定开始日期(旧版兼容)")
    private java.time.LocalDate fixedStartDate;

    @Schema(description = "固定结束日期(旧版兼容)")
    private java.time.LocalDate fixedEndDate;

    @Schema(description = "动态范围(旧版兼容)")
    private String dynamicRange;

    @Schema(description = "组织范围类型(旧版兼容)")
    private String orgScopeType;

    @Schema(description = "组织范围ID列表(旧版兼容)")
    private List<Long> orgScopeIds;

    // ==================== 元数据 ====================

    @Schema(description = "创建者ID")
    private Long creatorId;

    @Schema(description = "创建者姓名")
    private String creatorName;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    // ==================== 统计信息（只读） ====================

    @Schema(description = "指标数量")
    private Integer metricCount;

    @Schema(description = "快照数量")
    private Integer snapshotCount;

    // ==================== 内部类 ====================

    /**
     * 范围配置
     */
    @Data
    @Schema(description = "范围配置")
    public static class ScopeConfigDTO {
        @Schema(description = "范围模式: fixed-固定范围, dynamic-动态范围")
        private String mode = "fixed";

        @Schema(description = "起始日期 (YYYY-MM-DD)")
        private String startDate;

        @Schema(description = "结束日期 (YYYY-MM-DD), 动态模式可为空")
        private String endDate;

        @Schema(description = "是否包含起始日")
        private Boolean includeStartDay = true;

        @Schema(description = "是否包含结束日")
        private Boolean includeEndDay = true;

        @Schema(description = "手动选择的记录ID列表")
        private List<Long> selectedRecordIds;

        @Schema(description = "排除的记录ID列表")
        private List<Long> excludeRecordIds;

        @Schema(description = "条件筛选")
        private FilterConditionsDTO conditions;
    }

    /**
     * 筛选条件
     */
    @Data
    @Schema(description = "筛选条件")
    public static class FilterConditionsDTO {
        @Schema(description = "检查类型")
        private List<String> checkTypes;

        @Schema(description = "模板ID列表")
        private List<Long> templateIds;

        @Schema(description = "状态列表")
        private List<Integer> statusList;
    }

    /**
     * 目标配置
     */
    @Data
    @Schema(description = "目标配置")
    public static class TargetConfigDTO {
        @Schema(description = "部门ID列表")
        private List<Long> departmentIds;

        @Schema(description = "是否包含子部门")
        private Boolean includeDepartmentChildren = true;

        @Schema(description = "年级ID列表")
        private List<Long> gradeIds;

        @Schema(description = "班级ID列表")
        private List<Long> classIds;

        @Schema(description = "排除的班级ID列表")
        private List<Long> excludeClassIds;

        @Schema(description = "动态条件")
        private DynamicConditionsDTO dynamicConditions;
    }

    /**
     * 动态条件
     */
    @Data
    @Schema(description = "动态条件")
    public static class DynamicConditionsDTO {
        @Schema(description = "最小学生数")
        private Integer minStudentCount;

        @Schema(description = "最大学生数")
        private Integer maxStudentCount;

        @Schema(description = "只包含活跃班级")
        private Boolean onlyActiveClasses = true;
    }

    /**
     * 类别映射
     */
    @Data
    @Schema(description = "类别映射")
    public static class CategoryMappingDTO {
        @Schema(description = "映射ID")
        private Long id;

        @Schema(description = "模板类别ID")
        private Long templateCategoryId;

        @Schema(description = "模板类别名称")
        private String templateCategoryName;

        @Schema(description = "统一类别ID")
        private Long unifiedCategoryId;

        @Schema(description = "统一类别名称")
        private String unifiedCategoryName;
    }
}
