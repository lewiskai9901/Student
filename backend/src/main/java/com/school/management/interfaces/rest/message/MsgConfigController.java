package com.school.management.interfaces.rest.message;

import com.school.management.application.message.MsgConfigService;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.domain.message.model.MsgSubscriptionRule;
import com.school.management.domain.message.model.MsgTemplate;
import com.school.management.infrastructure.casbin.CasbinAccess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 消息中心管理配置 API（管理员）
 */
@RestController
@RequestMapping("/msg/config")
@Tag(name = "消息配置", description = "订阅规则与消息模板管理（管理员）")
@RequiredArgsConstructor
public class MsgConfigController {

    private final MsgConfigService configService;

    // ── 订阅规则 ─────────────────────────────────────────────────────────────

    @GetMapping("/rules")
    @Operation(summary = "获取所有订阅规则")
    @CasbinAccess(resource = "msg-config", action = "view")
    public Result<List<MsgSubscriptionRule>> listRules() {
        return Result.success(configService.listRules());
    }

    @PostMapping("/rules")
    @Operation(summary = "创建订阅规则")
    @CasbinAccess(resource = "msg-config", action = "create")
    public Result<MsgSubscriptionRule> createRule(@RequestBody CreateSubscriptionRuleRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        MsgSubscriptionRule rule = MsgSubscriptionRule.builder()
                .ruleName(request.getRuleName())
                .eventCategory(request.getEventCategory())
                .eventType(request.getEventType())
                .targetMode(request.getTargetMode())
                .targetConfig(request.getTargetConfig())
                .channel(request.getChannel() != null ? request.getChannel() : "IN_APP")
                .templateId(request.getTemplateId())
                .isEnabled(request.getIsEnabled() != null ? request.getIsEnabled() : 1)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .createdBy(currentUserId)
                .build();
        return Result.success(configService.createRule(rule));
    }

    @PutMapping("/rules/{id}")
    @Operation(summary = "更新订阅规则")
    @CasbinAccess(resource = "msg-config", action = "edit")
    public Result<MsgSubscriptionRule> updateRule(@PathVariable Long id,
                                                   @RequestBody CreateSubscriptionRuleRequest request) {
        MsgSubscriptionRule rule = MsgSubscriptionRule.builder()
                .ruleName(request.getRuleName())
                .eventCategory(request.getEventCategory())
                .eventType(request.getEventType())
                .targetMode(request.getTargetMode())
                .targetConfig(request.getTargetConfig())
                .channel(request.getChannel() != null ? request.getChannel() : "IN_APP")
                .templateId(request.getTemplateId())
                .isEnabled(request.getIsEnabled() != null ? request.getIsEnabled() : 1)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .build();
        return Result.success(configService.updateRule(id, rule));
    }

    @DeleteMapping("/rules/{id}")
    @Operation(summary = "删除订阅规则")
    @CasbinAccess(resource = "msg-config", action = "delete")
    public Result<Void> deleteRule(@PathVariable Long id) {
        configService.deleteRule(id);
        return Result.success();
    }

    // ── 消息模板 ─────────────────────────────────────────────────────────────

    @GetMapping("/templates")
    @Operation(summary = "获取所有消息模板")
    @CasbinAccess(resource = "msg-config", action = "view")
    public Result<List<MsgTemplate>> listTemplates() {
        return Result.success(configService.listTemplates());
    }

    @PostMapping("/templates")
    @Operation(summary = "创建消息模板")
    @CasbinAccess(resource = "msg-config", action = "create")
    public Result<MsgTemplate> createTemplate(@RequestBody CreateTemplateRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        MsgTemplate template = MsgTemplate.builder()
                .templateCode(request.getTemplateCode())
                .templateName(request.getTemplateName())
                .titleTemplate(request.getTitleTemplate())
                .contentTemplate(request.getContentTemplate())
                .isSystem(0)
                .isEnabled(request.getIsEnabled() != null ? request.getIsEnabled() : 1)
                .createdBy(currentUserId)
                .build();
        return Result.success(configService.createTemplate(template));
    }

    @PutMapping("/templates/{id}")
    @Operation(summary = "更新消息模板")
    @CasbinAccess(resource = "msg-config", action = "edit")
    public Result<MsgTemplate> updateTemplate(@PathVariable Long id,
                                               @RequestBody CreateTemplateRequest request) {
        MsgTemplate template = MsgTemplate.builder()
                .templateCode(request.getTemplateCode())
                .templateName(request.getTemplateName())
                .titleTemplate(request.getTitleTemplate())
                .contentTemplate(request.getContentTemplate())
                .isEnabled(request.getIsEnabled() != null ? request.getIsEnabled() : 1)
                .build();
        return Result.success(configService.updateTemplate(id, template));
    }

    @DeleteMapping("/templates/{id}")
    @Operation(summary = "删除消息模板")
    @CasbinAccess(resource = "msg-config", action = "delete")
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        configService.deleteTemplate(id);
        return Result.success();
    }

    // ── 手动发送 ─────────────────────────────────────────────────────────────

    @PostMapping("/send-manual")
    @Operation(summary = "手动发送站内消息")
    @CasbinAccess(resource = "msg-config", action = "create")
    public Result<Void> sendManual(@RequestBody SendManualRequest request) {
        configService.sendManual(request.getUserIds(), request.getTitle(), request.getContent());
        return Result.success();
    }
}
