package com.school.management.dto.request;

import com.school.management.exception.BusinessException;
import lombok.Data;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * 创建检查计划请求DTO
 *
 * @author system
 * @since 3.0.0
 */
@Data
public class CheckPlanCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 计划名称
     */
    @NotBlank(message = "计划名称不能为空")
    @Size(max = 100, message = "计划名称不能超过100字符")
    private String planName;

    /**
     * 计划描述
     */
    @Size(max = 500, message = "计划描述不能超过500字符")
    private String description;

    /**
     * 模板ID
     */
    @NotNull(message = "模板ID不能为空")
    private Long templateId;

    /**
     * 计划开始日期
     */
    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    /**
     * 计划结束日期
     */
    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    /**
     * 加权方案ID(可选) - 默认/全局加权方案
     */
    private Long weightConfigId;

    /**
     * 是否启用加权(0=否,1=是)
     */
    private Integer enableWeight;

    /**
     * 自定义标准人数(可选)
     */
    private Integer customStandardSize;

    /**
     * 分类/扣分项级别的加权配置列表(多配置模式)
     */
    private List<ItemWeightConfig> itemWeightConfigs;

    /**
     * 目标范围类型: all=全部, department=按院系, grade=按年级, custom=自定义
     */
    private String targetScopeType;

    /**
     * 目标范围配置
     */
    private TargetScopeConfig targetScopeConfig;

    /**
     * 验证日期范围
     */
    public void validateDateRange() {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException("开始日期不能晚于结束日期");
        }
    }

    /**
     * 分类/扣分项级别的加权配置
     */
    @Data
    public static class ItemWeightConfig implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 扣分项ID(可选)
         */
        private Long itemId;

        /**
         * 分类ID
         */
        private Long categoryId;

        /**
         * 加权方案ID
         */
        private Long weightConfigId;
    }

    /**
     * 目标范围配置
     */
    @Data
    public static class TargetScopeConfig implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 组织单元ID列表（原院系ID列表）
         */
        private List<Long> orgUnitIds;

        /**
         * 年级ID列表
         */
        private List<Long> gradeIds;

        /**
         * 班级ID列表(精确指定)
         */
        private List<Long> classIds;

        /**
         * 排除的班级ID列表
         */
        private List<Long> excludeClassIds;
    }
}
