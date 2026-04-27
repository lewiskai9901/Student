package com.school.management.application.inspection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.common.util.SecurityUtils;
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
 * 落到 inspection_audit_logs 表; 失败仅 WARN, 不影响主业务事务.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InspectionAuditLogger {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 记录一条审计.
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
        try {
            Long actorId = null;
            String actorName = null;
            try {
                actorId = SecurityUtils.getCurrentUserId();
                actorName = SecurityUtils.getCurrentUsername();
            } catch (Exception ignored) {
                // 系统自动动作 (定时器/事件监听器) 没有当前用户上下文
            }
            String payloadJson = payload != null ? objectMapper.writeValueAsString(payload) : null;
            jdbcTemplate.update(
                    "INSERT INTO inspection_audit_logs " +
                    "(tenant_id, entity_type, entity_id, entity_code, action, actor_user_id, actor_user_name, " +
                    " reason, payload, occurred_at, created_at) " +
                    "VALUES (1, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    entityType, entityId, entityCode, action, actorId, actorName,
                    reason, payloadJson, LocalDateTime.now(), LocalDateTime.now());
        } catch (Exception e) {
            log.warn("写入 inspection_audit_logs 失败 entityType={} entityId={} action={}: {}",
                    entityType, entityId, action, e.getMessage());
        }
    }
}
