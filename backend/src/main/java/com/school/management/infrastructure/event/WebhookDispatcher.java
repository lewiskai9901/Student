package com.school.management.infrastructure.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.inspection.event.v7.InspV7DomainEvent;
import com.school.management.domain.inspection.model.v7.platform.WebhookSubscription;
import com.school.management.domain.inspection.repository.v7.WebhookSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;

/**
 * Webhook 分发器 — 监听 InspV7DomainEvent，将事件以 HTTP POST 发送给订阅者。
 * 支持 HMAC-SHA256 签名、重试机制。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class WebhookDispatcher {

    private final WebhookSubscriptionRepository webhookSubscriptionRepository;
    private final ObjectMapper objectMapper;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Async
    @EventListener
    public void onInspV7Event(InspV7DomainEvent event) {
        String eventType = event.getClass().getSimpleName();
        log.debug("WebhookDispatcher received event: {}", eventType);

        try {
            List<WebhookSubscription> subscriptions = webhookSubscriptionRepository.findAllEnabled();
            for (WebhookSubscription sub : subscriptions) {
                if (matchesEventType(sub, eventType)) {
                    dispatchWebhook(sub, eventType, event);
                }
            }
        } catch (Exception e) {
            log.error("WebhookDispatcher failed for event {}: {}", eventType, e.getMessage(), e);
        }
    }

    private boolean matchesEventType(WebhookSubscription sub, String eventType) {
        String eventTypes = sub.getEventTypes();
        if (eventTypes == null || eventTypes.isBlank()) return false;
        return eventTypes.contains(eventType) || eventTypes.contains("*");
    }

    private void dispatchWebhook(WebhookSubscription sub, String eventType, InspV7DomainEvent event) {
        int maxRetries = sub.getRetryCount() != null ? sub.getRetryCount() : 3;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String payload = objectMapper.writeValueAsString(new WebhookPayload(eventType, event));
                String signature = computeHmac(payload, sub.getSecret());

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(sub.getTargetUrl()))
                        .header("Content-Type", "application/json")
                        .header("X-Webhook-Event", eventType)
                        .header("X-Webhook-Signature", signature)
                        .timeout(Duration.ofSeconds(15))
                        .POST(HttpRequest.BodyPublishers.ofString(payload))
                        .build();

                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    sub.recordTriggerSuccess();
                    webhookSubscriptionRepository.save(sub);
                    log.info("Webhook delivered: sub='{}', event={}, status={}",
                            sub.getSubscriptionName(), eventType, response.statusCode());
                    return;
                } else {
                    log.warn("Webhook response non-2xx: sub='{}', attempt={}/{}, status={}",
                            sub.getSubscriptionName(), attempt, maxRetries, response.statusCode());
                }
            } catch (Exception e) {
                log.warn("Webhook delivery failed: sub='{}', attempt={}/{}, error={}",
                        sub.getSubscriptionName(), attempt, maxRetries, e.getMessage());
            }

            // Exponential backoff before retry
            if (attempt < maxRetries) {
                try {
                    Thread.sleep(1000L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        // All retries exhausted
        sub.recordTriggerFailure();
        webhookSubscriptionRepository.save(sub);
        log.error("Webhook delivery exhausted all retries: sub='{}', event={}",
                sub.getSubscriptionName(), eventType);
    }

    private String computeHmac(String payload, String secret) {
        if (secret == null || secret.isBlank()) return "";
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return "sha256=" + HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            log.error("HMAC computation failed: {}", e.getMessage());
            return "";
        }
    }

    private record WebhookPayload(String eventType, Object eventData) {
    }
}
