package com.school.management.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.dto.wechat.TemplateMessageDTO;
import com.school.management.exception.BusinessException;
import com.school.management.service.WechatConfigService;
import com.school.management.service.WechatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 微信服务实现
 *
 * @author system
 * @since 4.3.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatServiceImpl implements WechatService {

    private final WechatConfigService wechatConfigService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String ACCESS_TOKEN_KEY = "wechat:access_token";
    private static final String JSAPI_TICKET_KEY = "wechat:jsapi_ticket";
    private static final String TEMPLATE_MESSAGE_URL = "https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=%s";

    @Override
    public Map<String, Object> getJsConfig(String url) {
        Map<String, Object> result = new HashMap<>();

        String appId = wechatConfigService.getAppId();
        if (!StringUtils.hasText(appId)) {
            log.warn("微信AppID未配置");
            result.put("error", "微信功能未配置");
            return result;
        }

        try {
            String jsapiTicket = getJsapiTicket();
            String nonceStr = UUID.randomUUID().toString().replace("-", "");
            long timestamp = System.currentTimeMillis() / 1000;

            // 生成签名
            String signature = generateSignature(jsapiTicket, nonceStr, timestamp, url);

            result.put("appId", appId);
            result.put("timestamp", timestamp);
            result.put("nonceStr", nonceStr);
            result.put("signature", signature);

            log.info("生成微信JS-SDK配置成功: url={}", url);
        } catch (Exception e) {
            log.error("生成微信JS-SDK配置失败", e);
            result.put("error", "获取配置失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    public String getShareUrl(Long notificationId) {
        String baseUrl = wechatConfigService.getShareBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            baseUrl = "https://example.com";
        }
        return baseUrl + "/notification/view/" + notificationId;
    }

    @Override
    public String getAccessToken() {
        // 先从Redis获取
        String accessToken = redisTemplate.opsForValue().get(ACCESS_TOKEN_KEY);
        if (StringUtils.hasText(accessToken)) {
            return accessToken;
        }

        // 调用微信API获取
        String appId = wechatConfigService.getAppId();
        String appSecret = wechatConfigService.getAppSecret();

        if (!StringUtils.hasText(appId) || !StringUtils.hasText(appSecret)) {
            throw new BusinessException("微信配置不完整，请联系管理员");
        }

        String url = String.format(
                "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s",
                appId, appSecret
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);

        if (response != null && response.containsKey("access_token")) {
            accessToken = (String) response.get("access_token");
            Integer expiresIn = (Integer) response.get("expires_in");

            // 缓存到Redis，提前10分钟过期
            redisTemplate.opsForValue().set(ACCESS_TOKEN_KEY, accessToken,
                    expiresIn - 600, TimeUnit.SECONDS);

            return accessToken;
        }

        log.error("获取access_token失败: {}", response);
        throw new BusinessException("获取微信凭证失败，请稍后重试");
    }

    /**
     * 获取jsapi_ticket
     */
    private String getJsapiTicket() {
        // 先从Redis获取
        String ticket = redisTemplate.opsForValue().get(JSAPI_TICKET_KEY);
        if (StringUtils.hasText(ticket)) {
            return ticket;
        }

        // 调用微信API获取
        String accessToken = getAccessToken();
        String url = String.format(
                "https://api.weixin.qq.com/cgi-bin/ticket/getticket?access_token=%s&type=jsapi",
                accessToken
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);

        if (response != null && Integer.valueOf(0).equals(response.get("errcode"))) {
            ticket = (String) response.get("ticket");
            Integer expiresIn = (Integer) response.get("expires_in");

            // 缓存到Redis，提前10分钟过期
            redisTemplate.opsForValue().set(JSAPI_TICKET_KEY, ticket,
                    expiresIn - 600, TimeUnit.SECONDS);

            return ticket;
        }

        log.error("获取jsapi_ticket失败: {}", response);
        throw new BusinessException("获取微信凭证失败，请稍后重试");
    }

    /**
     * 生成JS-SDK签名
     */
    private String generateSignature(String jsapiTicket, String nonceStr, long timestamp, String url) {
        String str = String.format("jsapi_ticket=%s&noncestr=%s&timestamp=%d&url=%s",
                jsapiTicket, nonceStr, timestamp, url);

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(str.getBytes());
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-1算法不可用", e);
            throw new BusinessException("签名生成失败");
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @Override
    public Map<String, Object> sendTemplateMessage(TemplateMessageDTO message) {
        Map<String, Object> result = new HashMap<>();

        if (!isConfigured()) {
            result.put("errcode", -1);
            result.put("errmsg", "微信配置不完整");
            log.warn("发送模板消息失败: 微信配置不完整");
            return result;
        }

        try {
            String accessToken = getAccessToken();
            String url = String.format(TEMPLATE_MESSAGE_URL, accessToken);

            // 构建请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String jsonBody = objectMapper.writeValueAsString(message);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);

            if (response != null) {
                Integer errcode = (Integer) response.get("errcode");
                if (errcode != null && errcode == 0) {
                    log.info("发送模板消息成功: openid={}, msgid={}", message.getTouser(), response.get("msgid"));
                } else {
                    log.warn("发送模板消息失败: openid={}, errcode={}, errmsg={}",
                            message.getTouser(), errcode, response.get("errmsg"));
                }
                return response;
            }

            result.put("errcode", -1);
            result.put("errmsg", "响应为空");
            return result;

        } catch (Exception e) {
            log.error("发送模板消息异常: openid={}", message.getTouser(), e);
            result.put("errcode", -1);
            result.put("errmsg", "发送异常: " + e.getMessage());
            return result;
        }
    }

    @Override
    public boolean isConfigured() {
        return wechatConfigService.isConfigured();
    }
}
