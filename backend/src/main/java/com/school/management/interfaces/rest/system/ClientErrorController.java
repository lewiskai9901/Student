package com.school.management.interfaces.rest.system;

import com.school.management.common.result.Result;
import com.school.management.infrastructure.access.UserContextHolder;
import com.school.management.infrastructure.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * P3 客户端错误上报 — 自建 Sentry-like 错误捕获.
 *
 * 入口: POST /api/errors/log (匿名可访问, 但记录 user_id)
 * 查询: GET  /api/errors/recent (要求 system:admin 权限)
 *
 * 防滥用:
 * - 速率限制: 同 fingerprint 60s 内最多 1 条 (合并 occurrence_count)
 * - 全局上限: 每分钟最多 100 条 (防止脚本 flood)
 * - 大字段截断: message 1KB / stack 16KB / url 512B
 */
@Slf4j
@RestController
@RequestMapping("/errors")
@RequiredArgsConstructor
public class ClientErrorController {

    private final JdbcTemplate jdbcTemplate;

    private static final int GLOBAL_RATE_LIMIT_PER_MINUTE = 100;
    private static final long FINGERPRINT_DEDUPE_WINDOW_MS = 60_000L;
    private static final int MAX_MESSAGE_LEN = 1024;
    private static final int MAX_STACK_LEN = 16_000;
    private static final int MAX_URL_LEN = 512;

    private static final ConcurrentHashMap<String, Long> recentFingerprints = new ConcurrentHashMap<>();
    private static final AtomicLong globalCounter = new AtomicLong(0);
    /** 用 AtomicLong 而非裸 long, 让 reset 真正原子, 防并发下两线程同时进入 reset 段. */
    private static final AtomicLong counterResetAt = new AtomicLong(System.currentTimeMillis());

    public record ClientError(
            String level,        // ERROR | WARN | INFO
            String source,       // JS | VUE | HTTP | UNHANDLED
            String message,
            String stack,
            String url,
            String routePath,
            String userAgent
    ) {}

    @PostMapping("/log")
    public Result<Map<String, Object>> log(@RequestBody ClientError err, HttpServletRequest req) {
        // 全局限流 — compareAndSet 保证 reset 段在多线程下只跑一次
        long now = System.currentTimeMillis();
        long lastReset = counterResetAt.get();
        if (now - lastReset > 60_000L && counterResetAt.compareAndSet(lastReset, now)) {
            globalCounter.set(0);
        }
        if (globalCounter.incrementAndGet() > GLOBAL_RATE_LIMIT_PER_MINUTE) {
            return Result.success(Map.of("dropped", true, "reason", "global_rate_limit"));
        }

        // 截断字段
        String message = truncate(err.message(), MAX_MESSAGE_LEN);
        String stack = truncate(err.stack(), MAX_STACK_LEN);
        String url = truncate(err.url(), MAX_URL_LEN);
        if (message == null || message.isBlank()) {
            return Result.success(Map.of("skipped", true, "reason", "empty_message"));
        }

        // 指纹: 同 message + url 1 分钟内只记 1 次, 累加 count
        String fingerprint = sha256Short((message + "|" + (url == null ? "" : url)));
        Long lastSeenAt = recentFingerprints.get(fingerprint);
        if (lastSeenAt != null && now - lastSeenAt < FINGERPRINT_DEDUPE_WINDOW_MS) {
            // 累加 occurrence_count
            try {
                jdbcTemplate.update(
                        "UPDATE client_error_logs SET occurrence_count = occurrence_count + 1, last_occurred_at = NOW() " +
                        "WHERE fingerprint = ? AND last_occurred_at > DATE_SUB(NOW(), INTERVAL 1 HOUR)",
                        fingerprint);
            } catch (Exception e) { log.warn("merge error log failed: {}", e.getMessage()); }
            return Result.success(Map.of("merged", true, "fingerprint", fingerprint));
        }
        recentFingerprints.put(fingerprint, now);

        // GC 旧指纹
        if (recentFingerprints.size() > 1000) {
            recentFingerprints.entrySet().removeIf(e -> now - e.getValue() > FINGERPRINT_DEDUPE_WINDOW_MS * 2);
        }

        Long userId = UserContextHolder.getUserId();
        Long tenantId = TenantContextHolder.getTenantId();

        try {
            jdbcTemplate.update(
                    "INSERT INTO client_error_logs(tenant_id, user_id, level, source, message, stack, url, route_path, user_agent, fingerprint) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    tenantId == null ? 0L : tenantId,
                    userId,
                    err.level() == null ? "ERROR" : err.level(),
                    err.source() == null ? "JS" : err.source(),
                    message,
                    stack,
                    url,
                    err.routePath(),
                    truncate(err.userAgent(), MAX_URL_LEN),
                    fingerprint);
            log.warn("[ClientError] level={} src={} userId={} url={} msg={}",
                    err.level(), err.source(), userId, url, message);
        } catch (Exception e) {
            log.error("Failed to write client error log: {}", e.getMessage());
        }
        return Result.success(Map.of("logged", true, "fingerprint", fingerprint));
    }

    /** 管理员查询近期错误 */
    @GetMapping("/recent")
    @PreAuthorize("hasAuthority('system:admin') or hasAuthority('*')")
    public Result<List<Map<String, Object>>> recent(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String level) {
        StringBuilder sql = new StringBuilder(
                "SELECT id, level, source, message, url, route_path, user_id, fingerprint, " +
                "occurrence_count, first_occurred_at, last_occurred_at, resolved " +
                "FROM client_error_logs WHERE 1=1 ");
        Object[] args;
        if (level != null && !level.isBlank()) {
            sql.append("AND level = ? ");
            args = new Object[]{level, Math.min(limit, 500)};
        } else {
            args = new Object[]{Math.min(limit, 500)};
        }
        sql.append("ORDER BY last_occurred_at DESC LIMIT ?");
        return Result.success(jdbcTemplate.queryForList(sql.toString(), args));
    }

    /** 标记已解决 */
    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAuthority('system:admin') or hasAuthority('*')")
    public Result<Void> resolve(@PathVariable Long id) {
        Long userId = UserContextHolder.getUserId();
        jdbcTemplate.update(
                "UPDATE client_error_logs SET resolved = 1, resolved_by = ?, resolved_at = NOW() WHERE id = ?",
                userId, id);
        return Result.success();
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String sha256Short(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(s.getBytes());
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 8; i++) hex.append(String.format("%02x", hash[i]));
            return hex.toString();
        } catch (Exception e) {
            return Integer.toHexString(s.hashCode());
        }
    }
}
