package com.school.management.infrastructure.extension;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Plugin Registrar 的通用抽象基类 — 消除 7 个 Registrar 的重复代码.
 *
 * 子类职责:
 *   1. 通过 getPluginList() 提供 SPI 实例列表 (由 Spring 注入)
 *   2. 通过 extractDefs() 从单个插件展开所有声明
 *   3. 通过 upsertOne() 把一条声明写入 DB (INSERT 或 UPDATE)
 *
 * 基类职责:
 *   1. 空插件列表时记日志并返回
 *   2. 逐插件 × 逐声明 循环调用,统计 created/updated
 *   3. 异常捕获,确保一条失败不影响整体
 *   4. 汇总日志
 *
 * CUSTOM 保护 / 冲突检测 / 行业解析 由子类通过 {@link RegistrarHelper} 复用.
 *
 * @param <P> Plugin SPI 类型 (如 PermissionProvider)
 * @param <D> 声明类型     (如 PermissionDef)
 */
@Slf4j
public abstract class AbstractPluginRegistrar<P, D> implements ApplicationRunner {

    protected final JdbcTemplate jdbc;
    protected final PluginPackageRegistrar packageRegistrar;

    protected AbstractPluginRegistrar(JdbcTemplate jdbc, PluginPackageRegistrar packageRegistrar) {
        this.jdbc = jdbc;
        this.packageRegistrar = packageRegistrar;
    }

    // ═════════════════════════ 公共模板 ═════════════════════════

    /** Phase 9: 记录每个 Registrar 的启动耗时 (ms) 供健康检查读取 */
    public static final Map<String, Long> STARTUP_DURATION_MS = new ConcurrentHashMap<>();
    public static final Map<String, Integer> STARTUP_DECLARATION_COUNT = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public final void run(ApplicationArguments args) {
        long startMs = System.currentTimeMillis();
        List<P> plugins = getPluginList();
        if (plugins == null || plugins.isEmpty()) {
            log.info("[{}] 无插件需要注册", registrarName());
            STARTUP_DURATION_MS.put(registrarName(), System.currentTimeMillis() - startMs);
            STARTUP_DECLARATION_COUNT.put(registrarName(), 0);
            return;
        }

        // ─── 冲突检测 (Phase 3.2) ───
        // 跨插件 ⇒ 相同 uniqueKey 由不同 origin 声明 = 冲突,启动 fail-fast
        Map<String, String> keyToOrigin = new HashMap<>();
        for (P plugin : plugins) {
            String origin = resolveOrigin(plugin);
            if (origin == null) continue;
            for (D def : extractDefs(plugin)) {
                String key = uniqueKey(plugin, def);
                if (key == null) continue;
                String prev = keyToOrigin.get(key);
                if (prev != null && !prev.equals(origin)) {
                    throw new IllegalStateException(String.format(
                        "[%s] 插件冲突: 声明 %s 同时被 %s 和 %s 声明,启动中止 (同一标识不能跨行业重复声明)",
                        registrarName(), key, prev, origin));
                }
                keyToOrigin.put(key, origin);
            }
        }

        // ─── 事务性 UPSERT (Phase 3.1) ───
        int total = 0, created = 0, updated = 0, skipped = 0;
        for (P plugin : plugins) {
            String pluginClass = plugin.getClass().getName();
            String industry = resolveIndustry(plugin);
            List<D> defs = extractDefs(plugin);
            for (D def : defs) {
                total++;
                try {
                    UpsertResult r = upsertOne(plugin, def, industry, pluginClass);
                    switch (r) {
                        case CREATED -> created++;
                        case UPDATED -> updated++;
                        case SKIPPED -> skipped++;
                    }
                } catch (Exception e) {
                    log.error("[{}] 写入失败 {}: {}", registrarName(), describeDef(def), e.getMessage());
                    throw new RuntimeException("Registrar 事务回滚: " + e.getMessage(), e);
                }
            }
        }
        long durationMs = System.currentTimeMillis() - startMs;
        STARTUP_DURATION_MS.put(registrarName(), durationMs);
        STARTUP_DECLARATION_COUNT.put(registrarName(), total);
        log.info("[{}] 扫描 {} 个插件,{} 条声明 (新增 {} / 更新 {} / 跳过 {}) 耗时 {} ms",
            registrarName(), plugins.size(), total, created, updated, skipped, durationMs);

        afterSync(plugins);  // hook for event publishing
    }

    // ═════════════════════════ 冲突检测契约 ═════════════════════════

    /**
     * 生成一条声明的唯一键 (用于跨插件冲突检测).
     * 默认用 {@code describeDef(def)} (包含 entityType + typeCode 或其他语义标识).
     * 子类可以重写更精确.
     */
    protected String uniqueKey(P plugin, D def) {
        return describeDef(def);
    }

    /** Hook: 所有插件处理完后调用, 供子类发 ApplicationEvent 等 */
    protected void afterSync(List<P> plugins) {}

    // ═════════════════════════ 子类必须实现 ═════════════════════════

    /** 本 Registrar 的名称, 用于日志 (通常 getClass().getSimpleName()) */
    protected String registrarName() { return getClass().getSimpleName(); }

    /** 所有插件实例列表 (通常由 Spring 注入 List<P>) */
    protected abstract List<P> getPluginList();

    /** 从单个插件中展开所有声明 */
    protected abstract List<D> extractDefs(P plugin);

    /** 把一条声明 UPSERT 到 DB, 返回状态 */
    protected abstract UpsertResult upsertOne(P plugin, D def, String industry, String pluginClass) throws Exception;

    /** 用于错误日志的声明描述 */
    protected String describeDef(D def) { return String.valueOf(def); }

    // ═════════════════════════ 可选重写 ═════════════════════════

    /** 默认走 PluginPackageRegistrar.resolveIndustry; 关系插件可重写走 resolveIndustryBySource */
    protected String resolveIndustry(P plugin) {
        return packageRegistrar.resolveIndustry(plugin.getClass());
    }

    /**
     * 合成 Phase 1 origin 字符串 (PLUGIN:<industry>@<version>).
     * 子类写 DB 时必须传入 origin, 不再双写 industry+plugin_class.
     */
    protected String resolveOrigin(P plugin) {
        return packageRegistrar.resolveOrigin(plugin.getClass());
    }

    // ═════════════════════════ 公用工具 (子类 upsert 中调用) ═════════════════════════

    /**
     * 若该记录已被标记 industry='CUSTOM' (管理员自创),不覆盖.
     * 返回 true 表示应跳过.
     */
    protected boolean isCustomProtected(String selectExistingIndustrySql, Object... params) {
        String existing = jdbc.query(selectExistingIndustrySql,
            rs -> rs.next() ? rs.getString(1) : null, params);
        return "CUSTOM".equals(existing);
    }

    /** UPSERT 结果 */
    protected enum UpsertResult { CREATED, UPDATED, SKIPPED }
}
