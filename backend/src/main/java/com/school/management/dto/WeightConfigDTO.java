package com.school.management.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 加权配置DTO
 *
 * @author system
 * @version 3.0.0
 * @since 2024-12-29
 */
@Data
public class WeightConfigDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 配置ID
     */
    private Long id;

    /**
     * 配置名称
     */
    private String configName;

    /**
     * 配置编码
     */
    private String configCode;

    /**
     * 加权模式(STANDARD=标准人数,PER_CAPITA=人均,SEGMENT=分段,NONE=不加权)
     */
    private String weightMode;

    /**
     * 加权模式描述
     */
    private String weightModeDesc;

    /**
     * 是否启用加权(配置级开关)
     */
    private Boolean enableWeight;

    /**
     * [已废弃] 是否使用固定标准人数
     * 注意: 标准人数来源已统一由 class_size_standards 表的 standardMode 字段控制
     * 此字段保留仅为兼容旧数据,新版本不再使用
     */
    @Deprecated
    private Boolean useFixedStandard;

    /**
     * 最小权重(如0.5)
     */
    private BigDecimal minWeight;

    /**
     * 最大权重(如2.0)
     */
    private BigDecimal maxWeight;

    /**
     * 是否启用权重限制
     */
    private Boolean enableWeightLimit;

    /**
     * 分段规则(JSON格式,当weightMode=SEGMENT时使用)
     */
    private String segmentRules;

    /**
     * 应用范围(ALL=全部,DEPT=部门,GRADE=年级,CLASS=班级)
     */
    private String applyScope;

    /**
     * 应用范围ID
     */
    private Long applyScopeId;

    /**
     * 应用范围名称
     */
    private String applyScopeName;

    /**
     * 生效日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private java.time.LocalDate effectiveDate;

    /**
     * 失效日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private java.time.LocalDate expiryDate;

    /**
     * 状态(1=启用,0=禁用)
     */
    private Integer status;

    /**
     * 状态描述
     */
    private String statusDesc;

    /**
     * 描述
     */
    private String description;

    /**
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 创建人姓名
     */
    private String createdByName;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * 获取加权模式描述
     */
    public String getWeightModeDesc() {
        if (weightMode == null) {
            return "未知";
        }
        switch (weightMode) {
            case "STANDARD":
                return "标准人数加权";
            case "PER_CAPITA":
                return "人均加权";
            case "SEGMENT":
                return "分段加权";
            case "NONE":
                return "不加权";
            default:
                return "未知";
        }
    }

    /**
     * 获取状态描述
     */
    public String getStatusDesc() {
        if (status == null) {
            return "未知";
        }
        return status == 1 ? "启用" : "禁用";
    }

    /**
     * 是否在有效期内
     */
    public Boolean isEffective() {
        java.time.LocalDate now = java.time.LocalDate.now();
        if (effectiveDate != null && now.isBefore(effectiveDate)) {
            return false;
        }
        if (expiryDate != null && now.isAfter(expiryDate)) {
            return false;
        }
        return status != null && status == 1;
    }
}
