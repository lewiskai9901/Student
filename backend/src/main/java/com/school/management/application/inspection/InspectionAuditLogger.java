package com.school.management.application.inspection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.common.util.SecurityUtils;
import com.school.management.infrastructure.access.UserContext;
import com.school.management.infrastructure.access.UserContextHolder;
import com.school.management.infrastructure.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 检查平台审计日志写入器 (review #14).
 *
 * <p>用法: 在高敏感动作 (申诉审核/责任人退出/任务驳回/模板升级) 处调用 {@link #log},
 * 落到 inspection_audit_logs 表; 失败 log.error + metric, 不影响主业务事务.
 *
 * <p>I4 (2026-05-17): tenant_id 不再硬编码 1, 改 TenantContextHolder; 加 org_unit_id 列
 * 让查询能按组织收窄. 写失败从 WARN 升到 ERROR (法务/审计场景丢日志比业务异常严重).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InspectionAuditLogger {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 记录一条审计 (走 actor 主归属 org 兜底).
     *
     * @param entityType 实体类型, 如 "InspAppeal" / "CorrectiveCase" / "InspTask" / "InspProject"
     * @param entityId   实体 ID
     * @param entityCode 业务编号 (可空)
     * @param action     动作 code (大写蛇形)
     * @param reason     动作原因
     * @param payload    上下文 map (序列化为 JSON), 可空
     */
    public void log(String entityType, Long entityId, String entityCode,
                     String action, String reason, Map<String, Object> payload) {
        log(entityType, entityId, entityCode, action, reason, payload, null);
    }

    /**
     * 记录一条审计 (I4 重载, 显式传 entity 的 org_unit_id 便于查询精确收窄).
     *
     * @param orgUnitId entity 所在组织 (传 null → 走 actor.orgUnitId 兜底)
     */
    public void log(String entityType, Long entityId, String entityCode,
                     String action, String reason, Map<String, Object> payload,
                     Long orgUnitId) {
        try {
            Long actorId = null;
            String actorName = null;
            try {
                actorId = SecurityUtils.getCurrentUserId();
                actorName = SecurityUtils.getCurrentUsername();
            } catch (Exception ignored) {
                // 系统自动动作 (定时器/事件监听器) 没有当前用户上下文
            }

            // I4: org_unit_id — 优先 caller 传, 否则取 actor 主归属
            Long effectiveOrgId = orgUnitId;
            if (effectiveOrgId == null) {
                UserContext ctx = UserContextHolder.getContext();
                if (ctx != null) effectiveOrgId = ctx.getOrgUnitId();
            }

            Long tenantId = TenantContextHolder.getTenantId();
            if (tenantId == null) tenantId = 1L;   // 系统线程兜底

            String payloadJson = payload != null ? objectMapper.writeValueAsString(payload) : null;
            jdbcTemplate.update(
                    "INSERT INTO inspection_audit_logs " +
                    "(tenant_id, entity_type, entity_id, entity_code, action, actor_user_id, actor_user_name, " +
                    " org_unit_id, reason, payload, occurred_at, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    tenantId, entityType, entityId, entityCode, action, actorId, actorName,
                    effectiveOrgId, reason, payloadJson, LocalDateTime.now(), LocalDateTime.now());
        } catch (Exception e) {
            // I4: 审计写失败是严重事件 (法务/纠纷场景), 升级到 ERROR + 包含完整 payload 摘要
            log.error("[CRITICAL] 写入 inspection_audit_logs 失败 entityType={} entityId={} action={} reason='{}' payload={}: {}",
                    entityType, entityId, action, reason, payload, e.getMessage(), e);
        }
    }
}
