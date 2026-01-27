package com.school.management.domain.asset.model.entity;

import com.school.management.domain.asset.model.valueobject.AlertLevel;
import com.school.management.domain.asset.model.valueobject.AlertType;
import com.school.management.domain.shared.Entity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 资产预警实体
 */
@Getter
@Setter
public class AssetAlert extends Entity<Long> {

    /** 预警类型 */
    private AlertType alertType;

    /** 关联资产ID */
    private Long assetId;

    /** 资产编码 */
    private String assetCode;

    /** 资产名称 */
    private String assetName;

    /** 关联借用记录ID */
    private Long borrowId;

    /** 预警内容 */
    private String alertContent;

    /** 预警级别 */
    private AlertLevel alertLevel;

    /** 是否已读 */
    private boolean isRead;

    /** 是否已处理 */
    private boolean isHandled;

    /** 处理备注 */
    private String handleRemark;

    /** 处理时间 */
    private LocalDateTime handleTime;

    /** 处理人ID */
    private Long handlerId;

    /** 处理人姓名 */
    private String handlerName;

    /** 通知用户ID */
    private Long notifyUserId;

    /** 通知用户姓名 */
    private String notifyUserName;

    /** 预警时间 */
    private LocalDateTime alertTime;

    /** 预警过期时间 */
    private LocalDateTime expireTime;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    public AssetAlert() {
    }

    /**
     * 创建预警
     */
    public static AssetAlert create(
            AlertType alertType,
            Long assetId,
            String assetCode,
            String assetName,
            String alertContent,
            AlertLevel alertLevel
    ) {
        AssetAlert alert = new AssetAlert();
        alert.alertType = alertType;
        alert.assetId = assetId;
        alert.assetCode = assetCode;
        alert.assetName = assetName;
        alert.alertContent = alertContent;
        alert.alertLevel = alertLevel;
        alert.isRead = false;
        alert.isHandled = false;
        alert.alertTime = LocalDateTime.now();
        return alert;
    }

    /**
     * 创建借用相关预警
     */
    public static AssetAlert createBorrowAlert(
            AlertType alertType,
            Long assetId,
            String assetCode,
            String assetName,
            Long borrowId,
            String alertContent,
            AlertLevel alertLevel,
            Long notifyUserId,
            String notifyUserName
    ) {
        AssetAlert alert = create(alertType, assetId, assetCode, assetName, alertContent, alertLevel);
        alert.borrowId = borrowId;
        alert.notifyUserId = notifyUserId;
        alert.notifyUserName = notifyUserName;
        return alert;
    }

    /**
     * 标记为已读
     */
    public void markAsRead() {
        this.isRead = true;
    }

    /**
     * 处理预警
     */
    public void handle(Long handlerId, String handlerName, String remark) {
        this.isHandled = true;
        this.handlerId = handlerId;
        this.handlerName = handlerName;
        this.handleRemark = remark;
        this.handleTime = LocalDateTime.now();
    }
}
