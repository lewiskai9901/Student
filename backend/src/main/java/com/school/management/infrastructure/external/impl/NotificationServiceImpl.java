package com.school.management.infrastructure.external.impl;

import com.school.management.infrastructure.external.NotificationService;
import com.school.management.infrastructure.external.SystemMessageDomainMapper;
import com.school.management.infrastructure.external.SystemMessagePO;
import com.school.management.infrastructure.persistence.user.UserDomainMapper;
import com.school.management.infrastructure.persistence.user.UserPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 通知服务实现
 * 支持站内消息和短信发送
 *
 * TODO: 微信模板消息功能待DDD迁移后重新集成（原WechatService + TemplateMessageDTO属于V1层）
 */
@Slf4j
@Service("dddNotificationService")
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final SystemMessageDomainMapper systemMessageDomainMapper;
    private final UserDomainMapper userDomainMapper;

    @Value("${sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${sms.api.url:}")
    private String smsApiUrl;

    @Value("${sms.api.key:}")
    private String smsApiKey;

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

    @Override
    @Async
    public void sendWechatTemplate(Long userId, String templateId, Map<String, String> data, String url) {
        try {
            // 获取用户信息检查微信绑定状态
            UserPO user = userDomainMapper.selectById(userId);
            if (user == null || user.getWechatOpenid() == null || user.getWechatOpenid().isEmpty()) {
                log.debug("用户未绑定微信，跳过微信通知: userId={}", userId);
                return;
            }

            // TODO: 微信模板消息发送待DDD迁移后重新集成
            // 原实现依赖V1的 WechatService 和 TemplateMessageDTO
            // 需要在DDD基础设施层创建对应的微信服务接口和实现
            log.info("微信模板消息发送暂未实现（DDD迁移中）: userId={}, templateId={}", userId, templateId);
        } catch (Exception e) {
            log.error("发送微信模板消息异常: userId={}", userId, e);
        }
    }

    @Override
    @Async
    public void sendSms(String phone, String content) {
        try {
            // 检查短信服务是否启用
            if (!smsEnabled) {
                log.debug("短信服务未启用，跳过短信发送: phone={}", phone);
                return;
            }

            // 验证手机号
            if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
                log.warn("无效的手机号，跳过短信发送: phone={}", phone);
                return;
            }

            // 调用短信API发送（这里使用示例实现，实际需要根据短信服务商API调整）
            boolean success = doSendSms(phone, content);

            if (success) {
                log.info("短信发送成功: phone={}", maskPhone(phone));
            } else {
                log.warn("短信发送失败: phone={}", maskPhone(phone));
            }
        } catch (Exception e) {
            log.error("发送短信异常: phone={}", maskPhone(phone), e);
        }
    }

    /**
     * 实际发送短信的逻辑
     * 需要根据实际使用的短信服务商API进行实现
     */
    private boolean doSendSms(String phone, String content) {
        // 示例：使用HTTP调用短信API
        // 实际实现需要根据短信服务商（如阿里云、腾讯云等）的API文档进行开发
        if (smsApiUrl == null || smsApiUrl.isEmpty()) {
            log.warn("短信API URL未配置");
            return false;
        }

        try {
            // 这里应该实现实际的HTTP调用
            // 例如使用RestTemplate或HttpClient调用短信服务商API
            log.info("调用短信API: url={}, phone={}, content={}", smsApiUrl, maskPhone(phone), content);

            // 模拟发送成功
            // 实际实现时应解析API响应判断是否成功
            return true;
        } catch (Exception e) {
            log.error("调用短信API失败", e);
            return false;
        }
    }

    /**
     * 对手机号进行脱敏处理
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
