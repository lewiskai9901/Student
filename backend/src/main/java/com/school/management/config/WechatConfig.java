package com.school.management.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * 微信配置
 *
 * @author system
 * @since 4.3.0
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "wechat")
public class WechatConfig {

    /**
     * 微信公众号AppID
     */
    private String appId;

    /**
     * 微信公众号AppSecret
     */
    private String appSecret;

    /**
     * 启动时验证微信配置
     */
    @PostConstruct
    public void validateConfig() {
        boolean hasAppId = StringUtils.hasText(appId);
        boolean hasAppSecret = StringUtils.hasText(appSecret);

        // 检查配置完整性
        if (hasAppId != hasAppSecret) {
            log.warn("【配置警告】微信公众号AppID和AppSecret必须同时配置或同时为空");
        }

        // 检查Secret格式（微信AppSecret固定32位）
        if (hasAppSecret && appSecret.length() != 32) {
            log.warn("【配置警告】微信AppSecret长度不正确，应为32位");
        }

        // 检查是否为占位符（常见错误）
        if (hasAppSecret && (appSecret.contains("your_") || appSecret.contains("xxx"))) {
            log.error("【安全错误】检测到微信AppSecret可能是占位符，请配置真实密钥");
        }

        // 模板消息配置验证
        if (template != null && template.isEnabled()) {
            if (!hasAppId || !hasAppSecret) {
                log.error("【配置错误】启用了微信模板消息但未配置AppID/AppSecret");
            }
            if (!StringUtils.hasText(template.getAnnouncementTemplateId())
                    && !StringUtils.hasText(template.getNotificationTemplateId())) {
                log.warn("【配置警告】启用了微信模板消息但未配置任何模板ID");
            }
        }

        if (hasAppId && hasAppSecret) {
            log.info("微信公众号配置已加载: appId={}", maskSecret(appId));
        } else {
            log.info("微信公众号功能未配置，相关功能将被禁用");
        }
    }

    /**
     * 遮蔽敏感信息用于日志显示
     */
    private String maskSecret(String secret) {
        if (secret == null || secret.length() < 8) {
            return "***";
        }
        return secret.substring(0, 4) + "****" + secret.substring(secret.length() - 4);
    }

    /**
     * 检查微信功能是否可用
     */
    public boolean isWechatEnabled() {
        return StringUtils.hasText(appId) && StringUtils.hasText(appSecret);
    }

    /**
     * 分享页面基础URL
     */
    private String shareBaseUrl;

    /**
     * 模板消息配置
     */
    private TemplateConfig template = new TemplateConfig();

    /**
     * 模板消息配置
     */
    @Data
    public static class TemplateConfig {
        /**
         * 公告推送模板ID
         */
        private String announcementTemplateId;

        /**
         * 通报推送模板ID
         */
        private String notificationTemplateId;

        /**
         * 是否启用推送
         */
        private boolean enabled = false;
    }
}
