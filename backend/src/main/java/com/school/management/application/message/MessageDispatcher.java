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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
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
    private final AccessRelationMapper accessRelationMapper;
    private final JdbcTemplate jdbcTemplate;
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

        // 5. 批量构建 + 批量 insert（单条 INSERT 多 VALUES，分片 500/批）
        List<MsgNotification> notifications = new ArrayList<>(targetUserIds.size());
        for (Long userId : targetUserIds) {
            notifications.add(MsgNotification.createFromEvent(
                    tenantId, userId, title, content,
                    event.getEventType(),
                    event.getSourceRefType(), event.getSourceRefId(),
                    event.getSubjectType(), event.getSubjectId(), event.getSubjectName(),
                    event.getEventCategory(), event.getSourceModule(), event.getId()));
        }
        int inserted = notificationRepository.saveAll(notifications);

        log.info("[消息分发] 规则「{}」→ {} 个用户(写入 {} 条), 事件: {}/{}, 主体: {}/{}",
                rule.getRuleName(), targetUserIds.size(), inserted,
                event.getEventCategory(), event.getEventType(),
                event.getSubjectType(), event.getSubjectName());
    }

    /**
     * 预览模式：仅对与事件无关的 targetMode 返回命中用户集合；
     * 需要 subject 的模式（BY_ORG_ADMIN / BY_RELATED / BY_SUBJECT）返回 null 表示「需事件才能预览」。
     */
    public Set<Long> previewTargets(String mode, String targetConfig) {
        if (mode == null) return Collections.emptySet();
        return switch (mode) {
            case "BY_ROLE" -> resolveByRole(targetConfig);
            case "BY_USER" -> resolveByUser(targetConfig);
            case "BY_USER_TYPE" -> resolveByUserType(targetConfig);
            case "BY_ORG_TYPE" -> resolveByOrgType(targetConfig);
            case "BY_PLACE_TYPE" -> resolveByPlaceType(targetConfig);
            case "BY_ORG_ADMIN", "BY_RELATED", "BY_SUBJECT" -> null;
            default -> Collections.emptySet();
        };
    }

    // ── 目标用户解析 ─────────────────────────────────────────────

    private Set<Long> resolveTargetUsers(MsgSubscriptionRule rule, Long subjectId, String subjectType) {
        String mode = rule.getTargetMode();
        if (mode == null) return Collections.emptySet();

        return switch (mode) {
            // === v3 四大核心模式（推荐使用）===
            case "BY_SUBJECT" -> resolveBySubject(subjectId, subjectType);
            case "BY_RELATION" -> resolveByRelation(rule.getTargetConfig(), subjectId, subjectType);
            case "BY_ROLE" -> resolveByRole(rule.getTargetConfig());
            case "BY_FEATURE" -> resolveByFeature(rule.getTargetConfig());

            // === 向后兼容（v2 遗留，建议逐步迁移到上面 4 种）===
            case "BY_ORG_ADMIN" -> resolveByOrgAdmin(subjectId, subjectType);
            case "BY_USER" -> resolveByUser(rule.getTargetConfig());
            case "BY_USER_TYPE" -> resolveByUserType(rule.getTargetConfig());
            case "BY_ORG_TYPE" -> resolveByOrgType(rule.getTargetConfig());
            case "BY_PLACE_TYPE" -> resolveByPlaceType(rule.getTargetConfig());
            case "BY_RELATED" -> resolveByRelated(subjectId, subjectType);

            default -> {
                log.warn("[消息分发] 未知 targetMode: {}", mode);
                yield Collections.emptySet();
            }
        };
    }

    /** BY_ROLE: targetConfig = ["admin","teacher"] → 单次 JOIN + IN 查询拥有这些角色的用户 */
    private Set<Long> resolveByRole(String targetConfig) {
        List<String> roleCodes = parseStringList(targetConfig);
        if (roleCodes.isEmpty()) return Collections.emptySet();
        String placeholders = roleCodes.stream().map(t -> "?").collect(Collectors.joining(","));
        String sql = "SELECT DISTINCT ur.user_id " +
                "FROM user_roles ur " +
                "JOIN roles r ON r.id = ur.role_id AND r.deleted = 0 " +
                "WHERE r.role_code IN (" + placeholders + ")";
        try {
            List<Long> ids = jdbcTemplate.queryForList(sql, Long.class, roleCodes.toArray());
            return new HashSet<>(ids);
        } catch (Exception e) {
            log.warn("[消息分发] BY_ROLE 查询失败: {}", e.getMessage());
            return Collections.emptySet();
        }
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

    /** BY_USER_TYPE: targetConfig = ["STUDENT","TEACHER"] → 单次 IN 查询命中这些 user_type_code 的用户 */
    private Set<Long> resolveByUserType(String targetConfig) {
        List<String> typeCodes = parseStringList(targetConfig);
        if (typeCodes.isEmpty()) return Collections.emptySet();
        String placeholders = typeCodes.stream().map(t -> "?").collect(Collectors.joining(","));
        String sql = "SELECT id FROM users " +
                "WHERE deleted = 0 " +
                "  AND user_type_code IN (" + placeholders + ")";
        try {
            List<Long> ids = jdbcTemplate.queryForList(sql, Long.class, typeCodes.toArray());
            return new HashSet<>(ids);
        } catch (Exception e) {
            log.warn("[消息分发] BY_USER_TYPE 查询失败: {}", e.getMessage());
            return Collections.emptySet();
        }
    }

    /**
     * BY_ORG_TYPE: targetConfig = ["CLASS","GRADE"] → 命中这些 unit_type 组织的全部 member 用户
     * 两跳：org_units(unit_type) → access_relations(resource=org_unit, subject=user)
     */
    private Set<Long> resolveByOrgType(String targetConfig) {
        List<String> typeCodes = parseStringList(targetConfig);
        if (typeCodes.isEmpty()) return Collections.emptySet();
        String placeholders = typeCodes.stream().map(t -> "?").collect(Collectors.joining(","));
        String sql = "SELECT DISTINCT ar.subject_id " +
                "FROM access_relations ar " +
                "JOIN org_units ou ON ou.id = ar.resource_id AND ou.deleted = 0 " +
                "WHERE ar.resource_type = 'org_unit' " +
                "  AND ar.subject_type = 'user' " +
                "  AND ar.deleted = 0 " +
                "  AND ou.unit_type IN (" + placeholders + ")";
        try {
            List<Long> ids = jdbcTemplate.queryForList(sql, Long.class, typeCodes.toArray());
            return new HashSet<>(ids);
        } catch (Exception e) {
            log.warn("[消息分发] BY_ORG_TYPE 查询失败: {}", e.getMessage());
            return Collections.emptySet();
        }
    }

    /**
     * BY_PLACE_TYPE: targetConfig = ["TYPE_TRAINING","TYPE_LAB"] → 命中这些场所当前的 occupants
     * 两跳：places(type_code) → place_occupants(当前在住 check_out_time IS NULL)
     */
    private Set<Long> resolveByPlaceType(String targetConfig) {
        List<String> typeCodes = parseStringList(targetConfig);
        if (typeCodes.isEmpty()) return Collections.emptySet();
        String placeholders = typeCodes.stream().map(t -> "?").collect(Collectors.joining(","));
        String sql = "SELECT DISTINCT po.occupant_id " +
                "FROM place_occupants po " +
                "JOIN places p ON p.id = po.place_id AND p.deleted = 0 " +
                "WHERE po.deleted = 0 " +
                "  AND po.check_out_time IS NULL " +
                "  AND p.type_code IN (" + placeholders + ")";
        try {
            List<Long> ids = jdbcTemplate.queryForList(sql, Long.class, typeCodes.toArray());
            return new HashSet<>(ids);
        } catch (Exception e) {
            log.warn("[消息分发] BY_PLACE_TYPE 查询失败: {}", e.getMessage());
            return Collections.emptySet();
        }
    }

    /** BY_SUBJECT: 当主体就是 USER 时，直接把主体自己作为目标 */
    private Set<Long> resolveBySubject(Long subjectId, String subjectType) {
        if (subjectId == null || !"USER".equalsIgnoreCase(subjectType)) return Collections.emptySet();
        return Set.of(subjectId);
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

    /**
     * BY_RELATION: 基于 access_relations 统一关系查询
     *
     * targetConfig JSON 格式:
     *   {
     *     "relation": "admin",            // 必填 - 关系码
     *     "resource_type": "org_unit",    // 必填 - subject 面向的 resource 类型
     *     "direction": "inward",          // 可选 - inward(默认) = 找"以 subject 为 resource 的 user"
     *                                     //         outward = 找"subject 关联的 resources"
     *     "include_deputies": true        // 可选 - 同时纳入 deputy 关系
     *   }
     *
     * 典型场景:
     *   - 成绩发布 (subject=班级 ORG_UNIT):
     *     {relation:"admin", resource_type:"org_unit"}        → 通知该班班主任
     *   - 学生扣分 (subject=学生 USER):
     *     {relation:"guardian_of", resource_type:"user", direction:"outward"}  → 通知该学生的家长
     */
    private Set<Long> resolveByRelation(String targetConfig, Long subjectId, String subjectType) {
        if (subjectId == null) return Collections.emptySet();
        Map<String, Object> config = parseJsonObject(targetConfig);
        String relation = (String) config.get("relation");
        String resourceType = (String) config.get("resource_type");
        String direction = (String) config.getOrDefault("direction", "inward");
        boolean includeDeputies = Boolean.TRUE.equals(config.get("include_deputies"));

        if (relation == null || relation.isBlank()) {
            log.warn("[消息分发] BY_RELATION 缺少 relation 参数, config={}", targetConfig);
            return Collections.emptySet();
        }

        List<String> relations = new ArrayList<>();
        relations.add(relation);
        if (includeDeputies) relations.add("deputy");

        String placeholders = relations.stream().map(r -> "?").collect(Collectors.joining(","));
        try {
            String sql;
            Object[] params;
            if ("outward".equalsIgnoreCase(direction)) {
                // subject 是我们的主体,找它关联的 user 们(resource_type 必须是 user)
                sql = "SELECT DISTINCT ar.resource_id FROM access_relations ar " +
                      "WHERE ar.subject_type = ? AND ar.subject_id = ? " +
                      "  AND ar.resource_type = 'user' " +
                      "  AND ar.relation IN (" + placeholders + ") " +
                      "  AND ar.deleted = 0 " +
                      "  AND (ar.valid_to IS NULL OR ar.valid_to > NOW())";
                params = new Object[relations.size() + 2];
                params[0] = subjectType != null ? subjectType.toLowerCase() : "user";
                params[1] = subjectId;
                for (int i = 0; i < relations.size(); i++) params[i + 2] = relations.get(i);
            } else {
                // inward(默认): subject 是 resource,找它的 user 们
                sql = "SELECT DISTINCT ar.subject_id FROM access_relations ar " +
                      "WHERE ar.resource_type = ? AND ar.resource_id = ? " +
                      "  AND ar.subject_type = 'user' " +
                      "  AND ar.relation IN (" + placeholders + ") " +
                      "  AND ar.deleted = 0 " +
                      "  AND (ar.valid_to IS NULL OR ar.valid_to > NOW())";
                params = new Object[relations.size() + 2];
                String rt = resourceType != null ? resourceType.toLowerCase()
                        : (subjectType != null ? subjectType.toLowerCase() : "org_unit");
                params[0] = rt;
                params[1] = subjectId;
                for (int i = 0; i < relations.size(); i++) params[i + 2] = relations.get(i);
            }
            List<Long> ids = jdbcTemplate.queryForList(sql, Long.class, params);
            return new HashSet<>(ids);
        } catch (Exception e) {
            log.warn("[消息分发] BY_RELATION 查询失败: {}, config={}", e.getMessage(), targetConfig);
            return Collections.emptySet();
        }
    }

    /**
     * BY_FEATURE: 根据类型能力(feature)筛选用户,替代学校耦合的 BY_USER_TYPE
     *
     * targetConfig: {"features":["isLearner"]} 或 {"features":["isLearner","receivesPersonalGrade"]}
     * 多 feature 为 AND 关系(全部具备)。
     *
     * 通用化优势: 学校场景判断 isLearner 命中 STUDENT;培训机构场景 TRAINEE 也命中;
     * 业务代码不需要知道具体类型码。
     */
    private Set<Long> resolveByFeature(String targetConfig) {
        Map<String, Object> config = parseJsonObject(targetConfig);
        @SuppressWarnings("unchecked")
        List<String> features = (List<String>) config.get("features");
        if (features == null || features.isEmpty()) {
            log.warn("[消息分发] BY_FEATURE 缺少 features 参数, config={}", targetConfig);
            return Collections.emptySet();
        }

        // 构造 JSON_EXTRACT 条件: 每个 feature 必须为 true
        StringBuilder sb = new StringBuilder(
                "SELECT u.id FROM users u " +
                "JOIN entity_type_configs c ON c.entity_type = 'USER' " +
                "  AND c.type_code = u.user_type_code AND c.deleted = 0 " +
                "WHERE u.deleted = 0 AND u.status = 1 ");
        for (String feature : features) {
            // 白名单校验 feature code,防止注入
            if (!feature.matches("[a-zA-Z0-9_]+")) {
                log.warn("[消息分发] BY_FEATURE 非法 feature 名: {}", feature);
                return Collections.emptySet();
            }
            sb.append("  AND JSON_EXTRACT(c.features, '$.").append(feature).append("') = true ");
        }
        try {
            List<Long> ids = jdbcTemplate.queryForList(sb.toString(), Long.class);
            return new HashSet<>(ids);
        } catch (Exception e) {
            log.warn("[消息分发] BY_FEATURE 查询失败: {}, config={}", e.getMessage(), targetConfig);
            return Collections.emptySet();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonObject(String json) {
        if (json == null || json.isBlank()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
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
