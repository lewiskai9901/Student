package com.school.management.application.message;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.application.event.EntityEventCreatedNotification;
import com.school.management.domain.event.model.EntityEvent;
import com.school.management.domain.message.model.MsgNotification;
import com.school.management.domain.message.model.MsgSubscriptionRule;
import com.school.management.domain.message.model.MsgTemplate;
import com.school.management.domain.message.repository.MsgNotificationRepository;
import com.school.management.domain.message.repository.MsgSubscriptionRuleRepository;
import com.school.management.domain.message.repository.MsgTemplateRepository;
import com.school.management.infrastructure.persistence.access.AccessRelationMapper;
import com.school.management.infrastructure.persistence.access.DddRoleMapper;
import com.school.management.infrastructure.persistence.access.DddUserRoleMapper;
import com.school.management.infrastructure.persistence.access.RolePO;
import com.school.management.infrastructure.persistence.access.UserRolePO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 消息分发器
 * 核心职责：根据订阅规则将事件转化为站内消息，分发给目标用户
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageDispatcher {

    private final MsgSubscriptionRuleRepository subscriptionRuleRepository;
    private final MsgTemplateRepository templateRepository;
    private final MsgNotificationRepository notificationRepository;
    private final DddRoleMapper roleMapper;
    private final DddUserRoleMapper userRoleMapper;
    private final AccessRelationMapper accessRelationMapper;
    private final ObjectMapper objectMapper;

    /**
     * 根据 EntityEventCreatedNotification 分发消息
     */
    public void dispatch(EntityEventCreatedNotification notification) {
        try {
            String eventCategory = deriveCategory(notification.getEventType());
            String eventType = notification.getEventType();

            List<MsgSubscriptionRule> rules = subscriptionRuleRepository.findByEventType(eventCategory, eventType);
            if (rules.isEmpty()) {
                return;
            }

            // 构建事件变量用于模板渲染
            Map<String, String> variables = buildVariables(notification);

            for (MsgSubscriptionRule rule : rules) {
                try {
                    processRule(rule, notification, variables);
                } catch (Exception e) {
                    log.warn("[消息分发] 规则 {} 处理失败: {}", rule.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("[消息分发] 事件 {} 处理异常", notification.getEventType(), e);
        }
    }

    /**
     * 根据 EntityEvent 分发消息（事件完整信息版本）
     */
    public void dispatch(EntityEvent event) {
        try {
            List<MsgSubscriptionRule> rules = subscriptionRuleRepository.findByEventType(
                    event.getEventCategory(), event.getEventType());
            if (rules.isEmpty()) {
                return;
            }

            Map<String, String> variables = buildVariablesFromEvent(event);

            for (MsgSubscriptionRule rule : rules) {
                try {
                    processRuleForEvent(rule, event, variables);
                } catch (Exception e) {
                    log.warn("[消息分发] 规则 {} 处理失败: {}", rule.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("[消息分发] 事件 {} 处理异常", event.getEventType(), e);
        }
    }

    // ── 私有方法 ──────────────────────────────────────────────────────────────

    private void processRule(MsgSubscriptionRule rule,
                              EntityEventCreatedNotification notification,
                              Map<String, String> variables) {
        // 解析目标用户
        Set<Long> targetUserIds = resolveTargetUsers(rule,
                notification.getSubjectId(), notification.getSubjectType());
        if (targetUserIds.isEmpty()) {
            return;
        }

        // 加载并渲染模板
        String title;
        String content;
        if (rule.getTemplateId() != null) {
            Optional<MsgTemplate> templateOpt = templateRepository.findById(rule.getTemplateId());
            if (templateOpt.isPresent()) {
                MsgTemplate.RenderedMessage rendered = templateOpt.get().render(variables);
                title = rendered.getTitle();
                content = rendered.getContent();
            } else {
                title = notification.getEventLabel();
                content = notification.getSubjectName() + " - " + notification.getEventLabel();
            }
        } else {
            title = notification.getEventLabel();
            content = notification.getSubjectName() + " - " + notification.getEventLabel();
        }

        // 为每个目标用户创建通知
        saveNotifications(targetUserIds, title, content, notification.getEventType(),
                null, null, 0L);
    }

    private void processRuleForEvent(MsgSubscriptionRule rule,
                                      EntityEvent event,
                                      Map<String, String> variables) {
        Set<Long> targetUserIds = resolveTargetUsers(rule, event.getSubjectId(), event.getSubjectType());
        if (targetUserIds.isEmpty()) {
            return;
        }

        String title;
        String content;
        if (rule.getTemplateId() != null) {
            Optional<MsgTemplate> templateOpt = templateRepository.findById(rule.getTemplateId());
            if (templateOpt.isPresent()) {
                MsgTemplate.RenderedMessage rendered = templateOpt.get().render(variables);
                title = rendered.getTitle();
                content = rendered.getContent();
            } else {
                title = event.getEventLabel();
                content = event.getSubjectName() + " - " + event.getEventLabel();
            }
        } else {
            title = event.getEventLabel();
            content = event.getSubjectName() + " - " + event.getEventLabel();
        }

        saveNotifications(targetUserIds, title, content, event.getEventType(),
                event.getSourceRefType(), event.getSourceRefId(),
                event.getTenantId() != null ? event.getTenantId() : 0L);
    }

    /**
     * 根据 targetMode 解析目标用户 ID 集合
     */
    private Set<Long> resolveTargetUsers(MsgSubscriptionRule rule, Long subjectId, String subjectType) {
        String mode = rule.getTargetMode();
        if (mode == null) {
            return Collections.emptySet();
        }

        return switch (mode) {
            case "BY_ROLE" -> resolveByRole(rule.getTargetConfig());
            case "BY_ORG_ADMIN" -> resolveByOrgAdmin(subjectId, subjectType);
            case "BY_USER" -> resolveByUser(rule.getTargetConfig());
            case "BY_RELATED" -> resolveByRelated(subjectId, subjectType);
            default -> {
                log.warn("[消息分发] 未知 targetMode: {}", mode);
                yield Collections.emptySet();
            }
        };
    }

    /**
     * BY_ROLE: 从 targetConfig 读角色编码列表，查所有拥有该角色的用户
     */
    private Set<Long> resolveByRole(String targetConfig) {
        List<String> roleCodes = parseStringList(targetConfig);
        if (roleCodes.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> userIds = new HashSet<>();
        for (String roleCode : roleCodes) {
            RolePO role = roleMapper.findByRoleCode(roleCode);
            if (role != null) {
                List<UserRolePO> userRoles = userRoleMapper.findByRoleId(role.getId());
                userRoles.forEach(ur -> userIds.add(ur.getUserId()));
            }
        }
        return userIds;
    }

    /**
     * BY_ORG_ADMIN: 找主体所属组织的管理员用户
     */
    private Set<Long> resolveByOrgAdmin(Long subjectId, String subjectType) {
        if (subjectId == null) {
            return Collections.emptySet();
        }
        // 查找对该主体有 manager/admin 关系的用户
        try {
            List<Long> managerUserIds = accessRelationMapper.findAccessibleResourceIds(
                    subjectType != null ? subjectType : "org_unit", "user", subjectId);
            return new HashSet<>(managerUserIds);
        } catch (Exception e) {
            log.warn("[消息分发] 查询 org admin 失败: {}", e.getMessage());
            return Collections.emptySet();
        }
    }

    /**
     * BY_USER: 直接从 targetConfig 读用户 ID 列表
     */
    private Set<Long> resolveByUser(String targetConfig) {
        List<Long> ids = parseLongList(targetConfig);
        return new HashSet<>(ids);
    }

    /**
     * BY_RELATED: 从事件关联主体中提取用户（这里简化为查询与主体有关联的用户）
     */
    private Set<Long> resolveByRelated(Long subjectId, String subjectType) {
        if (subjectId == null) {
            return Collections.emptySet();
        }
        try {
            // 查找与主体直接关联的用户
            List<Long> relatedUsers = accessRelationMapper.findAccessibleResourceIds(
                    "user", subjectType != null ? subjectType : "student", subjectId);
            return new HashSet<>(relatedUsers);
        } catch (Exception e) {
            log.warn("[消息分发] 查询关联用户失败: {}", e.getMessage());
            return Collections.emptySet();
        }
    }

    private void saveNotifications(Set<Long> userIds, String title, String content,
                                    String sourceEventType, String sourceRefType,
                                    Long sourceRefId, Long tenantId) {
        for (Long userId : userIds) {
            MsgNotification notification = MsgNotification.create(
                    tenantId, userId, title, content,
                    "EVENT", sourceEventType, sourceRefType, sourceRefId);
            notificationRepository.save(notification);
        }
        log.info("[消息分发] 已发送通知给 {} 个用户，事件类型: {}", userIds.size(), sourceEventType);
    }

    private Map<String, String> buildVariables(EntityEventCreatedNotification notification) {
        Map<String, String> vars = new HashMap<>();
        vars.put("subjectName", notification.getSubjectName());
        vars.put("eventLabel", notification.getEventLabel());
        vars.put("eventType", notification.getEventType());
        vars.put("time", java.time.LocalDateTime.now().toString());
        return vars;
    }

    private Map<String, String> buildVariablesFromEvent(EntityEvent event) {
        Map<String, String> vars = new HashMap<>();
        vars.put("subjectName", event.getSubjectName());
        vars.put("eventLabel", event.getEventLabel());
        vars.put("eventType", event.getEventType());
        vars.put("time", event.getOccurredAt() != null ? event.getOccurredAt().toString() : "");

        // 从 payload JSON 中提取变量
        if (event.getPayload() != null) {
            try {
                Map<String, Object> payload = objectMapper.readValue(event.getPayload(),
                        new TypeReference<Map<String, Object>>() {});
                payload.forEach((k, v) -> vars.put(k, v != null ? v.toString() : ""));
            } catch (Exception e) {
                log.debug("[消息分发] payload 解析失败，跳过变量注入: {}", e.getMessage());
            }
        }
        return vars;
    }

    /**
     * 根据事件类型推断大类
     */
    private String deriveCategory(String eventType) {
        if (eventType == null) return null;
        if (eventType.startsWith("INSP_")) return "INSPECTION";
        if (eventType.startsWith("EVAL_")) return "EVALUATION";
        if (eventType.startsWith("SYS_")) return "SYSTEM";
        return null;
    }

    private List<String> parseStringList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("[消息分发] 解析字符串列表失败: {}", json);
            return Collections.emptyList();
        }
    }

    private List<Long> parseLongList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            log.warn("[消息分发] 解析 Long 列表失败: {}", json);
            return Collections.emptyList();
        }
    }
}
