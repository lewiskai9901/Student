package com.school.management.application.asset.command;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 更新资产命令
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAssetCommand {

    @NotNull(message = "资产ID不能为空")
    private Long id;

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

    // 责任人
    private Long responsibleUserId;
    private String responsibleUserName;

    private String remark;

    // 操作人
    private Long operatorId;
    private String operatorName;
}
