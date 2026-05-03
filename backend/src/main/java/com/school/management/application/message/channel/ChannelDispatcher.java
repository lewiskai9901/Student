package com.school.management.application.message.channel;

import com.school.management.domain.message.model.MsgNotification;

/**
 * S-2: 消息通道分发器 SPI.
 *
 * 每种通道 (站内信 / 邮件 / SMS / 微信 / 钉钉 / Webhook) 实现一个 ChannelDispatcher,
 * Spring 启动时自动收集. MessageDispatcher 按 channel 字段查找对应实现并调用 deliver().
 *
 * 实现需保持幂等 — 同一 notification 重复 deliver 不应产生重复消息 (用 sentAt + retryCount 判断).
 */
public interface ChannelDispatcher {

    /**
     * 通道标识, 与 msg_subscription_rules.channel 字段对应.
     * 如: IN_APP / EMAIL / SMS / WECHAT / WEBHOOK
     */
    String channelCode();

    /** 中文标签, 用于审计日志和 UI 显示 */
    String displayName();

    /**
     * 发送消息.
     *
     * @param notification 已落库的消息记录 (id 已生成)
     * @return DeliveryResult 包含是否成功 / 失败原因 / 是否可重试
     */
    DeliveryResult deliver(MsgNotification notification);

    /** 通道是否可用 (例: EMAIL 没配置 SMTP 时返回 false) */
    default boolean isAvailable() { return true; }

    record DeliveryResult(boolean success, String errorMessage, boolean retryable) {
        public static DeliveryResult ok() {
            return new DeliveryResult(true, null, false);
        }
        public static DeliveryResult failed(String error, boolean retryable) {
            return new DeliveryResult(false, error, retryable);
        }
    }
}
