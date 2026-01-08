package com.school.management.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 检查计划详情VO
 *
 * @author system
 * @since 3.0.0
 */
@Data
public class CheckPlanVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 计划ID
     */
    private Long id;

    /**
     * 计划编号
     */
    private String planCode;

    /**
     * 计划名称
     */
    private String planName;

    /**
     * 计划描述
     */
    private String description;

    /**
     * 模板ID
     */
    private Long templateId;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 模板快照(JSON字符串)
     */
    private String templateSnapshot;

    /**
     * 计划开始日期
     */
    private LocalDate startDate;

    /**
     * 计划结束日期
     */
    private LocalDate endDate;

    /**
     * 加权方案ID
     */
    private Long weightConfigId;

    /**
     * 加权方案名称
     */
    private String weightConfigName;

    /**
     * 是否启用加权
     */
    private Integer enableWeight;

    /**
     * 自定义标准人数
     */
    private Integer customStandardSize;

    /**
     * 状态(0=草稿,1=进行中,2=已结束,3=已归档)
     */
    private Integer status;

    /**
     * 状态描述
     */
    private String statusText;

    /**
     * 检查次数
     */
    private Integer totalChecks;

    /**
     * 检查记录数
     */
    private Integer totalRecords;

    /**
     * 总扣分
     */
    private BigDecimal totalDeductionScore;

    /**
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 创建人姓名
     */
    private String creatorName;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 目标范围类型: all=全部, department=按院系, grade=按年级, custom=自定义
     */
    private String targetScopeType;

    /**
     * 目标范围配置(JSON字符串)
     */
    private String targetScopeConfig;

    /**
     * 获取状态描述
     */
    public String getStatusText() {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "草稿";
            case 1 -> "进行中";
            case 2 -> "已结束";
            case 3 -> "已归档";
            default -> "未知";
        };
    }
}
