package com.school.management.infrastructure.external.impl;

import com.school.management.infrastructure.external.NotificationService;
import com.school.management.infrastructure.external.SystemMessageDomainMapper;
import com.school.management.infrastructure.external.SystemMessagePO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 通知服务实现 —— 站内消息。
 *
 * <p>原 sendWechatTemplate (空壳) / sendSms (硬编码 return true 假发送) 已删 (2026-06-27 死代码清理:
 * 全仓零调用方; 真实通知走 application/message 通道, 短信/微信通道在那里以 flag+dryRun 防线管理)。
 */
@Slf4j
@Service("dddNotificationService")
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final SystemMessageDomainMapper systemMessageDomainMapper;

    @Override
    @Async
    public void sendInAppMessage(Long userId, String title, String content, String type) {
        try {
            SystemMessagePO message = new SystemMessagePO();
            message.setReceiverId(userId);
            message.setTitle(title);
            message.setContent(content);
            message.setMessageType(type);
            message.setIsRead(0);  // 0-未读, 1-已读

            systemMessageDomainMapper.insert(message);
            log.debug("Sent in-app message to user {}: {}", userId, title);
        } catch (Exception e) {
            log.error("Failed to send in-app message to user {}", userId, e);
        }
    }

    @Override
    @Async
    public void sendInAppMessageBatch(List<Long> userIds, String title, String content, String type) {
        for (Long userId : userIds) {
            sendInAppMessage(userId, title, content, type);
        }
    }
}
