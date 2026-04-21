package com.school.management.application.message;

import com.school.management.domain.message.model.MsgNotification;
import com.school.management.domain.message.model.MsgSubscriptionRule;
import com.school.management.domain.message.model.MsgTemplate;
import com.school.management.domain.message.repository.MsgNotificationRepository;
import com.school.management.domain.message.repository.MsgSubscriptionRuleRepository;
import com.school.management.domain.message.repository.MsgTemplateRepository;
import com.school.management.exception.BusinessException;
import com.school.management.infrastructure.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 消息中心管理配置应用服务（管理员操作）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MsgConfigService {

    private final MsgSubscriptionRuleRepository subscriptionRuleRepository;
    private final MsgTemplateRepository templateRepository;
    private final MsgNotificationRepository notificationRepository;
    private final MessageDispatcher messageDispatcher;
    private final JdbcTemplate jdbcTemplate;

    /** 模板编码格式：大写字母、数字、下划线，长度 3-50。 */
    private static final Pattern TEMPLATE_CODE_PATTERN = Pattern.compile("^[A-Z0-9_]{3,50}$");

    // ── 订阅规则 CRUD ────────────────────────────────────────────────────────

    public List<MsgSubscriptionRule> listRules() {
        return subscriptionRuleRepository.findAll();
    }

    @Transactional
    public MsgSubscriptionRule createRule(MsgSubscriptionRule rule) {
        return subscriptionRuleRepository.save(rule);
    }

    @Transactional
    public MsgSubscriptionRule updateRule(Long id, MsgSubscriptionRule update) {
        subscriptionRuleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("订阅规则不存在: " + id));
        MsgSubscriptionRule updated = MsgSubscriptionRule.builder()
                .id(id)
                .tenantId(update.getTenantId())
                .ruleName(update.getRuleName())
                .eventCategory(update.getEventCategory())
                .eventType(update.getEventType())
                .targetMode(update.getTargetMode())
                .targetConfig(update.getTargetConfig())
                .channel(update.getChannel())
                .templateId(update.getTemplateId())
                .isEnabled(update.getIsEnabled())
                .sortOrder(update.getSortOrder())
                .createdBy(update.getCreatedBy())
                .build();
        return subscriptionRuleRepository.save(updated);
    }

    @Transactional
    public void deleteRule(Long id) {
        subscriptionRuleRepository.deleteById(id);
    }

    // ── 订阅规则预览 ─────────────────────────────────────────────────────────

    public PreviewResult previewRule(String mode, String targetConfig) {
        Set<Long> userIds = messageDispatcher.previewTargets(mode, targetConfig);
        // 需要事件上下文才能解析的模式：返回提示，不给数字
        if (userIds == null) {
            return PreviewResult.notPreviewable(
                    "模式「" + mode + "」依赖具体事件上下文（主体/关联关系），无法静态预览。仅在事件触发时解析。");
        }
        int total = userIds.size();
        if (total == 0) {
            return new PreviewResult(0, Collections.emptyList(), null, true);
        }
        // 取前 10 个样本用户信息（id / username / real_name）
        List<Long> sampleIds = userIds.stream().limit(10).collect(Collectors.toList());
        String placeholders = sampleIds.stream().map(x -> "?").collect(Collectors.joining(","));
        String sql = "SELECT id, username, real_name FROM users " +
                "WHERE id IN (" + placeholders + ") AND deleted = 0";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, sampleIds.toArray());
        List<SampleUser> samples = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            samples.add(new SampleUser(
                    ((Number) row.get("id")).longValue(),
                    (String) row.get("username"),
                    (String) row.get("real_name")));
        }
        String warning = null;
        if (total > 5000) {
            warning = "命中 " + total + " 人，超过单次派发上限 5000，实际派发将被截断。请收窄目标条件。";
        } else if (total > 1000) {
            warning = "命中 " + total + " 人（>1000），将产生大量通知，请确认是否符合预期。";
        }
        return new PreviewResult(total, samples, warning, true);
    }

    public record SampleUser(Long id, String username, String realName) {}

    public record PreviewResult(int totalCount, List<SampleUser> sampleUsers,
                                String warning, boolean previewable) {
        public static PreviewResult notPreviewable(String warning) {
            return new PreviewResult(0, Collections.emptyList(), warning, false);
        }
    }

    // ── 消息模板 CRUD ────────────────────────────────────────────────────────

    public List<MsgTemplate> listTemplates() {
        return templateRepository.findAll();
    }

    @Transactional
    public MsgTemplate createTemplate(MsgTemplate template) {
        validateTemplateCode(template.getTemplateCode());
        // 检查 code 唯一性
        templateRepository.findByCode(template.getTemplateCode()).ifPresent(existing -> {
            throw new BusinessException("模板编码已存在: " + template.getTemplateCode());
        });
        return templateRepository.save(template);
    }

    @Transactional
    public MsgTemplate updateTemplate(Long id, MsgTemplate update) {
        validateTemplateCode(update.getTemplateCode());
        MsgTemplate existing = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("消息模板不存在: " + id));
        if (existing.getIsSystem() != null && existing.getIsSystem() == 1) {
            throw new IllegalArgumentException("系统内置模板不允许修改");
        }
        MsgTemplate updated = MsgTemplate.builder()
                .id(id)
                .tenantId(update.getTenantId())
                .templateCode(update.getTemplateCode())
                .templateName(update.getTemplateName())
                .titleTemplate(update.getTitleTemplate())
                .contentTemplate(update.getContentTemplate())
                .isSystem(existing.getIsSystem())
                .isEnabled(update.getIsEnabled())
                .createdBy(existing.getCreatedBy())
                .build();
        return templateRepository.save(updated);
    }

    /**
     * 校验模板编码格式：大写字母、数字、下划线，长度 3-50。
     * 编码会被集成脚本、订阅规则等以字面值引用，
     * 限制字符集可避免空格、特殊符号导致的匹配失败与注入风险。
     */
    private void validateTemplateCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException("模板编码不能为空");
        }
        if (!TEMPLATE_CODE_PATTERN.matcher(code).matches()) {
            throw new BusinessException("模板编码只能包含大写字母、数字和下划线，长度 3-50");
        }
    }

    @Transactional
    public void deleteTemplate(Long id) {
        MsgTemplate existing = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("消息模板不存在: " + id));
        if (existing.getIsSystem() != null && existing.getIsSystem() == 1) {
            throw new IllegalArgumentException("系统内置模板不允许删除");
        }
        templateRepository.deleteById(id);
    }

    // ── 手动发送 ─────────────────────────────────────────────────────────────

    /**
     * 手动发送站内消息给指定用户列表
     */
    @Transactional
    public void sendManual(List<Long> userIds, String title, String content) {
        if (userIds == null || userIds.isEmpty()) {
            throw new IllegalArgumentException("接收用户列表不能为空");
        }
        // 手动派发必须承载当前操作者所在租户,防止跨租户消息注入
        Long tenantId = TenantContextHolder.getTenantId();
        for (Long userId : userIds) {
            MsgNotification notification = MsgNotification.create(
                    tenantId, userId, title, content, "MANUAL", null, null, null);
            notificationRepository.save(notification);
        }
        log.info("[消息中心] 手动发送消息给 {} 个用户 (tenant={})", userIds.size(), tenantId);
    }
}
