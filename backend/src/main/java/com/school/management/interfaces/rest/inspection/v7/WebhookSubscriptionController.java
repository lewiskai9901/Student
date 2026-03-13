package com.school.management.interfaces.rest.inspection.v7;

import com.school.management.application.inspection.v7.WebhookSubscriptionApplicationService;
import com.school.management.common.result.Result;
import com.school.management.domain.inspection.model.v7.platform.WebhookSubscription;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v7/insp/webhooks")
@RequiredArgsConstructor
public class WebhookSubscriptionController {

    private final WebhookSubscriptionApplicationService webhookService;

    @PostMapping
    @CasbinAccess(resource = "insp:platform", action = "manage")
    public Result<WebhookSubscription> create(@RequestBody CreateWebhookRequest request) {
        return Result.success(webhookService.create(
                request.getProjectId(), request.getSubscriptionName(),
                request.getTargetUrl(), request.getSecret(),
                request.getEventTypes(), request.getRetryCount()));
    }

    @PutMapping("/{id}")
    @CasbinAccess(resource = "insp:platform", action = "manage")
    public Result<WebhookSubscription> update(@PathVariable Long id,
                                               @RequestBody UpdateWebhookRequest request) {
        return Result.success(webhookService.update(
                id, request.getSubscriptionName(), request.getTargetUrl(),
                request.getSecret(), request.getEventTypes(), request.getRetryCount()));
    }

    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "insp:platform", action = "manage")
    public Result<Void> delete(@PathVariable Long id) {
        webhookService.delete(id);
        return Result.success();
    }

    @PutMapping("/{id}/enable")
    @CasbinAccess(resource = "insp:platform", action = "manage")
    public Result<Void> enable(@PathVariable Long id) {
        webhookService.enable(id);
        return Result.success();
    }

    @PutMapping("/{id}/disable")
    @CasbinAccess(resource = "insp:platform", action = "manage")
    public Result<Void> disable(@PathVariable Long id) {
        webhookService.disable(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    @CasbinAccess(resource = "insp:platform", action = "view")
    public Result<WebhookSubscription> findById(@PathVariable Long id) {
        return Result.success(webhookService.findById(id));
    }

    @GetMapping
    @CasbinAccess(resource = "insp:platform", action = "view")
    public Result<List<WebhookSubscription>> list(@RequestParam(required = false) Long projectId) {
        if (projectId != null) {
            return Result.success(webhookService.findByProjectId(projectId));
        }
        return Result.success(webhookService.findAllEnabled());
    }

    @PostMapping("/{id}/test")
    @CasbinAccess(resource = "insp:platform", action = "manage")
    public Result<Void> test(@PathVariable Long id) {
        webhookService.testWebhook(id);
        return Result.success();
    }

    @Data
    public static class CreateWebhookRequest {
        private Long projectId;
        private String subscriptionName;
        private String targetUrl;
        private String secret;
        private String eventTypes;
        private Integer retryCount;
    }

    @Data
    public static class UpdateWebhookRequest {
        private String subscriptionName;
        private String targetUrl;
        private String secret;
        private String eventTypes;
        private Integer retryCount;
    }
}
