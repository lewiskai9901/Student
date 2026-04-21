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

/**
 * BY_FEATURE — 根据类型能力 (entity_type_configs.features JSON) 筛选用户.
 *
 * config: {"features":["isLearner"]} 或 {"features":["isLearner","receivesPersonalGrade"]}
 * 多 feature 为 AND 关系 (全部具备).
 *
 * 通用化优势: 学校场景判断 isLearner 命中 STUDENT; 培训机构场景 TRAINEE 也命中;
 * 业务代码不需要知道具体类型码.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ByFeatureTargetMode implements TargetModeResolver {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public String modeCode() {
        return "BY_FEATURE";
    }

    @Override
    public String displayName() {
        return "按能力";
    }

    @Override
    public List<Long> resolve(Map<String, Object> config, Map<String, Object> event) {
        try {
            if (config == null) return List.of();
            Object featuresObj = config.get("features");
            if (!(featuresObj instanceof Collection<?> col) || col.isEmpty()) {
                log.warn("[BY_FEATURE] 缺少 features 参数, config={}", config);
                return List.of();
            }
            List<String> features = col.stream().map(String::valueOf).toList();

            // 白名单校验 feature code, 防止注入
            for (String feature : features) {
                if (!feature.matches("[a-zA-Z0-9_]+")) {
                    log.warn("[BY_FEATURE] 非法 feature 名: {}", feature);
                    return List.of();
                }
            }

            StringBuilder sb = new StringBuilder(
                    "SELECT u.id FROM users u " +
                    "JOIN entity_type_configs c ON c.entity_type = 'USER' " +
                    "  AND c.type_code = u.user_type_code AND c.deleted = 0 " +
                    "WHERE u.deleted = 0 AND u.status = 1 ");
            for (String feature : features) {
                sb.append("  AND JSON_EXTRACT(c.features, '$.").append(feature).append("') = true ");
            }
            List<Long> ids = jdbcTemplate.queryForList(sb.toString(), Long.class);
            return new ArrayList<>(ids);
        } catch (Exception e) {
            log.warn("[BY_FEATURE] 查询失败: {}, config={}", e.getMessage(), config);
            return List.of();
        }
    }
}
