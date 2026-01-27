package com.school.management.interfaces.rest.asset.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 创建资产请求
 */
@Data
public class CreateAssetRequest {

    @NotBlank(message = "资产名称不能为空")
    private String assetName;

    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    private String brand;

    private String model;

    @NotBlank(message = "计量单位不能为空")
    private String unit;

    private Integer quantity;

    private BigDecimal originalValue;

    private BigDecimal netValue;

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
