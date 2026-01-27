package com.school.management.infrastructure.persistence.asset;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 资产持久化对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("asset")
public class AssetPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String assetCode;

    private String assetName;

    private Long categoryId;

    private String brand;

    private String model;

    private String unit;

    private Integer quantity;

    private BigDecimal originalValue;

    private BigDecimal netValue;

    private LocalDate purchaseDate;

    private LocalDate warrantyDate;

    private String supplier;

    private Integer status;

    private Integer managementMode;

    private String locationType;

    private Long locationId;

    private String locationName;

    private Long responsibleUserId;

    private String responsibleUserName;

    private String remark;

    // 折旧相关
    private Integer categoryType;
    private Integer depreciationMethod;
    private BigDecimal residualValue;
    private BigDecimal accumulatedDepreciation;
    private Integer usefulLife;
    private Integer stockWarningThreshold;

    private Long createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;

    // 非持久化字段,用于关联查询
    @TableField(exist = false)
    private String categoryName;

    @TableField(exist = false)
    private String categoryCode;
}
