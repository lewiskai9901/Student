package com.school.management.infrastructure.extension;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 插件包 Registrar — 启动时最先执行 (@Order(50)),用于:
 *  1. 扫描所有 {@link PluginManifest} 实现
 *  2. UPSERT 到 plugin_packages 表
 *  3. 建立全局 industry 解析器(供其他 Registrar 查询某插件类属于哪个行业)
 *
 * 其他 Registrar 注入本 bean,调用 {@link #resolveIndustry(Class)} 给新记录打 industry 标签.
 */
@Slf4j
@Component
@Order(50)
@RequiredArgsConstructor
public class PluginPackageRegistrar implements ApplicationRunner {

    private final List<PluginManifest> manifests;
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    @Getter
    private volatile List<PluginManifest> sortedManifests = List.of();

    @Override
    public void run(ApplicationArguments args) {
        if (manifests.isEmpty()) {
            log.warn("[PluginPackageRegistrar] 无 Manifest 注册,CORE 应至少提供一个");
            return;
        }
        List<PluginManifest> sorted = topologicalSort(manifests);
        this.sortedManifests = sorted;
        validateVersionCompatibility(sorted);  // Phase 8: SemVer 验证

        int created = 0, updated = 0;
        for (PluginManifest m : sorted) {
            try {
                boolean isNew = upsert(m);
                if (isNew) created++; else updated++;
            } catch (Exception e) {
                log.error("[PluginPackageRegistrar] 写入 plugin_packages 失败 {}: {}",
                    m.getIndustryCode(), e.getMessage());
            }
        }
        log.info("[PluginPackageRegistrar] 已加载 {} 个行业包 (新增 {}, 更新 {}) — 启动顺序: {}",
            sorted.size(), created, updated,
            sorted.stream().map(PluginManifest::getIndustryCode).toList());
    }

    /**
     * 给定一个插件类,返回它归属的行业码.
     * 供 EntityTypeRegistrar / RelationTypeRegistrar 等在 UPSERT 时打标签用.
     */
    public String resolveIndustry(Class<?> pluginClass) {
        if (pluginClass == null) return null;
        for (PluginManifest m : sortedManifests) {
            if (m.owns(pluginClass)) return m.getIndustryCode();
        }
        return null;
    }

    /**
     * 合成统一 origin 字符串 — Phase 1 的统一来源字段.
     *
     * 返回格式:
     *   "PLUGIN:CORE@1.0.0"  — 插件主流声明
     *   "PLUGIN:EDU@1.0.0"
     *   null                 — 无 industry 解析成功 (说明插件未归属)
     *
     * 管理员自创的 origin (TENANT:CUSTOM#<id>) 由创建接口直接写,
     * 不走此方法.
     */
    public String resolveOrigin(Class<?> pluginClass) {
        String industry = resolveIndustry(pluginClass);
        if (industry == null) return null;
        String version = resolveVersion(industry);
        return "PLUGIN:" + industry + "@" + (version != null ? version : "1.0.0");
    }

    /** 同上, 基于 registeredBy 字符串(供 RelationTypePlugin 用) */
    public String resolveOriginBySource(String source) {
        String industry = resolveIndustryBySource(source);
        if (industry == null) return null;
        String version = resolveVersion(industry);
        return "PLUGIN:" + industry + "@" + (version != null ? version : "1.0.0");
    }

    /** 按 industry 码查版本号 (来自 Manifest 声明) */
    private String resolveVersion(String industry) {
        if (industry == null) return null;
        for (PluginManifest m : sortedManifests) {
            if (industry.equals(m.getIndustryCode())) return m.getVersion();
        }
        return null;
    }

    /**
     * 按 source/registeredBy 字符串解析行业 (供关系插件等用).
     * "CORE" → CORE, "EducationPlugin"/"EducationRelationsPlugin" → EDU
     */
    public String resolveIndustryBySource(String source) {
        if (source == null) return null;
        if ("CORE".equals(source)) return "CORE";
        String lower = source.toLowerCase();
        // 先试别名映射
        if (lower.contains("education")) return "EDU";
        if (lower.contains("eldercare")) return "CARE";
        for (PluginManifest m : sortedManifests) {
            if (m.getIndustryCode().equals("CORE")) continue;
            if (lower.contains(m.getIndustryCode().toLowerCase())) {
                return m.getIndustryCode();
            }
        }
        return null;
    }

    // ─── 内部 ───

    private boolean upsert(PluginManifest m) throws Exception {
        Long exists = jdbc.queryForObject(
            "SELECT COUNT(1) FROM plugin_packages WHERE industry_code=?",
            Long.class, m.getIndustryCode());

        String dependsOnJson = objectMapper.writeValueAsString(m.getDependsOn());
        String manifestClass = m.getClass().getName();

        if (exists == null || exists == 0) {
            jdbc.update(
                "INSERT INTO plugin_packages (industry_code, industry_name, version, depends_on, " +
                "manifest_class, enabled, uninstall_policy, installed_at, last_started_at) " +
                "VALUES (?, ?, ?, ?, ?, 1, ?, NOW(), NOW())",
                m.getIndustryCode(), m.getIndustryName(), m.getVersion(),
                dependsOnJson, manifestClass, m.getUninstallPolicy().name());
            return true;
        } else {
            jdbc.update(
                "UPDATE plugin_packages SET industry_name=?, version=?, depends_on=?, " +
                "manifest_class=?, uninstall_policy=?, last_started_at=NOW() " +
                "WHERE industry_code=?",
                m.getIndustryName(), m.getVersion(), dependsOnJson,
                manifestClass, m.getUninstallPolicy().name(), m.getIndustryCode());
            return false;
        }
    }

    /**
     * Phase 8: SemVer 兼容性验证.
     * 遍历每个 Manifest 的 getDependsOnWithVersion(), 对每条 (dep, range):
     *   - 如 dep 不存在 → IllegalStateException (fail-fast)
     *   - 如 dep 版本不匹配 range → IllegalStateException
     */
    private void validateVersionCompatibility(List<PluginManifest> manifests) {
        java.util.Map<String, String> codeToVersion = new java.util.HashMap<>();
        for (PluginManifest m : manifests) codeToVersion.put(m.getIndustryCode(), m.getVersion());

        for (PluginManifest m : manifests) {
            java.util.Map<String, String> deps = m.getDependsOnWithVersion();
            if (deps == null || deps.isEmpty()) continue;
            for (var entry : deps.entrySet()) {
                String depCode = entry.getKey();
                String range = entry.getValue();
                String actual = codeToVersion.get(depCode);
                if (actual == null) {
                    throw new IllegalStateException(String.format(
                        "插件 %s 依赖 %s%s, 但 %s 未安装",
                        m.getIndustryCode(), depCode,
                        (range == null || range.isBlank() ? "" : "@" + range), depCode));
                }
                if (!SemVer.satisfies(actual, range)) {
                    throw new IllegalStateException(String.format(
                        "插件 %s (v%s) 要求 %s@%s, 但实际是 %s@%s (版本不兼容)",
                        m.getIndustryCode(), m.getVersion(), depCode, range, depCode, actual));
                }
                log.info("  版本兼容: {} (v{}) ⇒ 依赖 {}@{} 满足 (实际 v{})",
                    m.getIndustryCode(), m.getVersion(), depCode, range, actual);
            }
        }
    }

    /** 拓扑排序 - 依赖方先启动 */
    private List<PluginManifest> topologicalSort(List<PluginManifest> input) {
        java.util.Map<String, PluginManifest> byCode = new java.util.HashMap<>();
        for (PluginManifest m : input) byCode.put(m.getIndustryCode(), m);

        List<PluginManifest> result = new java.util.ArrayList<>();
        java.util.Set<String> visited = new java.util.HashSet<>();
        java.util.Set<String> visiting = new java.util.HashSet<>();

        for (PluginManifest m : input) visit(m, byCode, result, visited, visiting);
        return result;
    }

    private void visit(PluginManifest m, java.util.Map<String, PluginManifest> byCode,
                       List<PluginManifest> result, java.util.Set<String> visited,
                       java.util.Set<String> visiting) {
        String code = m.getIndustryCode();
        if (visited.contains(code)) return;
        if (visiting.contains(code)) {
            throw new IllegalStateException("Plugin 依赖存在循环: " + code);
        }
        visiting.add(code);
        for (String dep : m.getDependsOn()) {
            PluginManifest depM = byCode.get(dep);
            if (depM == null) {
                log.warn("[PluginPackageRegistrar] {} 依赖 {} 但未找到,跳过依赖", code, dep);
                continue;
            }
            visit(depM, byCode, result, visited, visiting);
        }
        visiting.remove(code);
        visited.add(code);
        result.add(m);
    }
}
