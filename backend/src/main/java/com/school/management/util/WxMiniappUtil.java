package com.school.management.util;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.school.management.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 微信小程序工具类
 *
 * @author system
 * @since 1.0.0
 */
@Slf4j
@Component
public class WxMiniappUtil {

    @Value("${wechat.miniapp.appid:}")
    private String appId;

    @Value("${wechat.miniapp.secret:}")
    private String appSecret;

    private static final String WX_LOGIN_URL = "https://api.weixin.qq.com/sns/jscode2session";

    /**
     * 微信登录会话信息
     */
    public static class WxSession {
        private String openId;
        private String sessionKey;
        private String unionId;

        public String getOpenId() {
            return openId;
        }

        public void setOpenId(String openId) {
            this.openId = openId;
        }

        public String getSessionKey() {
            return sessionKey;
        }

        public void setSessionKey(String sessionKey) {
            this.sessionKey = sessionKey;
        }

        public String getUnionId() {
            return unionId;
        }

        public void setUnionId(String unionId) {
            this.unionId = unionId;
        }
    }

    /**
     * 通过微信code获取用户会话信息
     *
     * @param code 微信登录临时凭证
     * @return 微信会话信息
     */
    public WxSession getSessionByCode(String code) {
        if (appId == null || appId.isEmpty() || appSecret == null || appSecret.isEmpty()) {
            log.warn("微信小程序配置未设置，使用模拟模式");
            // 开发模式下模拟返回
            return mockSession(code);
        }

        String url = String.format("%s?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                WX_LOGIN_URL, appId, appSecret, code);

        try {
            String result = HttpUtil.get(url, 5000);
            log.debug("微信登录API响应: {}", result);

            JSONObject json = JSONUtil.parseObj(result);

            // 检查错误
            Integer errcode = json.getInt("errcode");
            if (errcode != null && errcode != 0) {
                String errmsg = json.getStr("errmsg");
                log.error("微信登录失败: errcode={}, errmsg={}", errcode, errmsg);
                throw new BusinessException("微信登录失败: " + errmsg);
            }

            WxSession session = new WxSession();
            session.setOpenId(json.getStr("openid"));
            session.setSessionKey(json.getStr("session_key"));
            session.setUnionId(json.getStr("unionid"));

            if (session.getOpenId() == null || session.getOpenId().isEmpty()) {
                throw new BusinessException("获取微信OpenID失败");
            }

            return session;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用微信登录API异常: {}", e.getMessage(), e);
            throw new BusinessException("微信登录服务异常，请稍后重试");
        }
    }

    /**
     * 开发模式下模拟返回会话信息
     */
    private WxSession mockSession(String code) {
        WxSession session = new WxSession();
        // 使用code生成模拟openid，便于开发测试
        session.setOpenId("mock_openid_" + code.hashCode());
        session.setSessionKey("mock_session_key");
        log.info("开发模式: 使用模拟微信会话, openId={}", session.getOpenId());
        return session;
    }

    /**
     * 检查微信配置是否已设置
     */
    public boolean isConfigured() {
        return appId != null && !appId.isEmpty()
            && appSecret != null && !appSecret.isEmpty();
    }
}
