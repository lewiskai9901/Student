package com.school.management.interfaces.rest.system;

import com.school.management.application.access.AuthorizationService;
import com.school.management.application.event.TriggerService;
import com.school.management.common.result.Result;
import com.school.management.infrastructure.access.PluginDataScopeRouter;
import com.school.management.infrastructure.casbin.CasbinAccess;
import com.school.management.infrastructure.extension.PolicyContext;
import com.school.management.infrastructure.extension.PolicyRegistry;
import com.school.management.infrastructure.extension.TargetModeResolver;
import com.school.management.infrastructure.extension.Violation;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 插件测试沙箱 — 只给 SUPER_ADMIN 用的集成测试工作台.
 * 每个 endpoint 对应一种扩展点, 前端传参数后端直接调 SPI 返回结果.
 *
 * <p>用于手动验证:
 * <ul>
 *   <li>Policy Hook: 手动构造 payload 看哪些策略触发 / BLOCK / WARN</li>
 *   <li>TriggerPoint: 触发指定 point_code, 看写了多少事件+通知</li>
 *   <li>Relation implied: 给资源+关系, 看 BFS 展开后谁能访问</li>
 *   <li>DataScope 维度: 给 dim_code + userId, 看 resolver 返回什么</li>
 *   <li>TargetMode: 给 modeCode + config, 看解析出哪些用户</li>
 * </ul>
 */
@RestController
@RequestMapping("/plugin-platform/sandbox")
@RequiredArgsConstructor
public class PluginSandboxController {

    private final PolicyRegistry policyRegistry;
    private final TriggerService triggerService;
    private final AuthorizationService authorizationService;
    private final PluginDataScopeRouter pluginDataScopeRouter;
    private final List<TargetModeResolver> targetModeResolvers;
    private final JdbcTemplate jdbc;

    /**
     * POST /sandbox/policy/check
     * body: { entityType, phase, payload }
     * 调 PolicyRegistry.check(ctx), 返回 violations 列表. 不 enforce (不抛异常).
     */
    @PostMapping("/policy/check")
    @CasbinAccess(resource = "admin", action = "access")
    public Result<Map<String, Object>> policyCheck(@RequestBody Map<String, Object> body) {
        String entityType = (String) body.get("entityType");
        String phase = (String) body.get("phase");
        Object payload = body.get("payload");

        PolicyContext<Object> ctx = new PolicyContext<>(entityType, phase, payload);
        List<Violation> violations = policyRegistry.check(ctx);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("matched", violations.size());
        out.put("violations", violations.stream().map(v -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("severity", v.severity().name());
            m.put("code", v.code());
            m.put("message", v.message());
            m.put("detail", Optional.ofNullable(v.detail()).orElse(""));
            return m;
        }).toList());
        out.put("hasBlock", violations.stream().anyMatch(v -> v.severity() == Violation.Severity.BLOCK));
        return Result.success(out);
    }

    /**
     * POST /sandbox/trigger/fire
     * body: { code, context }
     * 调 triggerService.fire(code, context), 返回产生事件数 / 通知数.
     * 注意: 真实 fire 会落 DB!
     */
    @PostMapping("/trigger/fire")
    @CasbinAccess(resource = "admin", action = "access")
    @SuppressWarnings("unchecked")
    public Result<Map<String, Object>> triggerFire(@RequestBody Map<String, Object> body) {
        String code = (String) body.get("code");
        Map<String, Object> context = (Map<String, Object>) body.getOrDefault("context", Map.of());

        long beforeEvents = countSafely("SELECT COUNT(*) FROM entity_events");
        long beforeNotifications = countSafely("SELECT COUNT(*) FROM msg_notifications");

        Map<String, Object> out = new LinkedHashMap<>();
        try {
            triggerService.fire(code, context);
        } catch (Exception e) {
            out.put("triggered", false);
            out.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
            return Result.success(out);
        }

        long afterEvents = countSafely("SELECT COUNT(*) FROM entity_events");
        long afterNotifications = countSafely("SELECT COUNT(*) FROM msg_notifications");

        out.put("triggered", true);
        out.put("eventsCreated", afterEvents - beforeEvents);
        out.put("notificationsCreated", afterNotifications - beforeNotifications);
        return Result.success(out);
    }

    private long countSafely(String sql) {
        try {
            Long n = jdbc.queryForObject(sql, Long.class);
            return n != null ? n : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * POST /sandbox/relation/find
     * body: { resourceType, resourceId, relation, subjectType?, expandImplied }
     */
    @PostMapping("/relation/find")
    @CasbinAccess(resource = "admin", action = "access")
    public Result<Map<String, Object>> relationFind(@RequestBody Map<String, Object> body) {
        String resourceType = (String) body.get("resourceType");
        Long resourceId = ((Number) body.get("resourceId")).longValue();
        String relation = (String) body.get("relation");
        String subjectType = (String) body.getOrDefault("subjectType", null);
        boolean expandImplied = Boolean.TRUE.equals(body.getOrDefault("expandImplied", true));

        Map<String, Object> out = new LinkedHashMap<>();
        List<Long> ids;
        try {
            if (subjectType != null && !subjectType.isBlank()) {
                ids = authorizationService.findSubjectsWithRelation(
                    resourceType, resourceId, relation, subjectType, expandImplied);
            } else {
                ids = authorizationService.findSubjectsWithRelation(
                    resourceType, resourceId, relation, expandImplied);
            }
        } catch (Exception e) {
            out.put("error", e.getMessage());
            return Result.success(out);
        }
        out.put("subjectIds", ids);
        out.put("count", ids.size());
        return Result.success(out);
    }

    /**
     * POST /sandbox/datascope/resolve
     * body: { dimCode, userId, resourceType }
     */
    @PostMapping("/datascope/resolve")
    @CasbinAccess(resource = "admin", action = "access")
    public Result<Map<String, Object>> datascopeResolve(@RequestBody Map<String, Object> body) {
        String dimCode = (String) body.get("dimCode");
        Long userId = ((Number) body.get("userId")).longValue();
        String resourceType = (String) body.get("resourceType");

        List<Long> ids = pluginDataScopeRouter.resolve(dimCode, userId, resourceType);

        String interpretation;
        if (ids == null) interpretation = "null → 降级 SELF (维度未找到或 resolver 异常)";
        else if (ids.isEmpty()) interpretation = "空 list → 拒绝所有 (SQL WHERE 1=0)";
        else interpretation = "按 id 列表过滤";

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ids", ids != null ? ids : List.of());
        out.put("count", ids != null ? ids.size() : -1);
        out.put("interpretation", interpretation);
        out.put("resolverReturned", ids == null ? "null" : (ids.isEmpty() ? "[]" : "[...]"));
        return Result.success(out);
    }

    /**
     * POST /sandbox/target-mode/resolve
     * body: { modeCode, config, event }
     */
    @PostMapping("/target-mode/resolve")
    @CasbinAccess(resource = "admin", action = "access")
    @SuppressWarnings("unchecked")
    public Result<Map<String, Object>> targetModeResolve(@RequestBody Map<String, Object> body) {
        String modeCode = (String) body.get("modeCode");
        Map<String, Object> config = (Map<String, Object>) body.getOrDefault("config", Map.of());
        Map<String, Object> event = (Map<String, Object>) body.getOrDefault("event", Map.of());

        TargetModeResolver resolver = targetModeResolvers.stream()
            .filter(r -> r.modeCode().equals(modeCode))
            .findFirst().orElse(null);

        Map<String, Object> out = new LinkedHashMap<>();
        if (resolver == null) {
            out.put("error", "No resolver for modeCode=" + modeCode);
            out.put("available", targetModeResolvers.stream().map(TargetModeResolver::modeCode).toList());
            return Result.success(out);
        }

        List<Long> userIds;
        try {
            userIds = resolver.resolve(config, event);
            if (userIds == null) userIds = List.of();
        } catch (Exception e) {
            out.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
            return Result.success(out);
        }
        out.put("userIds", userIds);
        out.put("count", userIds.size());
        return Result.success(out);
    }

    /**
     * POST /sandbox/seed-demo
     * 创建一组样本数据用于演示流: 一个 [SANDBOX] 前缀宿舍, 方便 reset 时删.
     */
    @PostMapping("/seed-demo")
    @CasbinAccess(resource = "admin", action = "access")
    public Result<Map<String, Object>> seedDemo() {
        Long placeId = queryIdSafely(
            "SELECT id FROM places WHERE place_name LIKE '[SANDBOX]%' AND deleted = 0 LIMIT 1");
        if (placeId == null) {
            jdbc.update(
                "INSERT INTO places (place_code, place_name, type_code, capacity, current_occupancy, " +
                "status, tenant_id, created_at) " +
                "VALUES ('SANDBOX_D1', '[SANDBOX] 沙箱测试宿舍', 'DORMITORY', 4, 0, 1, 1, NOW())");
            placeId = queryIdSafely(
                "SELECT id FROM places WHERE place_code = 'SANDBOX_D1' AND deleted = 0 LIMIT 1");
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("placeId", placeId != null ? placeId : 0);
        out.put("hint", "样本数据已就绪. 可在沙箱中用此 placeId 触发 AFTER_CHECKIN 等测试.");
        return Result.success(out);
    }

    private Long queryIdSafely(String sql) {
        try {
            return jdbc.queryForObject(sql, Long.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * DELETE /sandbox/reset — 清理所有 [SANDBOX] 前缀数据
     */
    @DeleteMapping("/reset")
    @CasbinAccess(resource = "admin", action = "access")
    public Result<Map<String, Object>> reset() {
        int deletedPlaces = jdbc.update(
            "DELETE FROM places WHERE place_name LIKE '[SANDBOX]%'");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("deletedPlaces", deletedPlaces);
        return Result.success(out);
    }
}
