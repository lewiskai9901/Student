package com.school.management.application.message;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.*;
import java.util.stream.Collectors;

/**
 * 消息分发器
 * 核心职责：根据订阅规则将实体事件转化为站内通知，分发给目标用户
 *
 * 链路: TriggerService → Spring Event → EntityEventDispatchListener → 此类
 *
 * 匹配逻辑:
 *   msg_subscription_rules.event_category IS NULL → 匹配所有类别
 *   msg_subscription_rules.event_type IS NULL     → 匹配类别下所有类型
 *   两者都 NULL → 匹配所有事件（全局规则）
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
     * 根据 EntityEvent 分发消息
     * @param event 已持久化的实体事件
     */
    public void dispatch(EntityEvent event) {
        try {
            // 1. 查询匹配的订阅规则
            List<MsgSubscriptionRule> rules = subscriptionRuleRepository.findByEventType(
                    event.getEventCategory(), event.getEventType());
            if (rules.isEmpty()) {
                log.debug("[消息分发] 无匹配规则: category={}, type={}",
                        event.getEventCategory(), event.getEventType());
                return;
            }

            // 2. 构建模板变量 (基础字段 + payload 内所有字段)
            Map<String, String> variables = buildVariables(event);

            // 3. 逐条规则处理
            for (MsgSubscriptionRule rule : rules) {
                try {
                    processRule(rule, event, variables);
                } catch (Exception e) {
                    log.warn("[消息分发] 规则 {}({}) 处理失败: {}",
                            rule.getRuleName(), rule.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("[消息分发] 事件分发异常: type={}, error={}",
                    event.getEventType(), e.getMessage());
        }
    }

    // ── 核心处理 ──────────────────────────────────────────────────

    private void processRule(MsgSubscriptionRule rule, EntityEvent event,
                             Map<String, String> variables) {
        // 1. 解析目标用户
        Set<Long> targetUserIds = resolveTargetUsers(rule, event.getSubjectId(), event.getSubjectType());
        if (targetUserIds.isEmpty()) {
            log.debug("[消息分发] 规则 {} 无目标用户", rule.getRuleName());
            return;
        }

        // 2. 渲染消息内容
        String title;
        String content;
        if (rule.getTemplateId() != null) {
            Optional<MsgTemplate> tplOpt = templateRepository.findById(rule.getTemplateId());
            if (tplOpt.isPresent()) {
                MsgTemplate.RenderedMessage rendered = tplOpt.get().render(variables);
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

        // 3. 为每个目标用户创建通知（携带完整主体/分类/模块信息）
        // tenantId 必须从事件取得；若事件未携带则拒绝派发，避免落入默认租户。
        Long tenantId = event.getTenantId();
        if (tenantId == null) {
            log.warn("[消息分发] 事件缺少 tenantId，跳过派发: eventId={}, type={}",
                    event.getId(), event.getEventType());
            return;
        }
        for (Long userId : targetUserIds) {
            MsgNotification notification = MsgNotification.createFromEvent(
                    tenantId, userId, title, content,
                    event.getEventType(),
                    event.getSourceRefType(), event.getSourceRefId(),
                    event.getSubjectType(), event.getSubjectId(), event.getSubjectName(),
                    event.getEventCategory(), event.getSourceModule(), event.getId());
            notificationRepository.save(notification);
        }

        log.info("[消息分发] 规则「{}」→ {} 个用户, 事件: {}/{}, 主体: {}/{}",
                rule.getRuleName(), targetUserIds.size(),
                event.getEventCategory(), event.getEventType(),
                event.getSubjectType(), event.getSubjectName());
    }

    // ── 目标用户解析 ─────────────────────────────────────────────

    private Set<Long> resolveTargetUsers(MsgSubscriptionRule rule, Long subjectId, String subjectType) {
        String mode = rule.getTargetMode();
        if (mode == null) return Collections.emptySet();

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

    /** BY_ROLE: targetConfig = ["admin","teacher"] → 查拥有这些角色的用户 */
    private Set<Long> resolveByRole(String targetConfig) {
        List<String> roleCodes = parseStringList(targetConfig);
        if (roleCodes.isEmpty()) return Collections.emptySet();
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

    /** BY_ORG_ADMIN: 找主体所属组织的管理员 */
    private Set<Long> resolveByOrgAdmin(Long subjectId, String subjectType) {
        if (subjectId == null) return Collections.emptySet();
        try {
            List<Long> ids = accessRelationMapper.findAccessibleResourceIds(
                    subjectType != null ? subjectType : "org_unit", "user", subjectId);
            return new HashSet<>(ids);
        } catch (Exception e) {
            log.warn("[消息分发] 查询 org admin 失败: {}", e.getMessage());
            return Collections.emptySet();
        }
    }

    /** BY_USER: targetConfig = [1, 2, 3] → 直接指定用户ID */
    private Set<Long> resolveByUser(String targetConfig) {
        return new HashSet<>(parseLongList(targetConfig));
    }

    /** BY_RELATED: 查询与主体有关联关系的用户 */
    private Set<Long> resolveByRelated(Long subjectId, String subjectType) {
        if (subjectId == null) return Collections.emptySet();
        try {
            List<Long> ids = accessRelationMapper.findAccessibleResourceIds(
                    "user", subjectType != null ? subjectType : "student", subjectId);
            return new HashSet<>(ids);
        } catch (Exception e) {
            log.warn("[消息分发] 查询关联用户失败: {}", e.getMessage());
            return Collections.emptySet();
        }
    }

    // ── 模板变量构建 ─────────────────────────────────────────────

    private Map<String, String> buildVariables(EntityEvent event) {
        Map<String, String> vars = new HashMap<>();
        vars.put("subjectName", safe(event.getSubjectName()));
        vars.put("eventLabel", safe(event.getEventLabel()));
        vars.put("eventType", safe(event.getEventType()));
        vars.put("eventCategory", safe(event.getEventCategory()));
        vars.put("sourceModule", safe(event.getSourceModule()));
        vars.put("time", event.getOccurredAt() != null ? event.getOccurredAt().toString() : "");

        // 从 payload JSON 注入所有字段为模板变量
        if (event.getPayload() != null) {
            try {
                Map<String, Object> payload = objectMapper.readValue(event.getPayload(),
                        new TypeReference<>() {});
                payload.forEach((k, v) -> {
                    if (!k.startsWith("_")) { // 跳过 _refType, _refId 等内部字段
                        vars.put(k, v != null ? v.toString() : "");
                    }
                });
            } catch (Exception e) {
                log.debug("[消息分发] payload 解析跳过: {}", e.getMessage());
            }
        }
        return vars;
    }

    private String safe(String s) { return s != null ? s : ""; }

    // ── JSON 解析工具 ─────────────────────────────────────────────

    private List<String> parseStringList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            // 可能是逗号分隔的字符串而非JSON数组
            return Arrays.asList(json.split(","));
        }
    }

    private List<Long> parseLongList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
