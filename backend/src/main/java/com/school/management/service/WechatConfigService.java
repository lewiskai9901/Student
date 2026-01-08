package com.school.management.service;

import java.util.Map;

/**
 * 微信配置服务接口
 * 支持从数据库动态读取微信配置
 *
 * @author system
 * @since 4.5.0
 */
public interface WechatConfigService {

    /**
     * 获取微信AppID
     */
    String getAppId();

    /**
     * 获取微信AppSecret
     */
    String getAppSecret();

    /**
     * 获取分享基础URL
     */
    String getShareBaseUrl();

    /**
     * 是否启用模板推送
     */
    boolean isTemplateEnabled();

    /**
     * 获取公告模板ID
     */
    String getAnnouncementTemplateId();

    /**
     * 获取通报模板ID
     */
    String getNotificationTemplateId();

    /**
     * 检查微信配置是否完整
     */
    boolean isConfigured();

    /**
     * 获取所有微信配置
     */
    Map<String, String> getAllConfigs();

    /**
     * 更新微信配置
     */
    boolean updateConfigs(Map<String, String> configs);

    /**
     * 刷新配置缓存
     */
    void refreshCache();
}
