package com.school.management.application.asset.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 创建资产命令
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAssetCommand {

    @NotBlank(message = "资产名称不能为空")
    private String assetName;

    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    private String brand;

    private String model;

    @NotBlank(message = "计量单位不能为空")
    private String unit;

    private Integer quantity;

    // 管理模式: 1-单品管理, 2-批量管理
    // 如果不指定，则使用分类的默认管理模式
    private Integer managementMode;

    private BigDecimal originalValue;

    private BigDecimal netValue;

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
