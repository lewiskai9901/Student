package com.school.management.infrastructure.extension;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 特性注册表 —— 特性词汇表的单一权威来源。
 *
 * <p>启动时汇总所有 {@link FeatureDefProvider} (核心 + 各启用行业插件) 声明的特性, 提供:
 * 中文名/说明查询、归属查询、合法性校验。供 PluginRegistrar 启动治理 (未登记/跨行业 fail-fast)、
 * plugin-platform API (前端读取标签/说明/归属) 共用。插件禁用时其 provider bean 不存在 → 其特性自动不在册。
 */
@Slf4j
@Component
public class FeatureRegistry {

    /** 一条登记: 定义 + 归属行业。 */
    public record Entry(FeatureDef def, String owner) {}

    private final Map<String, Entry> byCode = new LinkedHashMap<>();

    public FeatureRegistry(List<FeatureDefProvider> providers) {
        for (FeatureDefProvider p : providers) {
            String owner = p.industry();
            for (FeatureDef f : p.features()) {
                Entry prev = byCode.get(f.code());
                if (prev != null && !prev.owner().equals(owner)) {
                    // 同一特性码被两个行业声明 = 归属冲突, 启动 fail-fast (词汇表必须唯一归属)
                    throw new IllegalStateException(String.format(
                        "特性归属冲突: '%s' 同时被 %s 和 %s 声明 —— 一个特性码只能归属一个行业",
                        f.code(), prev.owner(), owner));
                }
                byCode.putIfAbsent(f.code(), new Entry(f, owner));
            }
        }
        log.info("[FeatureRegistry] 登记特性 {} 项 (来自 {} 个 provider)", byCode.size(), providers.size());
    }

    public boolean isKnown(String code) { return byCode.containsKey(code); }

    /** 特性归属行业 (未登记返 null)。 */
    public String ownerOf(String code) {
        Entry e = byCode.get(code);
        return e == null ? null : e.owner();
    }

    public Collection<Entry> all() { return byCode.values(); }
}
