package com.school.management.application.asset.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 调拨资产命令
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferAssetCommand {

    @NotNull(message = "资产ID不能为空")
    private Long assetId;

    @NotBlank(message = "目标位置类型不能为空")
    private String locationType;

    @NotNull(message = "目标位置ID不能为空")
    private Long locationId;

    @NotBlank(message = "目标位置名称不能为空")
    private String locationName;

    // 新责任人
    private Long responsibleUserId;
    private String responsibleUserName;

    private String remark;

    // 操作人
    @NotNull(message = "操作人ID不能为空")
    private Long operatorId;

    private String operatorName;
}
