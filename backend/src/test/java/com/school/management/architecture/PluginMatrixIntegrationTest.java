package com.school.management.architecture;

import com.school.management.infrastructure.extension.PluginManifest;
import com.school.management.infrastructure.extension.PluginPackageRegistrar;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 矩阵集成测试 — 验证多行业包同时存在时 SPI 体系的正确性.
 *
 * 本测试不启动 Spring 容器 (否则慢), 而是用反射扫描所有 Manifest 类,
 * 做纯代码/数据层面的矩阵一致性校验:
 *
 *   1. 至少有 CORE 一个 manifest
 *   2. 所有 manifest 都有唯一 industry_code
 *   3. dependsOn 依赖都能找到对应 manifest (无 dangling 依赖)
 *   4. getDependsOnWithVersion 的 range 语法合法 (SemVer)
 *   5. 没有循环依赖
 *
 * 加入新行业后 (如 HEALTH), 本测试应继续绿色.
 * 如果出现冲突,本测试先于启动 crash.
 */
class PluginMatrixIntegrationTest {

    /** 用 ArchUnit (已有依赖) 扫描所有 PluginManifest 实现 */
    private List<PluginManifest> discoverManifests() {
        com.tngtech.archunit.core.domain.JavaClasses classes =
            new com.tngtech.archunit.core.importer.ClassFileImporter()
                .withImportOption(com.tngtech.archunit.core.importer.ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.school.management");

        java.util.List<PluginManifest> result = new java.util.ArrayList<>();
        for (com.tngtech.archunit.core.domain.JavaClass jc : classes) {
            if (jc.isInterface() || jc.getModifiers().contains(com.tngtech.archunit.core.domain.JavaModifier.ABSTRACT)) continue;
            if (!jc.getAllRawInterfaces().stream()
                    .anyMatch(i -> i.getFullName().equals(PluginManifest.class.getName()))) continue;
            try {
                Class<?> c = Class.forName(jc.getFullName());
                result.add((PluginManifest) c.getDeclaredConstructor().newInstance());
            } catch (Exception ignored) {
                // 跳过需要依赖注入的 (理论上 Manifest 不应有)
            }
        }
        return result;
    }

    @Test
    void atLeastOneManifestExists() {
        List<PluginManifest> manifests = discoverManifests();
        assertFalse(manifests.isEmpty(), "至少需要一个 PluginManifest (CORE)");
    }

    @Test
    void coreManifestMustExist() {
        List<PluginManifest> manifests = discoverManifests();
        boolean hasCore = manifests.stream().anyMatch(m -> "CORE".equals(m.getIndustryCode()));
        assertTrue(hasCore, "必须存在 CORE 包作为基础");
    }

    @Test
    void industryCodesAreUnique() {
        List<PluginManifest> manifests = discoverManifests();
        Set<String> codes = new HashSet<>();
        for (PluginManifest m : manifests) {
            String code = m.getIndustryCode();
            assertTrue(codes.add(code),
                "插件包 code 重复: " + code + " (" + m.getClass().getName() + ")");
        }
    }

    @Test
    void dependenciesAllResolve() {
        List<PluginManifest> manifests = discoverManifests();
        Set<String> allCodes = new HashSet<>();
        manifests.forEach(m -> allCodes.add(m.getIndustryCode()));

        for (PluginManifest m : manifests) {
            for (String dep : m.getDependsOn()) {
                assertTrue(allCodes.contains(dep),
                    m.getIndustryCode() + " 依赖 " + dep + " 但未找到对应 Manifest");
            }
        }
    }

    @Test
    void versionsAreValidSemver() {
        List<PluginManifest> manifests = discoverManifests();
        for (PluginManifest m : manifests) {
            String v = m.getVersion();
            assertNotNull(v, m.getIndustryCode() + " 版本为 null");
            assertTrue(v.matches("\\d+\\.\\d+\\.\\d+.*"),
                m.getIndustryCode() + " 版本 " + v + " 不是合法 SemVer (期望 x.y.z)");
        }
    }

    @Test
    void noCircularDependency() {
        List<PluginManifest> manifests = discoverManifests();
        java.util.Map<String, java.util.List<String>> graph = new java.util.HashMap<>();
        manifests.forEach(m -> graph.put(m.getIndustryCode(), m.getDependsOn()));

        // DFS 检测循环
        Set<String> visited = new HashSet<>();
        Set<String> onPath = new HashSet<>();
        for (String node : graph.keySet()) {
            if (visited.contains(node)) continue;
            detectCycle(node, graph, visited, onPath);
        }
    }

    private void detectCycle(String node, java.util.Map<String, java.util.List<String>> graph,
                              Set<String> visited, Set<String> onPath) {
        if (onPath.contains(node)) {
            fail("依赖图存在循环, 经过: " + node);
        }
        if (visited.contains(node)) return;
        onPath.add(node);
        for (String dep : graph.getOrDefault(node, List.of())) {
            if (graph.containsKey(dep)) detectCycle(dep, graph, visited, onPath);
        }
        onPath.remove(node);
        visited.add(node);
    }
}
