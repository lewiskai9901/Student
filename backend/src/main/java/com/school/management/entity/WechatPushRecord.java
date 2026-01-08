package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 微信推送记录实体
 *
 * @author system
 * @since 4.5.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wechat_push_record")
public class WechatPushRecord extends BaseEntity {

    /**
     * 业务类型枚举
     */
    public enum BusinessType {
        ANNOUNCEMENT,   // 公告
        NOTIFICATION    // 通报
    }

    /**
     * 推送状态枚举
     */
    public enum PushStatus {
        PENDING,    // 待发送
        SUCCESS,    // 成功
        FAILED      // 失败
    }

    /**
     * 业务类型: ANNOUNCEMENT-公告, NOTIFICATION-通报
     */
    private String businessType;

    /**
     * 业务ID
     */
    private Long businessId;

    /**
     * 目标用户ID
     */
    private Long userId;

    /**
     * 微信openid
     */
    private String openid;

    /**
     * 模板ID
     */
    private String templateId;

    /**
     * 发送内容JSON
     */
    private String content;

    /**
     * 状态: PENDING-待发送, SUCCESS-成功, FAILED-失败
     */
    private String status;

    /**
     * 错误码
     */
    private String errorCode;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 微信返回的消息ID
     */
    private String msgId;

    /**
     * 发送时间
     */
    private LocalDateTime sendTime;

    /**
     * 创建推送记录
     */
    public static WechatPushRecord create(
            BusinessType businessType,
            Long businessId,
            Long userId,
            String openid,
            String templateId,
            String content) {

        WechatPushRecord record = new WechatPushRecord();
        record.setBusinessType(businessType.name());
        record.setBusinessId(businessId);
        record.setUserId(userId);
        record.setOpenid(openid);
        record.setTemplateId(templateId);
        record.setContent(content);
        record.setStatus(PushStatus.PENDING.name());
        return record;
    }

    /**
     * 标记发送成功
     */
    public void markSuccess(String msgId) {
        this.status = PushStatus.SUCCESS.name();
        this.msgId = msgId;
        this.sendTime = LocalDateTime.now();
    }

    /**
     * 标记发送失败
     */
    public void markFailed(String errorCode, String errorMsg) {
        this.status = PushStatus.FAILED.name();
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
        this.sendTime = LocalDateTime.now();
    }
}
