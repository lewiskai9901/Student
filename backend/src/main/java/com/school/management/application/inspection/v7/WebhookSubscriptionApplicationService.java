package com.school.management.application.inspection.v7;

import com.school.management.domain.inspection.model.v7.platform.WebhookSubscription;
import com.school.management.domain.inspection.repository.v7.WebhookSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class WebhookSubscriptionApplicationService {

    private final WebhookSubscriptionRepository subscriptionRepository;

    // ========== CRUD ==========

    @Transactional
    public WebhookSubscription create(Long projectId, String subscriptionName,
                                      String targetUrl, String secret,
                                      String eventTypes, Integer retryCount) {
        WebhookSubscription subscription = WebhookSubscription.reconstruct(WebhookSubscription.builder()
                .projectId(projectId)
                .subscriptionName(subscriptionName)
                .targetUrl(targetUrl)
                .secret(secret)
                .eventTypes(eventTypes)
                .retryCount(retryCount)
                .isEnabled(true));
        WebhookSubscription saved = subscriptionRepository.save(subscription);
        log.info("Created webhook subscription: name={}, url={}, projectId={}", subscriptionName, targetUrl, projectId);
        return saved;
    }

    @Transactional
    public WebhookSubscription update(Long id, String subscriptionName, String targetUrl,
                                      String secret, String eventTypes, Integer retryCount) {
        WebhookSubscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Webhook订阅不存在: " + id));
        subscription.updateSubscription(subscriptionName, targetUrl, secret, eventTypes, retryCount);
        WebhookSubscription saved = subscriptionRepository.save(subscription);
        log.info("Updated webhook subscription: id={}, name={}", id, subscriptionName);
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        subscriptionRepository.deleteById(id);
        log.info("Deleted webhook subscription: id={}", id);
    }

    @Transactional(readOnly = true)
    public WebhookSubscription findById(Long id) {
        return subscriptionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Webhook订阅不存在: " + id));
    }

    @Transactional(readOnly = true)
    public List<WebhookSubscription> findByProjectId(Long projectId) {
        return subscriptionRepository.findByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public List<WebhookSubscription> findAllEnabled() {
        return subscriptionRepository.findAllEnabled();
    }

    // ========== Enable / Disable ==========

    @Transactional
    public WebhookSubscription enable(Long id) {
        WebhookSubscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Webhook订阅不存在: " + id));
        subscription.enable();
        WebhookSubscription saved = subscriptionRepository.save(subscription);
        log.info("Enabled webhook subscription: id={}, name={}", id, subscription.getSubscriptionName());
        return saved;
    }

    @Transactional
    public WebhookSubscription disable(Long id) {
        WebhookSubscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Webhook订阅不存在: " + id));
        subscription.disable();
        WebhookSubscription saved = subscriptionRepository.save(subscription);
        log.info("Disabled webhook subscription: id={}, name={}", id, subscription.getSubscriptionName());
        return saved;
    }

    // ========== Test ==========

    /**
     * 测试 Webhook 连接（不实际发送 HTTP 请求，仅记录日志）。
     */
    @Transactional
    public void testWebhook(Long id) {
        WebhookSubscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Webhook订阅不存在: " + id));
        log.info("Test webhook triggered: id={}, name={}, targetUrl={}, eventTypes={}",
                id, subscription.getSubscriptionName(), subscription.getTargetUrl(), subscription.getEventTypes());
        // 实际 HTTP 调用待实现，当前仅记录测试日志
    }
}
