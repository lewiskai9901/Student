package com.school.management.infrastructure.external.impl;

import com.school.management.dto.wechat.TemplateMessageDTO;
import com.school.management.entity.task.SystemMessage;
import com.school.management.entity.User;
import com.school.management.infrastructure.external.NotificationService;
import com.school.management.mapper.task.SystemMessageMapper;
import com.school.management.mapper.UserMapper;
import com.school.management.service.WechatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知服务实现
 * 支持站内消息、微信模板消息和短信发送
 */
@Slf4j
@Service("dddNotificationService")
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final SystemMessageMapper systemMessageMapper;
    private final UserMapper userMapper;

    @Autowired(required = false)
    private WechatService wechatService;

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
            SystemMessage message = new SystemMessage();
            message.setReceiverId(userId);
            message.setTitle(title);
            message.setContent(content);
            message.setMessageType(type);
            message.setIsRead(0);  // 0-未读, 1-已读

            systemMessageMapper.insert(message);
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
            // 检查微信服务是否可用
            if (wechatService == null || !wechatService.isConfigured()) {
                log.debug("微信服务未配置，跳过微信通知: userId={}", userId);
                return;
            }

            // 获取用户的OpenID
            User user = userMapper.selectById(userId);
            if (user == null || user.getWechatOpenid() == null || user.getWechatOpenid().isEmpty()) {
                log.debug("用户未绑定微信，跳过微信通知: userId={}", userId);
                return;
            }

            // 构建模板消息
            Map<String, TemplateMessageDTO.TemplateData> templateData = new HashMap<>();
            if (data != null) {
                data.forEach((key, value) ->
                        templateData.put(key, new TemplateMessageDTO.TemplateData(value)));
            }

            TemplateMessageDTO message = TemplateMessageDTO.builder()
                    .touser(user.getWechatOpenid())
                    .template_id(templateId)
                    .url(url)
                    .data(templateData)
                    .build();

            // 发送模板消息
            Map<String, Object> result = wechatService.sendTemplateMessage(message);
            Integer errcode = (Integer) result.get("errcode");

            if (errcode != null && errcode == 0) {
                log.info("微信模板消息发送成功: userId={}, templateId={}", userId, templateId);
            } else {
                log.warn("微信模板消息发送失败: userId={}, templateId={}, result={}",
                        userId, templateId, result);
            }
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
