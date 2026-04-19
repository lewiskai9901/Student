package com.school.management.infrastructure.extension.plugins.healthcare;

import com.school.management.infrastructure.extension.PluginPackage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 医疗行业插件包 (示例骨架) — 展示如何新增行业扩展.
 *
 * 当前规模: 1 个 EntityTypePlugin (Patient) + 1 个 MenuContributionPlugin,
 * 足以证明 CORE + EDU + HEALTH 三方共存时启动/治理 API/前端加载都正常.
 *
 * 真实医疗场景应继续扩展: 病历/药品/床位/排班/医嘱 等.
 */
@Component
public class HealthcareManifest implements PluginPackage {

    @Override
    public String getIndustryCode() { return "HEALTH"; }

    @Override
    public String getIndustryName() { return "医疗行业"; }

    @Override
    public List<String> getDependsOn() { return List.of("CORE"); }

    @Override
    public Map<String, String> getDependsOnWithVersion() {
        return Map.of("CORE", ">=1.0.0 <2.0.0");
    }

    @Override
    public boolean owns(Class<?> pluginClass) {
        return pluginClass.getPackageName().contains(".plugins.healthcare");
    }
}
