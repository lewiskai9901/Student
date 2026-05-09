package com.school.management.architecture;

import com.school.management.infrastructure.extension.*;
import com.school.management.infrastructure.extension.plugins.core.CoreManifest;
import com.school.management.infrastructure.extension.plugins.education.EducationManifest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 2 统一 SPI 契约验证.
 *
 * 保证:
 *  - {@link PluginPackage} 作为顶层接口, 继承 PluginManifest
 *  - {@link Contribution} sealed, permits 恰好 11 种
 *  - 每种 permitted Contribution 都实现 uniqueKey()
 *  - 7 个旧 SPI 都打了 @Deprecated (强化向下兼容承诺)
 *  - CoreManifest / EducationManifest 现在都是 PluginPackage 实例
 */
@SuppressWarnings("deprecation")
class UnifiedPluginPackageTest {

    @Test
    @DisplayName("PluginPackage 应为 interface 且继承 PluginManifest")
    void pluginPackageIsInterfaceAndExtendsManifest() {
        assertTrue(PluginPackage.class.isInterface(), "PluginPackage 必须是接口");
        assertTrue(PluginManifest.class.isAssignableFrom(PluginPackage.class),
            "PluginPackage 必须继承 PluginManifest (元数据桥接)");
    }

    @Test
    @DisplayName("PluginPackage 应有 metadata() 和 contribute() 两个方法")
    void pluginPackageHasRequiredMethods() {
        Set<String> methodNames = Arrays.stream(PluginPackage.class.getDeclaredMethods())
            .map(Method::getName)
            .collect(Collectors.toSet());
        assertTrue(methodNames.contains("metadata"),
            "PluginPackage 应声明 metadata() 方法");
        assertTrue(methodNames.contains("contribute"),
            "PluginPackage 应声明 contribute() 方法");
    }

    @Test
    @DisplayName("Contribution 必须是 sealed interface")
    void contributionIsSealed() {
        assertTrue(Contribution.class.isSealed(),
            "Contribution 必须用 sealed 关键字限定扩展");
    }

    @Test
    @DisplayName("Contribution 恰好有 13 个 permitted 子类型")
    void contributionPermitsExactly13Subtypes() {
        Class<?>[] permitted = Contribution.class.getPermittedSubclasses();
        assertEquals(13, permitted.length,
            "Track M3 扩展后约定 13 种 Contribution: entity/relation/event-domain/trigger-point/event-type/perm/role/menu/data-scope/route/policy/target-mode/domain, 实际=" + permitted.length);
    }

    @Test
    @DisplayName("每种 permitted Contribution 子类型都是 record 且实现 uniqueKey()")
    void permittedSubtypesAreRecordsWithUniqueKey() {
        for (Class<?> sub : Contribution.class.getPermittedSubclasses()) {
            assertTrue(sub.isRecord(), sub.getSimpleName() + " 必须是 record (immutable)");
            boolean hasUniqueKey = Arrays.stream(sub.getMethods())
                .anyMatch(m -> "uniqueKey".equals(m.getName()) && m.getParameterCount() == 0
                    && String.class.equals(m.getReturnType()));
            assertTrue(hasUniqueKey,
                sub.getSimpleName() + " 必须实现 String uniqueKey()");
        }
    }

    @Test
    @DisplayName("剩余 6 个旧 SPI 必须打 @Deprecated (迁移提示) — RelationTypePlugin 已删除 (W2.2)")
    void oldSpisAreDeprecated() {
        // Phase 2 W2.2: RelationTypePlugin 已删除并完成迁移 (commit refactor(access)).
        // 剩余 6 个 SPI 仍向下兼容, 需保持 @Deprecated 标注.
        List<Class<?>> oldSpis = List.of(
            EntityTypePlugin.class,
            MessagingDomainPlugin.class,
            PermissionProvider.class,
            RolePresetPlugin.class,
            DataScopePlugin.class,
            MenuContributionPlugin.class
        );
        for (Class<?> c : oldSpis) {
            assertTrue(c.isAnnotationPresent(Deprecated.class),
                c.getSimpleName() + " 应打 @Deprecated 指向 PluginPackage.contribute()");
            Deprecated d = c.getAnnotation(Deprecated.class);
            assertFalse(d.forRemoval(),
                c.getSimpleName() + " 暂不可 forRemoval=true (Phase 2 保留向下兼容)");
        }
    }

    @Test
    @DisplayName("PluginManifest 不可被标记 @Deprecated (PluginPackage 继承它, metadata 还在用)")
    void pluginManifestNotDeprecated() {
        assertFalse(PluginManifest.class.isAnnotationPresent(Deprecated.class),
            "PluginManifest 是 PluginPackage 的父接口, 不可废弃");
    }

    @Test
    @DisplayName("CoreManifest 必须是 PluginPackage 实例")
    void coreManifestIsPluginPackage() {
        assertTrue(PluginPackage.class.isAssignableFrom(CoreManifest.class),
            "CoreManifest 应 implements PluginPackage (Phase 2 升级)");
    }

    @Test
    @DisplayName("EducationManifest 必须是 PluginPackage 实例")
    void educationManifestIsPluginPackage() {
        assertTrue(PluginPackage.class.isAssignableFrom(EducationManifest.class),
            "EducationManifest 应 implements PluginPackage (Phase 2 升级)");
    }

    @Test
    @DisplayName("PluginMetadata.of(PluginManifest) 桥接正确")
    void pluginMetadataOfBridge() {
        PluginManifest core = new CoreManifest();
        PluginMetadata meta = PluginMetadata.of(core);
        assertEquals("CORE", meta.industryCode());
        assertEquals(core.getIndustryName(), meta.industryName());
        assertEquals(core.getVersion(), meta.version());
        assertNotNull(meta.uninstallPolicy());
    }

    @Test
    @DisplayName("PluginPackage 默认 metadata() 非 null; CoreManifest.contribute() 含 15 个核心关系 (W3.1 扩充)")
    void pluginPackageDefaultMethods() {
        PluginPackage core = new CoreManifest();
        // Phase 2 W2.2: CoreManifest 已覆盖 contribute() 声明 9 个核心关系 (CoreRelationsPlugin 已删).
        // Phase 3 W3.1: 加 viewer + responsible_for 各 3 个 (覆盖 user/org/place) → 9+6=15.
        // 旧测试期望"默认空流"已不再适用; 改为校验内容契约.
        long count = core.contribute().count();
        assertEquals(15, count, "CoreManifest 应贡献 15 个核心关系类型 (9 base + 3 viewer + 3 responsible_for)");
        assertNotNull(core.metadata(), "默认 metadata() 必须非 null");
        assertEquals("CORE", core.metadata().industryCode());
    }

    // ═══════════════════════ Phase 7 ═══════════════════════

    @Test
    @DisplayName("Phase 7.1: RouteContribution 进入 permitted 清单")
    void routeContributionInPermittedList() {
        boolean found = java.util.Arrays.stream(Contribution.class.getPermittedSubclasses())
            .anyMatch(c -> c.getSimpleName().equals("RouteContribution"));
        assertTrue(found, "RouteContribution 必须在 sealed permits 清单里");
    }

    @Test
    @DisplayName("Phase 7.5: PluginPackage.configSchema() 默认空, 可被覆盖")
    void pluginPackageConfigSchemaDefaultEmpty() {
        PluginPackage core = new CoreManifest();
        var schema = core.configSchema();
        assertNotNull(schema, "默认 configSchema() 必须非 null");
        assertTrue(schema.isEmpty(), "CoreManifest 没覆盖时 configSchema() 应该空");
    }

    @Test
    @DisplayName("Phase 7.4: PluginPackage.schemaVersion() 默认 0")
    void schemaVersionDefault() {
        assertEquals(0, new CoreManifest().schemaVersion(), "CoreManifest 未覆盖 schemaVersion() 应返回 0");
    }
}
