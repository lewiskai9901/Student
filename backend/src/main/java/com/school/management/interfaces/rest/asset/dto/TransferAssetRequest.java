package com.school.management.interfaces.rest.asset.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 调拨资产请求
 */
@Data
public class TransferAssetRequest {

    @NotBlank(message = "目标位置类型不能为空")
    private String locationType;

    @NotNull(message = "目标位置ID不能为空")
    private Long locationId;

    @NotBlank(message = "目标位置名称不能为空")
    private String locationName;

    private Long responsibleUserId;

    private String responsibleUserName;

    private String remark;
}
