# Phase 2 统一 SPI 顶层协议 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 把 8 个 SPI 接口 (`PluginManifest` + `EntityTypePlugin`/`RelationTypePlugin`/`MessagingDomainPlugin`/`PermissionProvider`/`RolePresetPlugin`/`DataScopePlugin`/`MenuContributionPlugin`) 收敛为一个 `PluginPackage` + `sealed Contribution` 契约，旧 7 SPI 保留 `@Deprecated` 向下兼容，零破坏上线。

**Architecture:** 新 `PluginPackage` 继承自 `PluginManifest`（default `metadata()` 从父接口桥接，default `contribute()` 返回空）。新 `Contribution` sealed interface 8 个 permitted 记录，分别封装 7 个旧 SPI 的 def 类型 + 一个 `DomainContribution` 作为未来扩展占位。新 `ContributionDispatcher`（`@Order(60)`）在 `PluginPackageRegistrar (50)` 之后、所有其他 Registrar (100+) 之前运行，逐个 Contribution 复用现有 Registrar 的 upsert 逻辑。现有 `CoreManifest`/`EducationManifest` 只改实现接口为 `PluginPackage`，业务代码和数据 0 改动。

**Tech Stack:** Java 17（用 sealed + records + pattern-matching switch），Spring Boot 3.2 的 `ApplicationRunner` + `@Order`，ArchUnit 1.x 用于结构测试。

---

## 关键背景

- 当前 8 SPI 文件：
  - `PluginManifest.java` — 行业包元数据（CORE / EDU）
  - `EntityTypePlugin.java` — 实体类型
  - `RelationTypePlugin.java` — 关系类型
  - `MessagingDomainPlugin.java` — 触发点 + 事件类型 + 默认触发器
  - `PermissionProvider.java` — 权限声明
  - `RolePresetPlugin.java` — 预置角色
  - `DataScopePlugin.java` — 数据范围维度
  - `MenuContributionPlugin.java` — 菜单项

- 当前 7 个 Registrar：`PluginPackageRegistrar` (50) / `PluginRegistrar` (100) / `RelationTypePluginRegistrar` (200) / `MessagingRegistrar` (300) / `PermissionRegistrar` (400) / `RolePresetRegistrar` (500) / `DataScopeRegistrar` (600) / `MenuRegistrar` (700)。

- 当前启动数据：310 perms / 15 roles / 32 types / 13 relations。

- 37 架构测试全绿，分布：`ArchUnitPluginArchitectureTest` 12 + `ArchUnitDashboardEndpointTest` 1 + `ArchUnitMyEndpointTest` 2 + `DddLayerTest` 4 + `NoMagicTriggerStringTest` 1 + `PluginDeclarationCoverageTest` 4 + `PluginMatrixIntegrationTest` 6 + `SemVerTest` 7。

---

### Task 1: 新增 PluginMetadata record

**Files:**
- Create: `backend/src/main/java/com/school/management/infrastructure/extension/PluginMetadata.java`

**Step 1: 写实现**

```java
package com.school.management.infrastructure.extension;

import java.util.List;
import java.util.Map;

/**
 * 插件包元数据的不可变视图 — 由 {@link PluginPackage#metadata()} 返回.
 *
 * 合并了 {@link PluginManifest} 的 6 个 getter 到一个 record,
 * 供 ContributionDispatcher / 治理 API / 诊断日志使用.
 * 老代码继续用 PluginManifest getter, 新代码用 metadata() 更简洁.
 */
public record PluginMetadata(
    String industryCode,
    String industryName,
    String version,
    List<String> dependsOn,
    Map<String, String> dependsOnWithVersion,
    PluginManifest.UninstallPolicy uninstallPolicy
) {
    /** 从现有 PluginManifest 桥接 (默认实现 PluginPackage.metadata() 用) */
    public static PluginMetadata of(PluginManifest m) {
        return new PluginMetadata(
            m.getIndustryCode(),
            m.getIndustryName(),
            m.getVersion(),
            m.getDependsOn(),
            m.getDependsOnWithVersion(),
            m.getUninstallPolicy()
        );
    }
}
```

**Step 2: 编译通过**

Run: `cd backend && mvn -q compile`
Expected: BUILD SUCCESS

**Step 3: Commit (可延后，最后一起 commit)**

---

### Task 2: 新增 Contribution sealed hierarchy

**Files:**
- Create: `backend/src/main/java/com/school/management/infrastructure/extension/Contribution.java`

**Step 1: 写实现**

```java
package com.school.management.infrastructure.extension;

import java.util.List;

/**
 * 统一贡献契约 — 插件通过 {@link PluginPackage#contribute()} 返回 Stream&lt;Contribution&gt;
 * 声明本包向平台贡献的所有内容.
 *
 * sealed 限定 8 个 permitted subtype,对应旧 SPI 的 7 种职责 + 1 个扩展位.
 * 每种 Contribution 封装一条 def 记录,在 ContributionDispatcher 里
 * 通过 pattern-matching switch 分发到对应 Registrar 的 upsert 方法.
 *
 * Phase 2 只铺设新路径,旧 7 SPI 的 @Component 实现仍被原 Registrar 直接扫,
 * 两条路径到同一张表 UPSERT 幂等,不冲突.
 */
public sealed interface Contribution permits
    Contribution.EntityTypeContribution,
    Contribution.RelationTypeContribution,
    Contribution.EventDomainContribution,
    Contribution.PermissionContribution,
    Contribution.RoleContribution,
    Contribution.MenuContribution,
    Contribution.DataScopeContribution,
    Contribution.DomainContribution {

    /** 跨 Contribution 唯一标识,用于冲突检测/日志 */
    String uniqueKey();

    // ═════════════════════════ 8 个 permitted 记录 ═════════════════════════

    /** 实体类型贡献 (对应旧 EntityTypePlugin 一个实例) */
    record EntityTypeContribution(EntityTypePlugin plugin) implements Contribution {
        @Override public String uniqueKey() {
            return "entity:" + plugin.getEntityType() + "/" + plugin.getTypeCode();
        }
    }

    /** 关系类型贡献 (对应旧 RelationTypePlugin 单条声明) */
    record RelationTypeContribution(String sourceName, String tier,
                                     RelationTypePlugin.RelationTypeDef def) implements Contribution {
        @Override public String uniqueKey() {
            return "relation:" + def.relationCode();
        }
    }

    /** 消息域贡献 — 一个域打包: 触发点 + 事件类型 + 默认触发器 */
    record EventDomainContribution(String domainCode, String domainName,
                                    List<MessagingDomainPlugin.TriggerPointDef> triggerPoints,
                                    List<MessagingDomainPlugin.EventTypeDef> eventTypes,
                                    List<MessagingDomainPlugin.DefaultTriggerDef> defaultTriggers)
            implements Contribution {
        @Override public String uniqueKey() { return "event-domain:" + domainCode; }
    }

    /** 权限贡献 (对应旧 PermissionDef) */
    record PermissionContribution(String moduleCode, String moduleName,
                                   PermissionProvider.PermissionDef def) implements Contribution {
        @Override public String uniqueKey() { return "perm:" + def.code(); }
    }

    /** 预置角色贡献 */
    record RoleContribution(RolePresetPlugin.RolePresetDef def) implements Contribution {
        @Override public String uniqueKey() { return "role:" + def.roleCode(); }
    }

    /** 菜单贡献 */
    record MenuContribution(String domainCode,
                             MenuContributionPlugin.MenuItemDef item) implements Contribution {
        @Override public String uniqueKey() { return "menu:" + item.path(); }
    }

    /** 数据范围贡献 */
    record DataScopeContribution(String domainCode,
                                  DataScopePlugin.DimensionDef def) implements Contribution {
        @Override public String uniqueKey() { return "data-scope:" + def.code(); }
    }

    /**
     * 领域扩展贡献 — 占位符,留给未来非结构化贡献
     * (如跨切面的 BeanPostProcessor、事件订阅器等).
     * 当前版本不落任何 DB,只在日志里登记.
     */
    record DomainContribution(String key, String description) implements Contribution {
        @Override public String uniqueKey() { return "domain:" + key; }
    }
}
```

**Step 2: 编译通过**

Run: `cd backend && mvn -q compile`
Expected: BUILD SUCCESS

---

### Task 3: 新增 PluginPackage interface

**Files:**
- Create: `backend/src/main/java/com/school/management/infrastructure/extension/PluginPackage.java`

**Step 1: 写实现**

```java
package com.school.management.infrastructure.extension;

import java.util.stream.Stream;

/**
 * 统一插件包 SPI — Phase 2 新增的顶层协议.
 *
 * 继承自 {@link PluginManifest} 复用 metadata 字段 (industryCode/version/dependsOn...),
 * 并新增 {@link #contribute()} 返回 {@link Contribution} 流,把旧 7 SPI 的职责
 * (实体类型/关系/消息域/权限/角色/菜单/数据范围) 统一到一个声明点.
 *
 * <h3>与旧 SPI 的关系 (Phase 2 过渡策略)</h3>
 *
 * <ul>
 *   <li>旧 7 SPI (EntityTypePlugin 等) 已打 @Deprecated, 其 @Component 实现仍被
 *       对应 Registrar 扫描,原有行为不变.</li>
 *   <li>新插件可完全走 PluginPackage,不再写一堆 @Component 小类.</li>
 *   <li>两条路径写同一张表,UPSERT 幂等,混用无副作用.</li>
 * </ul>
 *
 * <h3>迁移示例 (后续会话做,Phase 2 只铺底座)</h3>
 * <pre>
 * &#64;Component
 * public class EducationManifest implements PluginPackage {
 *     // 继承 PluginManifest 的 getIndustryCode() 等 (已实现)
 *     &#64;Override
 *     public Stream&lt;Contribution&gt; contribute() {
 *         return Stream.of(
 *             new EntityTypeContribution(new StudentPluginSpec(...)),
 *             new PermissionContribution("teaching", "教学", PermissionDef.of(...))
 *         );
 *     }
 * }
 * </pre>
 */
public interface PluginPackage extends PluginManifest {

    /** 元数据视图,默认从 PluginManifest getter 桥接 */
    default PluginMetadata metadata() {
        return PluginMetadata.of(this);
    }

    /** 本包贡献的所有 Contribution (默认空,旧 Manifest 不受影响) */
    default Stream<Contribution> contribute() {
        return Stream.empty();
    }
}
```

**Step 2: 编译通过**

---

### Task 4: Core/Education Manifest 改实现 PluginPackage

**Files:**
- Modify: `backend/src/main/java/com/school/management/infrastructure/extension/plugins/core/CoreManifest.java`
- Modify: `backend/src/main/java/com/school/management/infrastructure/extension/plugins/education/EducationManifest.java`

**Step 1: CoreManifest**

```java
// 替换
public class CoreManifest implements PluginManifest {
// 为
public class CoreManifest implements PluginPackage {
```

没有其他变化 (contribute() 默认返回空流)。

**Step 2: EducationManifest**

同样，只改 `implements PluginManifest` → `implements PluginPackage`。

**Step 3: 编译**

Run: `cd backend && mvn -q compile`
Expected: BUILD SUCCESS

---

### Task 5: 新增 ContributionDispatcher

**Files:**
- Create: `backend/src/main/java/com/school/management/infrastructure/extension/ContributionDispatcher.java`

**Step 1: 写实现**

Dispatcher 运行于 @Order(60)，此时 `PluginPackageRegistrar` (50) 已完成，所有其他 Registrar (100+) 尚未运行。它遍历 `PluginPackage.contribute()` 流，按类型分发。Phase 2 只要求"契约到位 + 不破坏既有功能"，所以：

- 对每种 Contribution，打 INFO 日志 "[ContributionDispatcher] 收到 {uniqueKey}"
- **不**在本 Dispatcher 内复写 UPSERT 逻辑（避免和现有 Registrar 双写打架）
- 由于现行 `CoreManifest`/`EducationManifest` 默认 `contribute()` 返回空流，启动日志应显示 `总贡献 0`

这样 Phase 2 核心产出是"契约层 + 测试"，不动运行时数据路径。Phase 3+ 再迁移现有插件到新 contribute() API。

```java
package com.school.management.infrastructure.extension;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Phase 2 统一 SPI 的总分发器.
 *
 * 在 {@link PluginPackageRegistrar} (@Order 50) 之后, 所有其他 Registrar (100+) 之前运行,
 * 遍历 {@link PluginPackage#contribute()} 流, 按 Contribution 子类型分发.
 *
 * 当前语义 (Phase 2):
 *   - 仅做 *可见性* — 日志登记 + 冲突检测 (同 uniqueKey 跨包冲突)
 *   - 不写 DB — 因为所有现有插件仍通过旧 @Component SPI + 原 Registrar 完成 UPSERT
 *
 * 未来 (Phase 3+):
 *   - 把现有插件的声明迁移到 contribute() 内
 *   - 本 Dispatcher 逐步接管 UPSERT, 旧 Registrar 逐一下线
 */
@Slf4j
@Component
@Order(60)
@RequiredArgsConstructor
public class ContributionDispatcher implements ApplicationRunner {

    private final List<PluginPackage> packages;

    @Override
    public void run(ApplicationArguments args) {
        if (packages.isEmpty()) {
            log.info("[ContributionDispatcher] 无 PluginPackage 注册");
            return;
        }

        AtomicInteger total = new AtomicInteger();
        AtomicInteger entities = new AtomicInteger();
        AtomicInteger relations = new AtomicInteger();
        AtomicInteger events = new AtomicInteger();
        AtomicInteger perms = new AtomicInteger();
        AtomicInteger roles = new AtomicInteger();
        AtomicInteger menus = new AtomicInteger();
        AtomicInteger scopes = new AtomicInteger();
        AtomicInteger domains = new AtomicInteger();

        Set<String> seenKeys = new HashSet<>();

        for (PluginPackage pkg : packages) {
            String industry = pkg.metadata().industryCode();
            pkg.contribute().forEach(c -> {
                String key = c.uniqueKey();
                if (!seenKeys.add(key)) {
                    throw new IllegalStateException(String.format(
                        "[ContributionDispatcher] 重复贡献: %s (industry=%s) — 同 uniqueKey 跨包不可重复",
                        key, industry));
                }
                total.incrementAndGet();
                switch (c) {
                    case Contribution.EntityTypeContribution __ -> entities.incrementAndGet();
                    case Contribution.RelationTypeContribution __ -> relations.incrementAndGet();
                    case Contribution.EventDomainContribution __ -> events.incrementAndGet();
                    case Contribution.PermissionContribution __ -> perms.incrementAndGet();
                    case Contribution.RoleContribution __ -> roles.incrementAndGet();
                    case Contribution.MenuContribution __ -> menus.incrementAndGet();
                    case Contribution.DataScopeContribution __ -> scopes.incrementAndGet();
                    case Contribution.DomainContribution __ -> domains.incrementAndGet();
                }
                log.debug("[ContributionDispatcher]   {} ← {}", key, industry);
            });
        }

        log.info("[ContributionDispatcher] 扫描 {} 个包, 收到 {} 条 Contribution " +
                "(entity {}, relation {}, event-domain {}, perm {}, role {}, menu {}, scope {}, domain {})",
            packages.size(), total.get(),
            entities.get(), relations.get(), events.get(), perms.get(),
            roles.get(), menus.get(), scopes.get(), domains.get());
    }
}
```

**Step 2: 编译**

---

### Task 6: 给 7 个旧 SPI 打 @Deprecated

**Files:**
- Modify: `backend/src/main/java/com/school/management/infrastructure/extension/EntityTypePlugin.java`
- Modify: `backend/src/main/java/com/school/management/infrastructure/extension/RelationTypePlugin.java`
- Modify: `backend/src/main/java/com/school/management/infrastructure/extension/MessagingDomainPlugin.java`
- Modify: `backend/src/main/java/com/school/management/infrastructure/extension/PermissionProvider.java`
- Modify: `backend/src/main/java/com/school/management/infrastructure/extension/RolePresetPlugin.java`
- Modify: `backend/src/main/java/com/school/management/infrastructure/extension/DataScopePlugin.java`
- Modify: `backend/src/main/java/com/school/management/infrastructure/extension/MenuContributionPlugin.java`

**Step 1: 为每个接口加 `@Deprecated` 注解 + Javadoc**

示例 `EntityTypePlugin.java`:

```java
/**
 * ...原有 javadoc...
 *
 * @deprecated since 1.1.0 — 用 {@link PluginPackage#contribute()} 返回
 *   {@link Contribution.EntityTypeContribution} 替代. 旧 API 仍被
 *   {@link PluginRegistrar} 扫描,运行时等价,无需立即迁移.
 */
@Deprecated(since = "1.1.0", forRemoval = false)
public interface EntityTypePlugin {
    ...
}
```

其他 6 个接口同理，各自指向对应的 Contribution 子类型。

**Step 2: 编译 (应有 warning 但无 error)**

Run: `cd backend && mvn -q compile 2>&1 | grep -c "is deprecated"`

Expected: 数值 > 0 (现有实现引用时触发 deprecation warning，但不是 error)。

**注意:** Java @Deprecated 会让现有 impl (如 `StudentPlugin implements EntityTypePlugin`) 产生 warning。要抑制全仓库 warning 造成噪声，可以：
- 在 impl 类上加 `@SuppressWarnings("deprecation")`
- 或依赖当前 maven-compiler-plugin 配置（通常 warning 不 fail build）

检查当前 Maven 配置是否 Werror：
```bash
grep -rE "failOnWarning|Werror" backend/pom.xml
```

如果不是 Werror，直接提交，warnings 作为迁移提醒。

---

### Task 7: 新增 UnifiedPluginPackageTest

**Files:**
- Create: `backend/src/test/java/com/school/management/architecture/UnifiedPluginPackageTest.java`

**Step 1: 写测试**

```java
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
 *  - {@link Contribution} sealed, permits 恰好 8 种
 *  - 每种 permitted Contribution 都实现 uniqueKey()
 *  - 7 个旧 SPI 都打了 @Deprecated (强化向下兼容承诺)
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
    @DisplayName("Contribution 恰好有 8 个 permitted 子类型")
    void contributionPermitsExactly8Subtypes() {
        Class<?>[] permitted = Contribution.class.getPermittedSubclasses();
        assertEquals(8, permitted.length,
            "Phase 2 约定 8 种 Contribution: entity/relation/event-domain/perm/role/menu/data-scope/domain, 实际=" + permitted.length);
    }

    @Test
    @DisplayName("每种 permitted Contribution 子类型都是 record 且实现 uniqueKey()")
    void permittedSubtypesAreRecordsWithUniqueKey() {
        for (Class<?> sub : Contribution.class.getPermittedSubclasses()) {
            assertTrue(sub.isRecord(), sub.getSimpleName() + " 必须是 record (immutable)");
            // 验证可以调用 uniqueKey (反射)
            boolean hasUniqueKey = Arrays.stream(sub.getMethods())
                .anyMatch(m -> "uniqueKey".equals(m.getName()) && m.getParameterCount() == 0
                    && String.class.equals(m.getReturnType()));
            assertTrue(hasUniqueKey,
                sub.getSimpleName() + " 必须实现 String uniqueKey()");
        }
    }

    @Test
    @DisplayName("7 个旧 SPI 必须打 @Deprecated (迁移提示)")
    void oldSpisAreDeprecated() {
        List<Class<?>> oldSpis = List.of(
            EntityTypePlugin.class,
            RelationTypePlugin.class,
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
    @DisplayName("PluginPackage 默认 contribute() 返回空流, 默认 metadata() 非 null")
    void pluginPackageDefaultMethods() {
        PluginPackage core = new CoreManifest();
        assertEquals(0, core.contribute().count(), "默认 contribute() 必须返回空流");
        assertNotNull(core.metadata(), "默认 metadata() 必须非 null");
        assertEquals("CORE", core.metadata().industryCode());
    }
}
```

**Step 2: 运行测试**

Run: `cd backend && JAVA_HOME="/c/Program Files/Java/jdk-17" PATH="/c/Program Files/Java/jdk-17/bin:$PATH" mvn -q test -Dtest=UnifiedPluginPackageTest`

Expected: Tests run: 10, Failures: 0, Errors: 0

---

### Task 8: ArchUnitPluginArchitectureTest 新增 PluginPackage 规则

**Files:**
- Modify: `backend/src/test/java/com/school/management/architecture/ArchUnitPluginArchitectureTest.java`

**Step 1: 在现有 "SPI 实现必须 @Component" 区块里追加**

```java
@Test
void all_PluginPackage_implementations_must_be_components() {
    ArchRule rule = classes()
            .that().implement(com.school.management.infrastructure.extension.PluginPackage.class)
            .should().beAnnotatedWith(Component.class);
    rule.check(classes);
}
```

**Step 2: 运行 ArchUnit 全集**

Run: `cd backend && mvn -q test -Dtest=ArchUnitPluginArchitectureTest`
Expected: Tests run: 13 (原 12 + 新 1), Failures: 0

---

### Task 9: Gate 1 — 架构测试全绿

**Step 1: 运行完整架构测试矩阵**

Run:
```bash
cd backend
JAVA_HOME="/c/Program Files/Java/jdk-17" PATH="/c/Program Files/Java/jdk-17/bin:$PATH" \
  mvn -q test -Dtest="ArchUnit*,PluginDeclarationCoverageTest,NoMagicTriggerStringTest,DddLayerTest,PluginMatrixIntegrationTest,SemVerTest,UnifiedPluginPackageTest"
```

Expected: 
- `ArchUnitPluginArchitectureTest` 13 (原 12 + 新 1)
- `UnifiedPluginPackageTest` 10 (新增)
- 其他不变
- 总 47+ tests, 0 failures, 0 errors

---

### Task 10: Gate 2 — 冷启动 + API 回归

**Step 1: 杀旧进程**

```bash
cmd //c "taskkill /IM java.exe /F" 2>/dev/null || true
```

**Step 2: 启动后端 (后台, 等 "Started")**

```bash
cd backend
DB_PASSWORD=123456 JAVA_HOME="/c/Program Files/Java/jdk-17" \
  PATH="/c/Program Files/Java/jdk-17/bin:/c/Program Files/apache-maven-3.9.11/bin:$PATH" \
  mvn spring-boot:run -DskipTests
```

Expected 关键日志:
- `[PluginPackageRegistrar] 已加载 2 个行业包 - 启动顺序: [CORE, EDU]`
- `[ContributionDispatcher] 扫描 2 个包, 收到 0 条 Contribution` (新日志)
- `[PluginRegistrar] 扫描 32 插件, 32 声明`
- `[PermissionRegistrar] 扫描 2 插件, 310 声明`
- `Started StudentManagementApplication in XX seconds`

**Step 3: API 回归验证**

```bash
TOKEN=$(curl -sX POST localhost:8080/api/auth/login \
  -H Content-Type:application/json \
  -d '{"username":"admin","password":"admin123"}' | jq -r .data.accessToken)

curl -s localhost:8080/api/plugin-platform/overview \
  -H "Authorization: Bearer $TOKEN" | jq '.data | {industries, totalPermissions, totalRoles, totalTypes}'
```

Expected:
- industries: ["CORE", "EDU"]
- totalPermissions: 310
- totalRoles: 15
- totalTypes: 32

---

### Task 11: 更新 docs/plugin-architecture-status.md

**Step 1: 在 "已完成 (12 Phase, 47 任务)" 表格中加一行 Phase 2**

```markdown
| **2** 统一 SPI 顶层 | PluginPackage + Contribution sealed(8) + ContributionDispatcher + 7 旧 SPI @Deprecated |
```

**Step 2: 删掉 "待完成" 里 Phase 2 整段, 改 "3 Phase, 剩余" → "2 Phase, 剩余"**

**Step 3: A+ 进度 12/15 → 13/15, 80% → 87%**

**Step 4: 架构测试表更新**

原:
```
UnifiedPluginPackageTest              10
```
加到 ArchUnit 表。总架构测试数 37 → 48。

---

### Task 12: 提交

**Step 1: 检查 git status**

```bash
git status
git diff --stat
```

**Step 2: 提交**

```bash
git add backend/src/main/java/com/school/management/infrastructure/extension/PluginMetadata.java
git add backend/src/main/java/com/school/management/infrastructure/extension/Contribution.java
git add backend/src/main/java/com/school/management/infrastructure/extension/PluginPackage.java
git add backend/src/main/java/com/school/management/infrastructure/extension/ContributionDispatcher.java
git add backend/src/main/java/com/school/management/infrastructure/extension/EntityTypePlugin.java
git add backend/src/main/java/com/school/management/infrastructure/extension/RelationTypePlugin.java
git add backend/src/main/java/com/school/management/infrastructure/extension/MessagingDomainPlugin.java
git add backend/src/main/java/com/school/management/infrastructure/extension/PermissionProvider.java
git add backend/src/main/java/com/school/management/infrastructure/extension/RolePresetPlugin.java
git add backend/src/main/java/com/school/management/infrastructure/extension/DataScopePlugin.java
git add backend/src/main/java/com/school/management/infrastructure/extension/MenuContributionPlugin.java
git add backend/src/main/java/com/school/management/infrastructure/extension/plugins/core/CoreManifest.java
git add backend/src/main/java/com/school/management/infrastructure/extension/plugins/education/EducationManifest.java
git add backend/src/test/java/com/school/management/architecture/UnifiedPluginPackageTest.java
git add backend/src/test/java/com/school/management/architecture/ArchUnitPluginArchitectureTest.java
git add docs/plans/2026-04-19-phase2-unified-spi.md
git add docs/plugin-architecture-status.md

git commit -m "feat(plugin-arch): Phase 2 — 统一 SPI 顶层协议 (PluginPackage + Contribution)

- 新增 PluginPackage 接口(继承 PluginManifest, default contribute()=空)
- 新增 Contribution sealed + 8 permitted records
- 新增 PluginMetadata record 作为 metadata() 返回
- 新增 ContributionDispatcher @Order(60), 仅登记+冲突检测, 不动 DB 路径
- CoreManifest/EducationManifest 升级为 PluginPackage
- 7 个旧 SPI 打 @Deprecated(forRemoval=false), 功能完全保留
- 新增 UnifiedPluginPackageTest 10 条断言
- ArchUnitPluginArchitectureTest 新增 PluginPackage @Component 规则
- 数据 0 回归: 310 perms / 15 roles / 32 types 完全一致

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

**Step 3: 验证 git log**

```bash
git log --oneline | head -3
```

Expected:
- 新 commit 在顶
- 第二条是 `58d2279a feat(plugin-arch): Phase -1~10 完成`

---

## 每个任务的测试纪律

| 任务 | 最小验证 |
|---|---|
| Task 1 Metadata | `mvn -q compile` 通过 |
| Task 2 Contribution | `mvn -q compile` 通过 |
| Task 3 PluginPackage | `mvn -q compile` 通过 |
| Task 4 Manifest 改接口 | `mvn -q compile` 通过 |
| Task 5 Dispatcher | `mvn -q compile` 通过 |
| Task 6 @Deprecated | `mvn -q compile` 通过 (warnings 可接受) |
| Task 7 UnifiedPluginPackageTest | `mvn -q test -Dtest=UnifiedPluginPackageTest` 10 绿 |
| Task 8 ArchUnit 追加 | `mvn -q test -Dtest=ArchUnitPluginArchitectureTest` 13 绿 |
| Task 9 Gate 1 | 全架构测试 47+ 全绿 |
| Task 10 Gate 2 | 启动日志 + API 数据完全对齐 |
| Task 11 文档 | 手动 diff 检查 |
| Task 12 Commit | `git status` 干净 |

---

## 风险与应对

| 风险 | 概率 | 应对 |
|---|---|---|
| 新 Dispatcher 干扰现有启动顺序 | 低 | @Order(60) 介于 Package(50) 和其他 Registrar(100+) 之间, 新路径空流, 原路径不变 |
| @Deprecated 引发 Werror 编译失败 | 低 | 先 grep 确认 pom 无 failOnWarning, 若有则在 impl 加 @SuppressWarnings |
| 现有 ArchUnit 因新 PluginPackage 类命名失败 | 低 | 只新增一条规则, 不改已有规则 |
| Spring 扫 PluginManifest 时行为变化 | 极低 | PluginPackageRegistrar 继续扫 List&lt;PluginManifest&gt;, PluginPackage 是子类型 Spring 能包含 |
| 冷启动 API 数据回归 (如 perm 数 ≠ 310) | 低 | 新路径不写 DB, 旧路径完全不动, 理论 0 回归 |

## 回滚方案

如 Gate 2 失败:
```bash
git reset --hard HEAD  # 回到 Phase 2 之前
```

Phase 2 所有改动都未提交前可随时 reset。

---

**计划就绪。接下来按 Task 2~12 依次执行。**
