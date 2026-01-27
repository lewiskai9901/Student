package com.school.management.application.asset.command;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 报废资产命令
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScrapAssetCommand {

    @NotNull(message = "资产ID不能为空")
    private Long assetId;

    private String reason;

    // 操作人
    @NotNull(message = "操作人ID不能为空")
    private Long operatorId;

    private String operatorName;
}
