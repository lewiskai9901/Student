package com.school.management.application.message.targetmode;

import com.school.management.infrastructure.extension.TargetModeResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * BY_ROLE — 根据角色码列表查拥有这些角色的所有用户.
 *
 * config 兼容两种形态:
 *   - JSON 数组: ["admin","teacher"]  → MessageDispatcher 上游拍平为 {"__list__":[...]}
 *   - JSON 对象: {"roles":["admin","teacher"]}
 *
 * 单次 JOIN + IN 查询, 不展开派生.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ByRoleTargetMode implements TargetModeResolver {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public String modeCode() {
        return "BY_ROLE";
    }

    @Override
    public String displayName() {
        return "按角色";
    }

    @Override
    public List<Long> resolve(Map<String, Object> config, Map<String, Object> event) {
        try {
            if (config == null) return List.of();
            List<String> roleCodes = extractRoleCodes(config);
            if (roleCodes.isEmpty()) return List.of();
            String placeholders = roleCodes.stream().map(t -> "?").collect(Collectors.joining(","));
            String sql = "SELECT DISTINCT ur.user_id " +
                    "FROM user_roles ur " +
                    "JOIN roles r ON r.id = ur.role_id AND r.deleted = 0 " +
                    "WHERE r.role_code IN (" + placeholders + ")";
            List<Long> ids = jdbcTemplate.queryForList(sql, Long.class, roleCodes.toArray());
            return new ArrayList<>(ids);
        } catch (Exception e) {
            log.warn("[BY_ROLE] 查询失败: {}", e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRoleCodes(Map<String, Object> config) {
        Object rolesField = config.get("roles");
        if (rolesField instanceof Collection<?> col) {
            return col.stream().map(String::valueOf).toList();
        }
        Object flat = config.get("__list__");
        if (flat instanceof Collection<?> col) {
            return col.stream().map(String::valueOf).toList();
        }
        return List.of();
    }
}
