package com.school.management.interfaces.rest.asset.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 批量入库请求
 */
@Data
public class BatchCreateAssetRequest {

    @NotBlank(message = "资产名称不能为空")
    private String assetName;

    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    private String brand;

    private String model;

    @NotBlank(message = "计量单位不能为空")
    private String unit;

    /**
     * 入库数量 - 批量入库核心字段
     */
    @NotNull(message = "入库数量不能为空")
    @Min(value = 1, message = "入库数量至少为1")
    @Max(value = 1000, message = "单次最多入库1000件")
    private Integer quantity;

    /**
     * 单价
     */
    private BigDecimal originalValue;

    private LocalDate purchaseDate;

    private LocalDate warrantyDate;

    private String supplier;

    private String locationType;

    private Long locationId;

    private String locationName;

    private Long responsibleUserId;

    private String responsibleUserName;

    private String remark;
}
