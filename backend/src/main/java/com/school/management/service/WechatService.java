package com.school.management.service;

import com.school.management.dto.wechat.TemplateMessageDTO;

import java.util.Map;

/**
 * 微信服务接口
 *
 * @author system
 * @since 4.3.0
 */
public interface WechatService {

    /**
     * 获取JS-SDK配置
     *
     * @param url 当前页面URL
     * @return JS-SDK配置（appId, timestamp, nonceStr, signature）
     */
    Map<String, Object> getJsConfig(String url);

    /**
     * 获取分享链接
     *
     * @param notificationId 通报ID
     * @return 分享链接
     */
    String getShareUrl(Long notificationId);

    /**
     * 发送模板消息
     *
     * @param message 模板消息
     * @return 发送结果 (包含 errcode, errmsg, msgid)
     */
    Map<String, Object> sendTemplateMessage(TemplateMessageDTO message);

    /**
     * 获取access_token (公开方法供其他服务使用)
     *
     * @return access_token
     */
    String getAccessToken();

    /**
     * 检查微信配置是否完整
     *
     * @return true-配置完整, false-配置不完整
     */
    boolean isConfigured();
}
