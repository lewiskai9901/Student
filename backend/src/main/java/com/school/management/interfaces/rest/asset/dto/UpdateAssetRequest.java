package com.school.management.interfaces.rest.asset.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 更新资产请求
 */
@Data
public class UpdateAssetRequest {

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

    private Long responsibleUserId;

    private String responsibleUserName;

    private String remark;
}
