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
 * {@link ResourceScopeMeta} 所需的全部锚定字段 (orgUnitField / creatorField / viaMembership /
 * membershipSubjectColumn)。
 *
 * <p>R2.2 把 {@code DataPermissionInterceptor.buildMeta} 的锚点来源从「注解 ⊕ data_resources 列」
 * 切到本注册表 (R2.4 后注解兜底已删), 实现「锚点集中声明」。本类的 {@link #deriveAnchor} 是
 * 纯函数 (注册行 → 派生锚点), 与 DB 加载分离, 便于等价单测。
 *
 * <p><b>驱动全部锚定语义</b> (Tier2 收官): orgUnitField / creatorField / viaMembership /
 * membershipSubjectColumn —— 注册表是这些的唯一真相源。meta 余下两字段各得其所、非注册表职责:
 * tableAlias 来自注解 (查询级 SQL 形态, 同表多查询可异); typeField 来自 data_resources
 * (资源级配置, 非关系级)。
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
    /** R4: 每资源 relationCode → AnchorRow (per-relation storage_kind 查询; 引擎 RECORD_RELATION 分支用)。 */
    private final Map<String, Map<String, AnchorRow>> relationCache = new ConcurrentHashMap<>();
    /** R8 P3-INSERT: owner_org 标注 enforce_insert_scope=1 的资源 (其 INSERT 受 owner_org∈可写组织 授权)。 */
    private final java.util.Set<String> insertGuardedResources = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** 懒加载标志: 首次访问加载一次后置 true。volatile + loadLock 双检锁保证只加载一次。 */
    private volatile boolean loaded = false;
    private final Object loadLock = new Object();

    public ResourceRelationRegistry(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 一条 resource_relations 行里派生锚点所需的最小字段。{@code resolverBean} 仅 PROVIDER 存储非空;
     * {@code subjectColumn} 仅 owner_org SUBJECT_GRAPH 行用 (Tier2: 主表充当 ar.subject_id 的列, 可空)。
     */
    public record AnchorRow(String relationCode, StorageKind storageKind, String columnName,
                            String resolverBean, String subjectColumn) {}

    /** 从注册行派生出的锚点视图 (对应 ResourceScopeMeta 的锚定字段)。 */
    public record DerivedAnchor(boolean viaMembership, String orgUnitField, String creatorField,
                                String membershipSubjectColumn) {}

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
     *   <li>Tier2: owner_org SUBJECT_GRAPH 行的 {@code subjectColumn} → membershipSubjectColumn
     *       (主表充当 ar.subject_id 的列; null = 默认 id)。</li>
     * </ul>
     */
    public static DerivedAnchor deriveAnchor(List<AnchorRow> rows) {
        boolean viaMembership = false;
        String orgUnitField = null;
        String creatorField = null;
        String membershipSubjectColumn = null;
        for (AnchorRow row : rows) {
            if (OWNER_ORG.equals(row.relationCode())) {
                if (row.storageKind() == StorageKind.SUBJECT_GRAPH) {
                    viaMembership = true;   // 成员图: 组织来自 access_relations member, 无列锚
                    orgUnitField = null;
                    membershipSubjectColumn = row.subjectColumn();  // Tier2: subject 列归注册表
                } else if (row.storageKind() == StorageKind.COLUMN) {
                    viaMembership = false;
                    orgUnitField = row.columnName();
                }
            } else if (CREATOR.equals(row.relationCode())) {
                creatorField = row.columnName();
            }
        }
        return new DerivedAnchor(viaMembership, orgUnitField, creatorField, membershipSubjectColumn);
    }

    // ── DB 加载 + 缓存 ─────────────────────────────────────────────────────

    /**
     * DB 取数 + 按资源分组。抽成 protected 是测试 seam —— 单测可覆写返回 canned 行,
     * 免去 ResultSet mock; 生产实现走 {@code resource_relations} 查询。
     */
    protected Map<String, List<AnchorRow>> fetchByResource() {
        Map<String, List<AnchorRow>> byResource = new HashMap<>();
        jdbc.query(
            "SELECT resource_code, relation_code, storage_kind, column_name, resolver_bean, subject_column " +
            "FROM resource_relations WHERE enabled = 1 AND tenant_id = 1",
            (java.sql.ResultSet rs) -> {
                byResource
                    .computeIfAbsent(rs.getString("resource_code"), k -> new ArrayList<>())
                    .add(new AnchorRow(rs.getString("relation_code"),
                            StorageKind.fromCode(rs.getString("storage_kind")),
                            rs.getString("column_name"),
                            rs.getString("resolver_bean"),
                            rs.getString("subject_column")));
            });
        return byResource;
    }

    /**
     * 加载 resource_relations → 派生锚点 → 填缓存。
     *
     * <p><b>fail-fast</b> (统一锚定 P3): 取数为空 → 抛 {@link IllegalStateException}。
     * resource_relations 由 contribution 启动期写入 (重启级); 空集只可能是写入失败。
     * R2.4 删注解兜底后, 空注册表 = 数据权限全无锚点; 必须 fail-fast, 不得静默 fail-open。
     */
    private void doLoad() {
        Map<String, List<AnchorRow>> byResource = fetchByResource();
        if (byResource.isEmpty()) {
            throw new IllegalStateException(
                "resource_relations 注册表为空 — contribution 写入可能失败。" +
                "数据权限锚点无兜底, 拒绝以 fail-open 状态服务 (统一锚定 P3 fail-fast)。");
        }
        Map<String, DerivedAnchor> fresh = new HashMap<>();
        Map<String, Map<String, AnchorRow>> freshRel = new HashMap<>();
        byResource.forEach((code, rows) -> {
            fresh.put(code, deriveAnchor(rows));
            Map<String, AnchorRow> byRel = new HashMap<>();
            for (AnchorRow r : rows) byRel.put(r.relationCode(), r);
            freshRel.put(code, byRel);
        });
        cache.clear();
        cache.putAll(fresh);
        relationCache.clear();
        relationCache.putAll(freshRel);
        insertGuardedResources.clear();
        insertGuardedResources.addAll(fetchInsertGuardedResources());
        log.info("[ResourceRelationRegistry] 加载 {} 个资源的锚点 (来自 resource_relations {} 行), INSERT 授权资源 {} 个",
                fresh.size(), byResource.values().stream().mapToInt(List::size).sum(), insertGuardedResources.size());
    }

    /**
     * owner_org 标注 enforce_insert_scope=1 的资源集 (R8 P3-INSERT)。protected = 测试 seam;
     * jdbc 为空 (单测 mock 注册表) → 空集 (INSERT 授权不触发, fail-safe)。
     */
    protected java.util.Set<String> fetchInsertGuardedResources() {
        if (jdbc == null) return java.util.Set.of();
        return new java.util.HashSet<>(jdbc.queryForList(
            "SELECT resource_code FROM resource_relations WHERE relation_code = 'owner_org' " +
            "AND enforce_insert_scope = 1 AND enabled = 1 AND tenant_id = 1", String.class));
    }

    /** 该资源的 INSERT 是否受 owner_org∈可写组织 授权 (R8 P3-INSERT)。首次调用触发懒加载。 */
    public boolean isInsertGuarded(String resourceCode) {
        ensureLoaded();
        return insertGuardedResources.contains(resourceCode);
    }

    /**
     * 懒加载: 首次访问同步加载一次。双检锁消除「Tomcat 接客早于启动钩子、缓存空」窗口 ——
     * 任何请求都不会读到空缓存 (首个请求阻塞片刻完成加载)。
     */
    private void ensureLoaded() {
        if (loaded) return;
        synchronized (loadLock) {
            if (loaded) return;
            doLoad();
            loaded = true;
        }
    }

    /** 启动期全量加载 (ApplicationReady: 在 ContributionDispatcher 写完 resource_relations 之后)。 */
    @EventListener(ApplicationReadyEvent.class)
    public void load() {
        ensureLoaded();
    }

    /** 资源的派生锚点; 未注册 → empty (调用方处理)。首次调用触发懒加载。 */
    public Optional<DerivedAnchor> forResource(String resourceCode) {
        ensureLoaded();
        return Optional.ofNullable(cache.get(resourceCode));
    }

    /**
     * 某资源某关系的注册行 (R4: per-relation storage_kind 查询)。未注册 → empty。
     *
     * <p>引擎将来据 {@code storageKind()==RECORD_RELATION} 决定走 {@code record_relations} 子查询
     * (而非 owner_org/creator 的列/成员图路径)。{@link #forResource} 派生的 DerivedAnchor 只汇总
     * owner_org/creator 两关系; 本方法暴露<b>任意</b>关系 (reviewer/inspected/...) 的原始注册行。
     */
    public Optional<AnchorRow> relationOf(String resourceCode, String relationCode) {
        ensureLoaded();
        Map<String, AnchorRow> byRel = relationCache.get(resourceCode);
        return byRel == null ? Optional.empty() : Optional.ofNullable(byRel.get(relationCode));
    }

    /**
     * 某资源已注册的全部关系行 (R3c: 供数据权限 UI 多 grant 编辑器列出可选锚点关系)。
     * 未注册资源 → 空列表。数据驱动, 无行业硬编码 —— 关系来自 resource_relations 注册表。
     */
    public List<AnchorRow> relationsOf(String resourceCode) {
        ensureLoaded();
        Map<String, AnchorRow> byRel = relationCache.get(resourceCode);
        return byRel == null ? List.of() : new ArrayList<>(byRel.values());
    }

    /**
     * 全部 PROVIDER 关系的 {@code "resource/relation" -> resolver_bean} 映射 (R3c 守护)。
     * 供启动期校验每个 resolver bean 真存在 (否则运行期静默 fail-closed 拒绝, 难排查)。
     */
    public Map<String, String> providerResolverBeans() {
        ensureLoaded();
        Map<String, String> out = new java.util.LinkedHashMap<>();
        relationCache.forEach((resource, byRel) -> byRel.forEach((rel, row) -> {
            if (row.storageKind() == StorageKind.PROVIDER) {
                out.put(resource + "/" + rel, row.resolverBean());
            }
        }));
        return out;
    }

    /** 配置变更后强制重载缓存 (绕过 loaded 标志)。 */
    public void refresh() {
        synchronized (loadLock) {
            doLoad();
            loaded = true;
        }
    }
}
