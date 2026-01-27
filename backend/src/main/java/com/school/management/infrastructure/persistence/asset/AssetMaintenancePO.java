package com.school.management.infrastructure.persistence.asset;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 资产维修保养记录持久化对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("asset_maintenance")
public class AssetMaintenancePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long assetId;

    private Integer maintenanceType;

    private String faultDesc;

    private LocalDate startDate;

    private LocalDate endDate;

    private BigDecimal cost;

    private String maintainer;

    private String result;

    private Integer status;

    private Long createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // 非持久化字段,用于关联查询
    @TableField(exist = false)
    private String assetCode;

    @TableField(exist = false)
    private String assetName;
}
