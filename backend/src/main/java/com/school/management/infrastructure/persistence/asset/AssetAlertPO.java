package com.school.management.infrastructure.persistence.asset;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资产预警持久化对象
 */
@Data
@TableName("asset_alert")
public class AssetAlertPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer alertType;

    private Long assetId;

    private String assetCode;

    private String assetName;

    private Long borrowId;

    private String alertContent;

    private Integer alertLevel;

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

    private LocalDateTime updatedAt;
}
