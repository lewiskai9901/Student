package com.school.management.infrastructure.access;

import com.school.management.domain.access.model.StorageKind;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 资源关系注册表读服务 (统一锚定模型 R2.2) —— 从 {@code resource_relations} 派生
 * {@link ResourceScopeMeta} 所需的锚点字段 (orgUnitField / creatorField / viaMembership)。
 *
 * <p>R2.2 把 {@code DataPermissionInterceptor.buildMeta} 的锚点来源从「注解 ⊕ data_resources 列」
 * 切到本注册表 (注册表优先, 注解兜底), 实现「锚点集中声明」。本类的 {@link #deriveAnchor} 是
 * 纯函数 (注册行 → 派生锚点), 与 DB 加载分离, 便于等价单测。
 *
 * <p><b>仅驱动 3 个字段</b>: orgUnitField / creatorField / viaMembership。
 * tableAlias / membershipSubjectColumn (query 形态) 仍来自注解; typeField 仍来自 data_resources;
 * resourceType 仍来自注解/data_resources (当前全 NULL)。
 *
 * <p>启动期 (ApplicationReady, 在 {@code ContributionDispatcher} @Order(60) ApplicationRunner 写完
 * resource_relations 之后) 全量加载进内存缓存; {@link #forResource} 供拦截器热路径无 DB 命中。
 * resource_relations 是 contribution 驱动 (重启级), 故缓存够用; 配置变更可调 {@link #refresh}。
 */
@Slf4j
@Component
public class ResourceRelationRegistry {

    private final JdbcTemplate jdbc;
    private final Map<String, DerivedAnchor> cache = new ConcurrentHashMap<>();

    public ResourceRelationRegistry(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 一条 resource_relations 行里派生锚点所需的最小字段。 */
    public record AnchorRow(String relationCode, StorageKind storageKind, String columnName) {}

    /** 从注册行派生出的锚点视图 (对应 ResourceScopeMeta 的 3 个字段)。 */
    public record DerivedAnchor(boolean viaMembership, String orgUnitField, String creatorField) {}

    /** 关系码常量 (与 manifest 登记一致)。 */
    public static final String OWNER_ORG = "owner_org";
    public static final String CREATOR = "creator";

    /**
     * 纯函数: 一个资源的注册行集合 → 派生锚点。
     *
     * <ul>
     *   <li>{@code owner_org} 行: storage=SUBJECT_GRAPH → viaMembership=true, orgUnitField=null
     *       (成员图路径, 组织来自 access_relations member); storage=COLUMN → viaMembership=false,
     *       orgUnitField=该列名。</li>
     *   <li>{@code creator} 行: creatorField = 该列名。</li>
     *   <li>无 owner_org 行: orgUnitField=null, viaMembership=false (该资源无组织锚)。</li>
     *   <li>无 creator 行: creatorField=null。</li>
     * </ul>
     */
    public static DerivedAnchor deriveAnchor(List<AnchorRow> rows) {
        boolean viaMembership = false;
        String orgUnitField = null;
        String creatorField = null;
        for (AnchorRow row : rows) {
            if (OWNER_ORG.equals(row.relationCode())) {
                if (row.storageKind() == StorageKind.SUBJECT_GRAPH) {
                    viaMembership = true;   // 成员图: 组织来自 access_relations member, 无列锚
                    orgUnitField = null;
                } else if (row.storageKind() == StorageKind.COLUMN) {
                    viaMembership = false;
                    orgUnitField = row.columnName();
                }
            } else if (CREATOR.equals(row.relationCode())) {
                creatorField = row.columnName();
            }
        }
        return new DerivedAnchor(viaMembership, orgUnitField, creatorField);
    }

    // ── DB 加载 + 缓存 ─────────────────────────────────────────────────────

    /** 启动期全量加载 (ApplicationReady: 在 ContributionDispatcher 写完 resource_relations 之后)。 */
    @EventListener(ApplicationReadyEvent.class)
    public void load() {
        Map<String, List<AnchorRow>> byResource = new HashMap<>();
        jdbc.query(
            "SELECT resource_code, relation_code, storage_kind, column_name " +
            "FROM resource_relations WHERE enabled = 1 AND tenant_id = 1",
            (java.sql.ResultSet rs) -> {
                byResource
                    .computeIfAbsent(rs.getString("resource_code"), k -> new ArrayList<>())
                    .add(new AnchorRow(rs.getString("relation_code"),
                            StorageKind.fromCode(rs.getString("storage_kind")),
                            rs.getString("column_name")));
            });
        Map<String, DerivedAnchor> fresh = new HashMap<>();
        byResource.forEach((code, rows) -> fresh.put(code, deriveAnchor(rows)));
        cache.clear();
        cache.putAll(fresh);
        log.info("[ResourceRelationRegistry] 加载 {} 个资源的锚点 (来自 resource_relations {} 行)",
                fresh.size(), byResource.values().stream().mapToInt(List::size).sum());
    }

    /** 资源的派生锚点; 未注册 → empty (调用方 buildMeta 走注解兜底)。 */
    public Optional<DerivedAnchor> forResource(String resourceCode) {
        return Optional.ofNullable(cache.get(resourceCode));
    }

    /** 配置变更后重载缓存。 */
    public void refresh() {
        load();
    }
}
