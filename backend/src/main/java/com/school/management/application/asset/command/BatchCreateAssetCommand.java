package com.school.management.application.asset.command;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 批量入库命令
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchCreateAssetCommand {

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

    // 位置信息
    private String locationType;
    private Long locationId;
    private String locationName;

    // 责任人
    private Long responsibleUserId;
    private String responsibleUserName;

    private String remark;

    // 操作人
    private Long operatorId;
    private String operatorName;
}
