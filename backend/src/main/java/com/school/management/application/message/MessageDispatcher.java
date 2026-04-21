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
import com.school.management.infrastructure.extension.TargetModeResolver;
import com.school.management.infrastructure.tenant.TenantContextHolder;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
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
 *
 * Track M2: target_mode 解析不再 switch 硬编码, 改为 Spring DI 收集
 * {@link TargetModeResolver} bean, 按 modeCode 查找. 行业要加 BY_WARD/BY_DEPARTMENT
 * 等新模式, 只需 implements TargetModeResolver + @Component (或
 * Contribution.TargetModeResolverContribution), 不改 core.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageDispatcher {

    private final MsgSubscriptionRuleRepository subscriptionRuleRepository;
    private final MsgTemplateRepository templateRepository;
    private final MsgNotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;
    private final List<TargetModeResolver> resolvers;
    private final JdbcTemplate jdbcTemplate;

    private Map<String, TargetModeResolver> resolverByMode;

    @PostConstruct
    void initResolverMap() {
        resolverByMode = resolvers.stream()
            .collect(Collectors.toMap(TargetModeResolver::modeCode, Function.identity(),
                (a, b) -> {
                    log.error("[MessageDispatcher] duplicate TargetModeResolver for {}: {} vs {}",
                        a.modeCode(), a.getClass(), b.getClass());
                    return a;
                }));
        log.info("[MessageDispatcher] 加载 {} target mode resolvers: {}",
            resolverByMode.size(), resolverByMode.keySet());
    }

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
        Set<Long> targetUserIds = resolveTargetUsers(rule, event);
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

        // 3. 规模保护：单条规则命中 > MAX_TARGETS_PER_RULE 直接截断，避免误配爆库
        final int MAX_TARGETS_PER_RULE = 5000;
        final int WARN_TARGETS_PER_RULE = 1000;
        if (targetUserIds.size() > MAX_TARGETS_PER_RULE) {
            log.error("[消息分发] 规则「{}」命中 {} 个用户，超过上限 {}，已截断。请检查规则配置是否合理。",
                    rule.getRuleName(), targetUserIds.size(), MAX_TARGETS_PER_RULE);
            targetUserIds = targetUserIds.stream().limit(MAX_TARGETS_PER_RULE).collect(Collectors.toSet());
        } else if (targetUserIds.size() > WARN_TARGETS_PER_RULE) {
            log.warn("[消息分发] 规则「{}」命中 {} 个用户（>{}），正在批量派发。",
                    rule.getRuleName(), targetUserIds.size(), WARN_TARGETS_PER_RULE);
        }

        // 4. tenantId 必须从事件取得；缺失则拒绝派发，避免落入默认租户。
        Long tenantId = event.getTenantId();
        if (tenantId == null) {
            log.warn("[消息分发] 事件缺少 tenantId，跳过派发: eventId={}, type={}",
                    event.getId(), event.getEventType());
            return;
        }

        // 5. 批量构建 + 批量 insert
        //    M4.2: 先做一次 saveAll (高性能路径); 若整批失败, 降级 per-user save + 写 DLQ,
        //    保证单用户数据问题不让整批派发失败.
        List<MsgNotification> notifications = new ArrayList<>(targetUserIds.size());
        for (Long userId : targetUserIds) {
            notifications.add(MsgNotification.createFromEvent(
                    tenantId, userId, title, content,
                    event.getEventType(),
                    event.getSourceRefType(), event.getSourceRefId(),
                    event.getSubjectType(), event.getSubjectId(), event.getSubjectName(),
                    event.getEventCategory(), event.getSourceModule(), event.getId()));
        }
        int inserted;
        int dlqWritten = 0;
        try {
            inserted = notificationRepository.saveAll(notifications);
        } catch (Exception batchEx) {
            log.warn("[消息分发] 批量 saveAll 失败, 降级 per-user 保存 + DLQ: ruleId={}, err={}",
                    rule.getId(), batchEx.getMessage());
            inserted = 0;
            for (MsgNotification notification : notifications) {
                try {
                    notificationRepository.save(notification);
                    inserted++;
                } catch (Exception singleEx) {
                    writeDlq(event, rule, notification.getUserId(), singleEx);
                    dlqWritten++;
                }
            }
        }

        log.info("[消息分发] 规则「{}」→ {} 个用户(写入 {} 条, DLQ {} 条), 事件: {}/{}, 主体: {}/{}",
                rule.getRuleName(), targetUserIds.size(), inserted, dlqWritten,
                event.getEventCategory(), event.getEventType(),
                event.getSubjectType(), event.getSubjectName());
    }

    /**
     * M4.2: 写消息派发死信表. 不抛异常 (DLQ 写失败只打 error 日志, 不影响主链路).
     * 5 分钟后可重试一次; 生产环境应有定时 job 消费 next_retry_at < NOW() AND retry_count < N 的记录.
     */
    private void writeDlq(EntityEvent event, MsgSubscriptionRule rule, Long targetUserId, Exception ex) {
        try {
            Long tenantId = event.getTenantId() != null
                    ? event.getTenantId()
                    : TenantContextHolder.getTenantId();
            if (tenantId == null) tenantId = 1L;
            String errMsg = ex.getClass().getSimpleName() + ": "
                    + (ex.getMessage() != null ? ex.getMessage() : "");
            // error_message 是 TEXT 列, 做长度保护避免异常链过长
            if (errMsg.length() > 4000) errMsg = errMsg.substring(0, 4000);
            jdbcTemplate.update(
                    "INSERT INTO msg_notification_failures " +
                    "(event_id, rule_id, target_user_id, error_message, retry_count, next_retry_at, tenant_id) " +
                    "VALUES (?, ?, ?, ?, 0, DATE_ADD(NOW(), INTERVAL 5 MINUTE), ?)",
                    event.getId(), rule.getId(), targetUserId, errMsg, tenantId);
            log.warn("[消息分发] DLQ 写入成功: eventId={}, ruleId={}, userId={}, err={}",
                    event.getId(), rule.getId(), targetUserId, errMsg);
        } catch (Exception dlqEx) {
            // DLQ 自身失败不能再抛了; 只记 error
            log.error("[消息分发] DLQ 写入失败 (原异常被吞): eventId={}, ruleId={}, userId={}, dlqErr={}, origErr={}",
                    event.getId(), rule.getId(), targetUserId, dlqEx.getMessage(), ex.getMessage());
        }
    }

    /**
     * 预览模式：返回规则命中的目标用户集合 (用于 UI 调试)。
     * 依赖事件上下文的模式 ({@link #CONTEXT_DEPENDENT_MODES}) 无事件时返回 null,
     * 让前端提示 "该模式需具体事件触发才能解析".
     */
    public Set<Long> previewTargets(String mode, String targetConfig) {
        if (mode == null) return Collections.emptySet();
        TargetModeResolver r = resolverByMode.get(mode);
        if (r == null) {
            log.warn("[消息分发] previewTargets: 未知 target_mode {}", mode);
            return Collections.emptySet();
        }
        if (!r.supportsPreview()) return null;   // 依赖事件上下文, 无法静态预览
        try {
            Map<String, Object> cfg = parseConfig(targetConfig);
            List<Long> ids = r.resolve(cfg, Collections.emptyMap());
            return ids == null ? Collections.emptySet() : new LinkedHashSet<>(ids);
        } catch (Exception e) {
            log.warn("[消息分发] previewTargets 异常, mode={}, config={}, err={}",
                mode, targetConfig, e.getMessage());
            return Collections.emptySet();
        }
    }

    // ── 目标用户解析 ────────────────────────────────────────────────

    /**
     * 通过 SPI 查找 resolver, 不硬编码 switch. 未知 mode / resolver 异常均降级空集.
     */
    private Set<Long> resolveTargetUsers(MsgSubscriptionRule rule, EntityEvent event) {
        String mode = rule.getTargetMode();
        if (mode == null) return Collections.emptySet();

        TargetModeResolver r = resolverByMode.get(mode);
        if (r == null) {
            log.warn("[消息分发] 未知 targetMode: {} — 请确认该模式对应 TargetModeResolver 已注册", mode);
            return Collections.emptySet();
        }

        try {
            Map<String, Object> cfg = parseConfig(rule.getTargetConfig());
            Map<String, Object> eventMap = toEventMap(event);
            List<Long> ids = r.resolve(cfg, eventMap);
            return ids == null ? Collections.emptySet() : new LinkedHashSet<>(ids);
        } catch (Exception e) {
            log.warn("[消息分发] resolver {} 解析失败, ruleId={}, err={}",
                mode, rule.getId(), e.getMessage());
            return Collections.emptySet();
        }
    }

    /**
     * targetConfig 兼容两种 JSON 形态:
     *   - 对象: {"relation":"admin", ...}      → 原样返回
     *   - 数组: ["admin","teacher"]            → 包装为 {"__list__": [...]} (历史 BY_ROLE 契约)
     */
    private Map<String, Object> parseConfig(String json) {
        if (json == null || json.isBlank()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception eObj) {
            // 尝试数组形态
            try {
                List<Object> list = objectMapper.readValue(json, new TypeReference<List<Object>>() {});
                Map<String, Object> wrapped = new HashMap<>();
                wrapped.put("__list__", list);
                return wrapped;
            } catch (Exception eList) {
                // 逗号分隔字符串兜底 (与历史 parseStringList 同步)
                Map<String, Object> wrapped = new HashMap<>();
                wrapped.put("__list__", Arrays.asList(json.split(",")));
                return wrapped;
            }
        }
    }

    /** 将 EntityEvent 展平为 resolver 可读的 Map, 避免跨 layer 依赖 domain model. */
    private Map<String, Object> toEventMap(EntityEvent event) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", event.getId());
        m.put("tenantId", event.getTenantId());
        m.put("subjectType", event.getSubjectType());
        m.put("subjectId", event.getSubjectId());
        m.put("subjectName", event.getSubjectName());
        m.put("eventCategory", event.getEventCategory());
        m.put("eventType", event.getEventType());
        m.put("eventLabel", event.getEventLabel());
        m.put("sourceModule", event.getSourceModule());
        m.put("sourceRefType", event.getSourceRefType());
        m.put("sourceRefId", event.getSourceRefId());
        m.put("payload", event.getPayload());
        return m;
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
}
