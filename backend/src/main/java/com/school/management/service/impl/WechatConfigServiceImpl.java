package com.school.management.service.impl;

import com.school.management.config.WechatConfig;
import com.school.management.service.SystemConfigService;
import com.school.management.service.WechatConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 微信配置服务实现
 * 优先从数据库读取配置，如果数据库没有则使用yml配置
 *
 * @author system
 * @since 4.5.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatConfigServiceImpl implements WechatConfigService {

    private final SystemConfigService systemConfigService;
    private final WechatConfig wechatConfig; // yml配置作为fallback

    // 配置键常量
    private static final String KEY_APP_ID = "wechat.app_id";
    private static final String KEY_APP_SECRET = "wechat.app_secret";
    private static final String KEY_SHARE_BASE_URL = "wechat.share_base_url";
    private static final String KEY_TEMPLATE_ENABLED = "wechat.template.enabled";
    private static final String KEY_ANNOUNCEMENT_TEMPLATE_ID = "wechat.template.announcement_id";
    private static final String KEY_NOTIFICATION_TEMPLATE_ID = "wechat.template.notification_id";

    @Override
    public String getAppId() {
        String value = systemConfigService.getConfigValue(KEY_APP_ID);
        if (StringUtils.hasText(value)) {
            return value;
        }
        return wechatConfig.getAppId();
    }

    @Override
    public String getAppSecret() {
        String value = systemConfigService.getConfigValue(KEY_APP_SECRET);
        if (StringUtils.hasText(value)) {
            return value;
        }
        return wechatConfig.getAppSecret();
    }

    @Override
    public String getShareBaseUrl() {
        String value = systemConfigService.getConfigValue(KEY_SHARE_BASE_URL);
        if (StringUtils.hasText(value)) {
            return value;
        }
        return wechatConfig.getShareBaseUrl();
    }

    @Override
    public boolean isTemplateEnabled() {
        String value = systemConfigService.getConfigValue(KEY_TEMPLATE_ENABLED);
        if (StringUtils.hasText(value)) {
            return "true".equalsIgnoreCase(value) || "1".equals(value);
        }
        return wechatConfig.getTemplate() != null && wechatConfig.getTemplate().isEnabled();
    }

    @Override
    public String getAnnouncementTemplateId() {
        String value = systemConfigService.getConfigValue(KEY_ANNOUNCEMENT_TEMPLATE_ID);
        if (StringUtils.hasText(value)) {
            return value;
        }
        return wechatConfig.getTemplate() != null ? wechatConfig.getTemplate().getAnnouncementTemplateId() : null;
    }

    @Override
    public String getNotificationTemplateId() {
        String value = systemConfigService.getConfigValue(KEY_NOTIFICATION_TEMPLATE_ID);
        if (StringUtils.hasText(value)) {
            return value;
        }
        return wechatConfig.getTemplate() != null ? wechatConfig.getTemplate().getNotificationTemplateId() : null;
    }

    @Override
    public boolean isConfigured() {
        return StringUtils.hasText(getAppId()) && StringUtils.hasText(getAppSecret());
    }

    @Override
    public Map<String, String> getAllConfigs() {
        Map<String, String> configs = new HashMap<>();
        configs.put("appId", maskSecret(getAppId()));
        configs.put("appSecret", maskSecret(getAppSecret()));
        configs.put("shareBaseUrl", getShareBaseUrl());
        configs.put("templateEnabled", String.valueOf(isTemplateEnabled()));
        configs.put("announcementTemplateId", getAnnouncementTemplateId());
        configs.put("notificationTemplateId", getNotificationTemplateId());
        configs.put("configured", String.valueOf(isConfigured()));
        return configs;
    }

    @Override
    public boolean updateConfigs(Map<String, String> configs) {
        try {
            Map<String, String> dbConfigs = new HashMap<>();

            if (configs.containsKey("appId")) {
                dbConfigs.put(KEY_APP_ID, configs.get("appId"));
            }
            if (configs.containsKey("appSecret") && !configs.get("appSecret").contains("*")) {
                // 只有当不是掩码值时才更新
                dbConfigs.put(KEY_APP_SECRET, configs.get("appSecret"));
            }
            if (configs.containsKey("shareBaseUrl")) {
                dbConfigs.put(KEY_SHARE_BASE_URL, configs.get("shareBaseUrl"));
            }
            if (configs.containsKey("templateEnabled")) {
                dbConfigs.put(KEY_TEMPLATE_ENABLED, configs.get("templateEnabled"));
            }
            if (configs.containsKey("announcementTemplateId")) {
                dbConfigs.put(KEY_ANNOUNCEMENT_TEMPLATE_ID, configs.get("announcementTemplateId"));
            }
            if (configs.containsKey("notificationTemplateId")) {
                dbConfigs.put(KEY_NOTIFICATION_TEMPLATE_ID, configs.get("notificationTemplateId"));
            }

            if (!dbConfigs.isEmpty()) {
                boolean result = systemConfigService.batchUpdate(dbConfigs);
                if (result) {
                    refreshCache();
                    log.info("微信配置更新成功");
                }
                return result;
            }
            return true;
        } catch (Exception e) {
            log.error("更新微信配置失败", e);
            return false;
        }
    }

    @Override
    public void refreshCache() {
        systemConfigService.refreshCache();
    }

    /**
     * 掩码敏感信息
     */
    private String maskSecret(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        if (value.length() <= 8) {
            return "****";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }
}
