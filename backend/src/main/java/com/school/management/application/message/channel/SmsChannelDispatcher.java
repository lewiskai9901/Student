package com.school.management.application.message.channel;

import com.school.management.domain.message.model.MsgNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * S+1: SMS 短信通道.
 *
 * 当前为"占位实现" — 接入真实 SMS 厂商 (阿里云/腾讯云/Twilio) 时只需:
 *   1. 替换 doSendSms() 内部为 SDK 调用
 *   2. 将厂商 messageId 写入 MsgNotification.lastError (复用字段记录)
 *
 * 启用: inspection.message.sms.enabled=true + inspection.message.sms.signature 配置短信签名.
 *
 * 当前 doSendSms 仅记日志, 不真发 — 上线前确认厂商凭证后再切换.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "inspection.message.sms.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
public class SmsChannelDispatcher implements ChannelDispatcher {

    private final JdbcTemplate jdbc;

    @Value("${inspection.message.sms.signature:}")
    private String signature;

    @Value("${inspection.message.sms.dry-run:true}")
    private boolean dryRun;

    @Override
    public String channelCode() { return "SMS"; }

    @Override
    public String displayName() { return "短信"; }

    @Override
    public DeliveryResult deliver(MsgNotification notification) {
        if (!"USER".equals(notification.getReceiverType())) {
            return DeliveryResult.failed("SMS 通道暂不支持非 USER 接收人", false);
        }
        Long userId = notification.getReceiverId();
        if (userId == null) return DeliveryResult.failed("接收人 ID 为空", false);

        String phone;
        try {
            phone = jdbc.queryForObject(
                    "SELECT phone FROM users WHERE id = ? AND deleted = 0", String.class, userId);
        } catch (Exception e) {
            return DeliveryResult.failed("查询用户手机号失败: " + e.getMessage(), false);
        }
        if (phone == null || phone.isBlank()) {
            return DeliveryResult.failed("用户未配置手机号", false);
        }

        try {
            doSendSms(phone, notification.getTitle(), notification.getContent());
            return DeliveryResult.ok();
        } catch (Exception e) {
            log.warn("[sms] 发送失败 userId={}, phone={}, err={}", userId, phone, e.getMessage());
            return DeliveryResult.failed(e.getMessage(), true);
        }
    }

    /**
     * 真实接厂商 SDK 时替换此方法. 当前 dryRun=true 仅记日志.
     */
    private void doSendSms(String phone, String title, String content) {
        if (dryRun) {
            log.info("[sms-dry-run] signature={}, phone={}, title={}, content={}",
                    signature, phone, title, abbreviate(content, 50));
            return;
        }
        // TODO: 接阿里云 dysms / 腾讯云 sms / Twilio 等
        throw new UnsupportedOperationException("SMS 厂商 SDK 未接入, 当前仅 dryRun 模式可用");
    }

    private static String abbreviate(String s, int max) {
        if (s == null || s.length() <= max) return s;
        return s.substring(0, max) + "...";
    }
}
