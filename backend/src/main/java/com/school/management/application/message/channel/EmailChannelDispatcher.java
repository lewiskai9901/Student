package com.school.management.application.message.channel;

import com.school.management.domain.message.model.MsgNotification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * S-2: 邮件通道分发器.
 *
 * 通过 Spring Mail (JavaMailSender) 发送站内消息到用户邮箱.
 * 配置: application.yml `spring.mail.*` (host/port/username/password).
 *      启用: `inspection.message.email.enabled=true`
 *
 * 失败重试由 MessageDispatcher 根据 DeliveryResult.retryable 决定.
 *
 * 当前简单实现 — 复杂场景 (HTML/附件/多收件人) 待后续增强.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "inspection.message.email.enabled", havingValue = "true", matchIfMissing = false)
public class EmailChannelDispatcher implements ChannelDispatcher {

    private final JavaMailSender mailSender;
    private final JdbcTemplate jdbc;
    private final String fromAddress;

    @Autowired
    public EmailChannelDispatcher(JavaMailSender mailSender, JdbcTemplate jdbc,
                                   @Value("${spring.mail.username:noreply@example.com}") String fromAddress) {
        this.mailSender = mailSender;
        this.jdbc = jdbc;
        this.fromAddress = fromAddress;
    }

    @Override
    public String channelCode() { return "EMAIL"; }

    @Override
    public String displayName() { return "邮件"; }

    @Override
    public DeliveryResult deliver(MsgNotification notification) {
        // EMAIL 通道目前只支持 USER 类型接收人 (从 users.email 字段取邮箱)
        if (!"USER".equals(notification.getReceiverType())) {
            return DeliveryResult.failed("EMAIL 通道暂不支持非 USER 接收人: " + notification.getReceiverType(), false);
        }
        Long userId = notification.getReceiverId();
        if (userId == null) {
            return DeliveryResult.failed("接收人 ID 为空", false);
        }
        // 1. 查邮箱
        String email;
        try {
            email = jdbc.queryForObject(
                    "SELECT email FROM users WHERE id = ? AND deleted = 0", String.class, userId);
        } catch (Exception e) {
            return DeliveryResult.failed("查询用户邮箱失败: " + e.getMessage(), false);
        }
        if (email == null || email.isBlank()) {
            return DeliveryResult.failed("用户未配置邮箱", false);
        }
        // 2. 发送
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(email);
            msg.setSubject(notification.getTitle());
            msg.setText(notification.getContent());
            mailSender.send(msg);
            log.info("[email] 已发送, userId={}, email={}, title={}", userId, email, notification.getTitle());
            return DeliveryResult.ok();
        } catch (MailException e) {
            log.warn("[email] 发送失败, userId={}, email={}, err={}", userId, email, e.getMessage());
            return DeliveryResult.failed(e.getMessage(), true);
        } catch (Exception e) {
            return DeliveryResult.failed("邮件发送异常: " + e.getMessage(), true);
        }
    }
}
