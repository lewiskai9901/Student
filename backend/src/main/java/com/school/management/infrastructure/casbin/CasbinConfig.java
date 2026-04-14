package com.school.management.infrastructure.casbin;

import lombok.extern.slf4j.Slf4j;
import org.casbin.jcasbin.main.Enforcer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

/**
 * Casbin enforcer 配置。
 *
 * <p>直接从业务表 ({@code role_permissions} + {@code user_roles}) 读取并灌入内存策略,
 * 不依赖 {@code casbin_rule} 表 (该表仅用于 jCasbin 库默认 adapter, 本项目不使用)。</p>
 *
 * <p>策略真相来源:
 * <ul>
 *   <li>p-policy: {@code role_permissions JOIN roles JOIN permissions}, 按 {@code permission_code}
 *       最后一个 {@code :} 切分为 (resource, action)</li>
 *   <li>g-policy: {@code user_roles JOIN roles}, 将 user_id 绑定到 role_code</li>
 * </ul>
 *
 * <p>权限变更后需重启或手动调用 {@link CasbinPolicyService#syncFromDatabase()} 重建内存策略。</p>
 */
@Slf4j
@Configuration
public class CasbinConfig {

    @Bean
    public Enforcer enforcer(JdbcTemplate jdbcTemplate) throws Exception {
        Path tempModel = Files.createTempFile("casbin-model", ".conf");
        try (InputStream is = new ClassPathResource("casbin/model.conf").getInputStream()) {
            Files.copy(is, tempModel, StandardCopyOption.REPLACE_EXISTING);
        }

        Enforcer enforcer = new Enforcer(tempModel.toString());
        Files.deleteIfExists(tempModel);

        int pCount = loadPolicies(enforcer, jdbcTemplate);
        int gCount = loadGroupings(enforcer, jdbcTemplate);
        log.info("Casbin enforcer initialized from business tables: {} p-policies, {} g-policies",
                pCount, gCount);

        return enforcer;
    }

    private int loadPolicies(Enforcer enforcer, JdbcTemplate jdbc) {
        String sql = "SELECT r.role_code, r.tenant_id, p.permission_code " +
                "FROM role_permissions rp " +
                "JOIN roles r ON r.id = rp.role_id AND r.deleted = 0 AND r.status = 1 " +
                "JOIN permissions p ON p.id = rp.permission_id AND p.deleted = 0 AND p.status = 1";

        int count = 0;
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(sql);
            for (Map<String, Object> row : rows) {
                String roleCode = (String) row.get("role_code");
                String tenant = String.valueOf(row.get("tenant_id"));
                String permCode = (String) row.get("permission_code");
                String[] parts = splitLastColon(permCode);
                if (enforcer.addPolicy(roleCode, tenant, parts[0], parts[1])) {
                    count++;
                }
            }
        } catch (Exception e) {
            log.error("Casbin p-policy load failed: {}", e.getMessage(), e);
        }
        return count;
    }

    private int loadGroupings(Enforcer enforcer, JdbcTemplate jdbc) {
        String sql = "SELECT ur.user_id, r.role_code, r.tenant_id " +
                "FROM user_roles ur " +
                "JOIN roles r ON r.id = ur.role_id AND r.deleted = 0 AND r.status = 1";

        int count = 0;
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(sql);
            for (Map<String, Object> row : rows) {
                String userId = String.valueOf(row.get("user_id"));
                String roleCode = (String) row.get("role_code");
                String tenant = String.valueOf(row.get("tenant_id"));
                if (enforcer.addGroupingPolicy(userId, roleCode, tenant)) {
                    count++;
                }
            }
        } catch (Exception e) {
            log.error("Casbin g-policy load failed: {}", e.getMessage(), e);
        }
        return count;
    }

    /**
     * 按最后一个冒号分离 resource / action。
     * {@code "academic:course:view"} → {@code ["academic:course", "view"]}
     * {@code "calendar"} (无冒号) → {@code ["calendar", "*"]}
     */
    static String[] splitLastColon(String permCode) {
        int idx = permCode.lastIndexOf(':');
        if (idx > 0 && idx < permCode.length() - 1) {
            return new String[]{permCode.substring(0, idx), permCode.substring(idx + 1)};
        }
        return new String[]{permCode, "*"};
    }
}
