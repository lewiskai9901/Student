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
 *  - {@link Contribution} sealed, permits 恰好 16 种 (统一锚定模型 R1: +resource-relation)
 *  - 每种 permitted Contribution 都实现 uniqueKey()
 *  - 已无 @Deprecated 声明型 SPI; EntityTypePlugin 是合法保留的扩展 SPI
 *  - CoreManifest / EducationManifest 现在都是 PluginPackage 实例
 */
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
    @DisplayName("Contribution 恰好有 16 个 permitted 子类型")
    void contributionPermitsExactly16Subtypes() {
        Class<?>[] permitted = Contribution.class.getPermittedSubclasses();
        assertEquals(16, permitted.length,
            "15 (双轨收敛收官) + 1 ResourceRelationContribution (统一锚定模型 R1). 共 16 种: " +
            "relation/event-domain/trigger-point/event-type/perm/role/role-scope/role-perm/" +
            "menu/data-scope/data-resource/resource-relation/policy/target-mode/domain/workflow. " +
            "实际=" + permitted.length);
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
    @DisplayName("双轨收敛收官: 已无 @Deprecated 声明型 SPI; EntityTypePlugin 是合法保留的扩展 SPI 不标 @Deprecated")
    void noDeprecatedDeclarationSpiRemains() {
        // 双轨收敛收官 (2026-06-13): 6 个声明型 SPI (RelationType/DataScope/RolePreset/Menu/
        // Permission/MessagingDomain) 全删。EntityTypePlugin 携带生命周期行为 (beforeCreate/
        // afterCreate/validate), 是合法的 bean SPI (同 Policy/TargetModeResolver), 不再标
        // @Deprecated — 它不是过渡债务而是稳定设计, 无替代品可迁。
        assertFalse(EntityTypePlugin.class.isAnnotationPresent(Deprecated.class),
            "EntityTypePlugin 是稳定保留的扩展 SPI, 不应再标 @Deprecated");
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
    @DisplayName("PluginPackage 默认 metadata() 非 null; CoreManifest.contribute() 含 326 个 contribution (完美重构 P1: +inspection_submission 码+锚)")
    void pluginPackageDefaultMethods() {
        PluginPackage core = new CoreManifest();
        // Phase 2 W2.2: CoreManifest 已覆盖 contribute() 声明 9 个核心关系 (CoreRelationsPlugin 已删).
        // Phase 3 W3.1: 加 viewer + responsible_for 各 3 个 (覆盖 user/org/place) → 9+6=15.
        // Phase 5: 加 2 个 WorkflowContribution (leave-approval + hello-world) → 15+2=17.
        // Phase 6 (workflow-engine): 加 access-relation-approval workflow → 17+1=18.
        // Casbin 缺口修复 (2026-06-12): 加 TENANT_ADMIN RolePermissionBindingContribution
        // (23 + 菜单对齐补 3 个 workflow 查看码 = 26) → 18+26=44.
        // Phase1 双轨收敛: 加 19 个 DataResourceContribution → 44+19=63.
        // Role 双轨收敛: 加 3 个 RoleContribution → 63+3=66.
        // Menu 双轨收敛: 加 8 个 MenuContribution → 66+8=74.
        // Permission 双轨收敛: 加 223 个 PermissionContribution (聚合 CorePermissionCatalog) → 74+223=297.
        // R4: inspection_record 加 reviewer RECORD_RELATION 关系 → resource-relation 23→24 → 321+1=322.
        // R3c P2: inspection_record 加 inspected PROVIDER 关系 (受检面) → resource-relation 24→25 → 322+1=323.
        // 完美重构 P1: +inspection_submission data-resource(+1 → 21) + 其 owner_org/creator/inspected(COLUMN) 3 关系,
        //   删 inspection_record 的 inspected PROVIDER(-1) → resource-relation 25-1+3=27 → 323+1+2=326.
        // 完美重构 P2: inspection_task(data-resource 已存在) +owner_org/creator/reviewer/inspector 4 COLUMN 关系,
        //   删 inspection_record 的 reviewer RECORD_RELATION(-1) → resource-relation 27-1+4=30 → 326+3=329.
        // 完美重构 P3: 余表 3 资源码(evidence/submission_detail/project_inspector)各 +dr +owner_org/creator →
        //   data-resource 21→24(+3), resource-relation 30→36(+6) → 329+9=338.
        // 旧测试期望"默认空流"已不再适用; 改为校验内容契约.
        long count = core.contribute().count();
        assertEquals(338, count, "CoreManifest 应贡献 338 个 contribution (15 关系 + 3 workflow + 26 TENANT_ADMIN + 24 data-resource + 36 resource-relation + 3 role + 8 menu + 223 permission)");
        long rolePermCount = new CoreManifest().contribute()
            .filter(c -> c instanceof Contribution.RolePermissionBindingContribution)
            .count();
        assertEquals(26, rolePermCount, "TENANT_ADMIN 默认功能权限应为 26 条");
        assertNotNull(core.metadata(), "默认 metadata() 必须非 null");
        assertEquals("CORE", core.metadata().industryCode());
    }

    // ═══════════════════════ Phase 7 ═══════════════════════

    @Test
    @DisplayName("Phase 7.5: PluginPackage.configSchema() 默认空, 可被覆盖")
    void pluginPackageConfigSchemaDefaultEmpty() {
        PluginPackage core = new CoreManifest();
        var schema = core.configSchema();
        assertNotNull(schema, "默认 configSchema() 必须非 null");
        assertTrue(schema.isEmpty(), "CoreManifest 没覆盖时 configSchema() 应该空");
    }
}
