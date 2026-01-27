package com.school.management.application.asset.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 资产预警DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetAlertDTO {

    private Long id;

    private Integer alertType;

    private String alertTypeDesc;

    private Long assetId;

    private String assetCode;

    private String assetName;

    private Long borrowId;

    private String alertContent;

    private Integer alertLevel;

    private String alertLevelDesc;

    private Boolean isRead;

    private Boolean isHandled;

    private String handleRemark;

    private LocalDateTime handleTime;

    private Long handlerId;

    private String handlerName;

    private Long notifyUserId;

    private String notifyUserName;

    private LocalDateTime alertTime;

    private LocalDateTime expireTime;

    private LocalDateTime createdAt;
}
