package com.school.management.application.access;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 启动时加载 relation_types 字典到内存,业务代码通过常量使用关系码。
 *
 * 使用:
 *   RelationCodes.MEMBER, RelationCodes.ADMIN, ...
 *   RelationTypeRegistry.isRegistered(code, from, to)
 *
 * 启动时序:
 *   旧实现 @PostConstruct 会在 RelationTypePluginRegistrar(@Order 200, ApplicationRunner)
 *   写入之前就执行, 冷启动首次读到空表.
 *   现改为 @EventListener(ApplicationReadyEvent) — 在所有 ApplicationRunner 执行完毕后加载,
 *   保证字典与插件声明一致.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RelationTypeRegistry {

    private final JdbcTemplate jdbcTemplate;

    /** key = "{code}|{from}|{to}", value = tier (CORE/COMMON_EXT/DOMAIN) */
    private final Map<String, String> registry = new ConcurrentHashMap<>();

    @EventListener(ApplicationReadyEvent.class)
    public void load() {
        registry.clear();
        jdbcTemplate.query(
            "SELECT relation_code, from_type, to_type, tier, is_enabled " +
            "FROM relation_types WHERE is_enabled = 1 AND plugin_enabled = 1",
            rs -> {
                String key = rs.getString("relation_code") + "|" +
                             rs.getString("from_type") + "|" +
                             rs.getString("to_type");
                registry.put(key, rs.getString("tier"));
            });
        log.info("[RelationTypeRegistry] 加载 {} 个关系类型: {}",
            registry.size(), registry.keySet());
    }

    public boolean isRegistered(String code, String fromType, String toType) {
        return registry.containsKey(code + "|" + fromType + "|" + toType);
    }

    public String getTier(String code, String fromType, String toType) {
        return registry.get(code + "|" + fromType + "|" + toType);
    }

    public Set<String> allKeys() {
        return Collections.unmodifiableSet(registry.keySet());
    }

    /** 热重载(管理员增加关系类型后调用) */
    public void reload() {
        load();
    }

    // ═══════════════════════════════════════════════════════════════
    // 核心关系常量(硬编码到代码,业务可直接依赖)
    // ═══════════════════════════════════════════════════════════════
    public static final class Relations {
        // CORE 9 个
        public static final String MEMBER       = "member";
        public static final String ADMIN        = "admin";
        public static final String DEPUTY       = "deputy";
        public static final String MANAGES      = "manages";
        public static final String BELONGS_TO   = "belongs_to";
        public static final String OCCUPIES     = "occupies";
        public static final String DELEGATED_TO = "delegated_to";
        public static final String WATCHES      = "watches";

        // 注:父子树形不走关系表,走 parent_id 字段

        private Relations() {}
    }

    // 实体类型常量
    public static final class Types {
        public static final String USER = "user";
        public static final String ORG_UNIT = "org_unit";
        public static final String PLACE = "place";
        private Types() {}
    }
}
