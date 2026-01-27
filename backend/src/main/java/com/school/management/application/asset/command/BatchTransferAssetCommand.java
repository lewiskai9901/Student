package com.school.management.application.asset.command;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量调拨命令
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchTransferAssetCommand {

    /**
     * 要调拨的资产ID列表
     */
    @NotEmpty(message = "请选择要调拨的资产")
    private List<Long> assetIds;

    /**
     * 目标位置类型
     */
    @NotNull(message = "请选择目标位置类型")
    private String locationType;

    /**
     * 目标位置ID
     */
    @NotNull(message = "请选择目标位置")
    private Long locationId;

    /**
     * 目标位置名称
     */
    private String locationName;

    /**
     * 新责任人ID (可选)
     */
    private Long responsibleUserId;

    /**
     * 新责任人姓名 (可选)
     */
    private String responsibleUserName;

    /**
     * 调拨原因/备注
     */
    private String remark;

    // 操作人信息
    private Long operatorId;
    private String operatorName;
}
