package com.school.management.application.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.common.web.RequestContextHelper;
import com.school.management.domain.place.event.PlaceCapacityUpdatedEvent;
import com.school.management.domain.place.event.PlaceOrgAssignedEvent;
import com.school.management.domain.place.event.PlaceResponsibleAssignedEvent;
import com.school.management.domain.place.event.PlaceStatusChangedEvent;
import com.school.management.infrastructure.access.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Place 领域事件审计 listener — 写 place_audit_logs (P6-5).
 *
 * <p>place_audit_logs schema 是 CloudTrail 风格的 forensic audit table,
 * 11 个 NOT NULL 字段含 source_ip / api_endpoint 等 HTTP 上下文.
 *
 * <p>策略:
 * <ul>
 *   <li>HTTP 上下文 (RequestContextHelper) 抓 source_ip / user_agent / api_endpoint /
 *       http_method / request_id</li>
 *   <li>非 web 调用 (event listener 经常是 @Async, 但 RequestContextHolder 在
 *       同步分发时还能拿到; @Async 后失效, 用 'system' fallback)</li>
 *   <li>event_id = UUID</li>
 *   <li>resource_type / resource_id / event_name / event_type / event_source 从 event 抽</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlaceEventHandler {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @EventListener
    @Async
    public void handle(PlaceStatusChangedEvent event) {
        writePlaceAudit("PlaceStatusChanged", "STATUS_CHANGE",
            event.getPlaceId(), event.getPlaceName(),
            Map.of("oldStatus", String.valueOf(event.getOldStatus()),
                   "newStatus", String.valueOf(event.getNewStatus())),
            event.getReason());
    }

    @EventListener
    @Async
    public void handle(PlaceCapacityUpdatedEvent event) {
        writePlaceAudit("PlaceCapacityUpdated", "CAPACITY_CHANGE",
            event.getPlaceId(), null,
            Map.of("event", "capacity_updated"),
            null);
    }

    @EventListener
    @Async
    public void handle(PlaceOrgAssignedEvent event) {
        writePlaceAudit("PlaceOrgAssigned", "ORG_ASSIGNMENT",
            event.getPlaceId(), null,
            Map.of("event", "org_assigned"),
            null);
    }

    @EventListener
    @Async
    public void handle(PlaceResponsibleAssignedEvent event) {
        writePlaceAudit("PlaceResponsibleAssigned", "RESPONSIBLE_ASSIGNMENT",
            event.getPlaceId(), null,
            Map.of("event", "responsible_assigned"),
            null);
    }

    /**
     * 写 place_audit_logs — 一条 forensic 风格审计记录.
     */
    private void writePlaceAudit(String eventName, String eventType,
                                  Long resourceId, String resourceName,
                                  Map<String, Object> changes, String reason) {
        try {
            // event_id PRIMARY KEY varchar — 用 UUID
            String eventId = UUID.randomUUID().toString();
            String requestId = RequestContextHelper.requestId();
            if (requestId == null || requestId.isBlank()) requestId = eventId;

            String sourceIp = RequestContextHelper.clientIp();
            if (sourceIp == null) sourceIp = "system";   // 非 web 调用

            String apiEndpoint = RequestContextHelper.requestUri();
            if (apiEndpoint == null) apiEndpoint = "/internal/event-listener";

            String httpMethod = RequestContextHelper.httpMethod();
            String userAgent = RequestContextHelper.userAgent();

            Long userId = UserContextHolder.getUserId();
            String userName = UserContextHolder.getUsername();

            String changesJson = objectMapper.writeValueAsString(changes != null ? changes : Map.of());

            jdbcTemplate.update(
                "INSERT INTO place_audit_logs (event_id, request_id, resource_type, resource_id, " +
                "resource_name, event_name, event_type, event_source, event_time, " +
                "user_id, user_name, source_ip, user_agent, api_endpoint, http_method, " +
                "changed_fields, reason, tenant_id) " +
                "VALUES (?, ?, 'place', ?, ?, ?, ?, 'PlaceEventHandler', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                eventId, requestId, resourceId, resourceName, eventName, eventType,
                LocalDateTime.now(), userId, userName, sourceIp, userAgent,
                apiEndpoint, httpMethod, changesJson, reason, 1L);
        } catch (Exception e) {
            log.warn("[PlaceAudit] write failed for {} {}: {}", eventName, resourceId, e.getMessage());
        }
    }
}
