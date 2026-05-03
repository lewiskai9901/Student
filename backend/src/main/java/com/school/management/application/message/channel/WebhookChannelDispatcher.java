package com.school.management.application.message.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.message.model.MsgNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * S+1: Webhook 通道 — POST JSON 到外部 URL.
 *
 * 用法:
 *   1. msg_subscription_rules.channel = 'WEBHOOK'
 *   2. msg_subscription_rules.webhook_url 字段 (新增) 或 user 配置中的默认 URL
 *
 * 当前简化实现: 从 msg_webhook_endpoints 表 (用户级别配置) 取 URL.
 * 表不存在时降级到全局默认 URL (inspection.message.webhook.default-url).
 *
 * 启用: inspection.message.webhook.enabled=true.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "inspection.message.webhook.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
public class WebhookChannelDispatcher implements ChannelDispatcher {

    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbc;

    @Value("${inspection.message.webhook.default-url:}")
    private String defaultUrl;

    @Value("${inspection.message.webhook.timeout-ms:5000}")
    private int timeoutMs;

    private RestTemplate restTemplate;

    @Override
    public String channelCode() { return "WEBHOOK"; }

    @Override
    public String displayName() { return "Webhook"; }

    @Override
    public DeliveryResult deliver(MsgNotification notification) {
        // 选 URL: 用户级 > 默认
        String url = resolveUrl(notification);
        if (url == null || url.isBlank()) {
            return DeliveryResult.failed("没有可用的 Webhook URL (用户级和全局默认都未配置)", false);
        }

        // 构造 payload
        Map<String, Object> payload = buildPayload(notification);

        try {
            ResponseEntity<String> resp = getRestTemplate().exchange(
                    url, HttpMethod.POST,
                    new HttpEntity<>(payload, jsonHeaders()),
                    String.class);
            int code = resp.getStatusCode().value();
            if (code >= 200 && code < 300) {
                log.info("[webhook] 投递成功 url={} status={} msgId={}", url, code, notification.getId());
                return DeliveryResult.ok();
            }
            return DeliveryResult.failed("HTTP " + code + ": " + resp.getBody(), code >= 500);
        } catch (RestClientException e) {
            log.warn("[webhook] 投递异常 url={} err={}", url, e.getMessage());
            return DeliveryResult.failed("Webhook 调用失败: " + e.getMessage(), true);
        } catch (Exception e) {
            return DeliveryResult.failed("Webhook 异常: " + e.getMessage(), true);
        }
    }

    private String resolveUrl(MsgNotification n) {
        // 尝试用户级配置 (表可能不存在 — 降级 default)
        if ("USER".equals(n.getReceiverType()) && n.getReceiverId() != null) {
            try {
                List<String> urls = jdbc.queryForList(
                        "SELECT url FROM msg_webhook_endpoints WHERE user_id = ? AND enabled = 1 AND deleted = 0 LIMIT 1",
                        String.class, n.getReceiverId());
                if (!urls.isEmpty()) return urls.get(0);
            } catch (Exception ignored) { /* 表不存在 */ }
        }
        return defaultUrl;
    }

    private Map<String, Object> buildPayload(MsgNotification n) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("id", n.getId());
        p.put("receiverType", n.getReceiverType());
        p.put("receiverId", n.getReceiverId());
        p.put("title", n.getTitle());
        p.put("content", n.getContent());
        p.put("eventCategory", n.getEventCategory());
        p.put("sourceEventType", n.getSourceEventType());
        p.put("subjectType", n.getSubjectType());
        p.put("subjectId", n.getSubjectId());
        p.put("subjectName", n.getSubjectName());
        p.put("createdAt", n.getCreatedAt() == null ? null : n.getCreatedAt().toString());
        return p;
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("X-Source", "inspection-platform");
        return h;
    }

    private RestTemplate getRestTemplate() {
        if (restTemplate == null) {
            org.springframework.http.client.SimpleClientHttpRequestFactory f =
                    new org.springframework.http.client.SimpleClientHttpRequestFactory();
            f.setConnectTimeout(timeoutMs);
            f.setReadTimeout(timeoutMs);
            restTemplate = new RestTemplate(f);
        }
        return restTemplate;
    }
}
