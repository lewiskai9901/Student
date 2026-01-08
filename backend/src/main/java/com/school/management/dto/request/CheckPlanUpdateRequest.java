package com.school.management.dto.request;

import com.school.management.exception.BusinessException;
import lombok.Data;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 更新检查计划请求DTO
 *
 * @author system
 * @since 3.0.0
 */
@Data
public class CheckPlanUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 计划ID
     */
    @NotNull(message = "计划ID不能为空")
    private Long id;

    /**
     * 计划名称
     */
    @Size(max = 100, message = "计划名称不能超过100字符")
    private String planName;

    /**
     * 计划描述
     */
    @Size(max = 500, message = "计划描述不能超过500字符")
    private String description;

    /**
     * 计划开始日期
     */
    private LocalDate startDate;

    /**
     * 计划结束日期
     */
    private LocalDate endDate;

    /**
     * 加权方案ID(可选)
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
    private java.util.List<CheckPlanCreateRequest.ItemWeightConfig> itemWeightConfigs;

    /**
     * 目标范围类型: all=全部, department=按院系, grade=按年级, custom=自定义
     */
    private String targetScopeType;

    /**
     * 目标范围配置
     */
    private CheckPlanCreateRequest.TargetScopeConfig targetScopeConfig;

    /**
     * 验证日期范围
     */
    public void validateDateRange() {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException("开始日期不能晚于结束日期");
        }
    }
}
