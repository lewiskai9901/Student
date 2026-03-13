package com.school.management.interfaces.rest.inspection.v7;

import com.school.management.application.inspection.v7.NotificationRuleApplicationService;
import com.school.management.common.result.Result;
import com.school.management.domain.inspection.model.v7.platform.NotificationRule;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v7/insp/notification-rules")
@RequiredArgsConstructor
public class NotificationRuleController {

    private final NotificationRuleApplicationService notificationRuleService;

    @PostMapping
    @CasbinAccess(resource = "insp:platform", action = "manage")
    public Result<NotificationRule> create(@RequestBody CreateNotificationRuleRequest request) {
        return Result.success(notificationRuleService.create(
                request.getProjectId(), request.getRuleName(), request.getEventType(),
                request.getChannels(), request.getRecipientType(), request.getRecipientConfig(),
                request.getCondition(), request.getPriority()));
    }

    @PutMapping("/{id}")
    @CasbinAccess(resource = "insp:platform", action = "manage")
    public Result<NotificationRule> update(@PathVariable Long id,
                                           @RequestBody UpdateNotificationRuleRequest request) {
        return Result.success(notificationRuleService.update(
                id, request.getRuleName(), request.getEventType(),
                request.getChannels(), request.getRecipientType(), request.getRecipientConfig(),
                request.getCondition(), request.getPriority()));
    }

    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "insp:platform", action = "manage")
    public Result<Void> delete(@PathVariable Long id) {
        notificationRuleService.delete(id);
        return Result.success();
    }

    @PutMapping("/{id}/enable")
    @CasbinAccess(resource = "insp:platform", action = "manage")
    public Result<Void> enable(@PathVariable Long id) {
        notificationRuleService.enable(id);
        return Result.success();
    }

    @PutMapping("/{id}/disable")
    @CasbinAccess(resource = "insp:platform", action = "manage")
    public Result<Void> disable(@PathVariable Long id) {
        notificationRuleService.disable(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    @CasbinAccess(resource = "insp:platform", action = "view")
    public Result<NotificationRule> findById(@PathVariable Long id) {
        return Result.success(notificationRuleService.findById(id));
    }

    @GetMapping
    @CasbinAccess(resource = "insp:platform", action = "view")
    public Result<List<NotificationRule>> list(@RequestParam(required = false) Long projectId) {
        if (projectId != null) {
            return Result.success(notificationRuleService.findByProjectId(projectId));
        }
        return Result.success(notificationRuleService.findAllEnabled());
    }

    @Data
    public static class CreateNotificationRuleRequest {
        private Long projectId;
        private String ruleName;
        private String eventType;
        private String channels;
        private String recipientType;
        private String recipientConfig;
        private String condition;
        private Integer priority;
    }

    @Data
    public static class UpdateNotificationRuleRequest {
        private String ruleName;
        private String eventType;
        private String channels;
        private String recipientType;
        private String recipientConfig;
        private String condition;
        private Integer priority;
    }
}
