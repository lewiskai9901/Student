package com.school.management.architecture;

import com.school.management.infrastructure.extension.EntityTypePlugin;
import com.school.management.infrastructure.extension.MessagingDomainPlugin;
import com.school.management.infrastructure.extension.PermissionProvider;
import com.school.management.infrastructure.extension.PluginManifest;
import com.school.management.infrastructure.extension.PluginPackage;
import com.school.management.infrastructure.extension.RolePresetPlugin;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 插件架构强约束 (ArchUnit).
 *
 * 防止插件化重构后出现回归:
 *  - 行业隔离: 教育插件不能 import 医疗,反之亦然
 *  - SPI 实现必须 @Component (否则 Spring 扫不到)
 *  - 命名规范: Plugin/Provider/Manifest 后缀
 *  - 业务代码不能从 plugins/ 包直接 import 行业插件实现类 (应通过 SPI 接口)
 */
class ArchUnitPluginArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void loadClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.school.management");
    }

    // ─── 行业隔离 ───

    @Test
    void education_plugins_must_not_depend_on_other_industries() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..plugins.education..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..plugins.healthcare..",
                        "..plugins.eldercare..",
                        "..plugins.finance.."
                );
        rule.check(classes);
    }

    @Test
    void core_plugins_must_not_depend_on_industry_plugins() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..plugins.core..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..plugins.education..",
                        "..plugins.healthcare..",
                        "..plugins.eldercare.."
                );
        rule.check(classes);
    }

    // ─── SPI 实现必须 @Component ───

    @Test
    void all_PluginManifest_implementations_must_be_components() {
        ArchRule rule = classes()
                .that().implement(PluginManifest.class)
                .should().beAnnotatedWith(Component.class);
        rule.check(classes);
    }

    @Test
    void all_PluginPackage_implementations_must_be_components() {
        ArchRule rule = classes()
                .that().implement(PluginPackage.class)
                .should().beAnnotatedWith(Component.class);
        rule.check(classes);
    }

    // ─── Phase 3.5: 已迁出的 4 个教育特定领域不得复现 ───

    @Test
    void migrated_education_domains_must_not_return_to_top_level_domain() {
        // Phase 3.5 把 student/academic/teaching/calendar 迁到 plugins/education/domain/,
        // 本规则阻止未来有人在顶层 com.school.management.domain.{pkg} 里再建这 4 个子包.
        //
        // 注意用精确前缀 com.school.management.domain.{pkg} 而非 ..domain.{pkg}..,
        // 否则 plugins/education/domain/student 会误伤 (那是插件本地的, 允许).
        ArchRule rule = noClasses()
                .should().resideInAnyPackage(
                    "com.school.management.domain.student..",
                    "com.school.management.domain.academic..",
                    "com.school.management.domain.teaching..",
                    "com.school.management.domain.calendar.."
                );
        rule.check(classes);
    }

    @Test
    void all_EntityTypePlugin_implementations_must_be_components() {
        ArchRule rule = classes()
                .that().implement(EntityTypePlugin.class)
                .should().beAnnotatedWith(Component.class);
        rule.check(classes);
    }

    // RelationTypePlugin 已在 Phase 2 W2.2 删除 — 关系类型现在通过 PluginPackage.contribute()
    // 返回 Stream<Contribution.RelationTypeContribution> 声明. 无需独立 SPI 实现校验.

    @Test
    void all_MessagingDomainPlugin_implementations_must_be_components() {
        ArchRule rule = classes()
                .that().implement(MessagingDomainPlugin.class)
                .should().beAnnotatedWith(Component.class);
        rule.check(classes);
    }

    @Test
    void all_PermissionProvider_implementations_must_be_components() {
        ArchRule rule = classes()
                .that().implement(PermissionProvider.class)
                .should().beAnnotatedWith(Component.class);
        rule.check(classes);
    }

    @Test
    void all_RolePresetPlugin_implementations_must_be_components() {
        ArchRule rule = classes()
                .that().implement(RolePresetPlugin.class)
                .should().beAnnotatedWith(Component.class);
        rule.check(classes);
    }

    // ─── 命名规范 ───

    @Test
    void manifest_classes_must_have_Manifest_suffix() {
        // 行业顶级 Manifest 必须 Manifest 后缀.
        // 例外: 细粒度 messaging 插件实现 PluginPackage (transitively PluginManifest) 以
        // 走 contribute() 渠道, 它们按类型职责命名 (*MessagingPlugin), 不在本规则约束内.
        // 通过 residesOutsideOfPackages 排除 messaging 子包.
        ArchRule rule = classes()
                .that().implement(PluginManifest.class)
                .and().resideOutsideOfPackages("..plugins..messaging..")
                .should().haveSimpleNameEndingWith("Manifest");
        rule.check(classes);
    }

    @Test
    void messaging_domain_plugins_must_have_MessagingPlugin_suffix() {
        ArchRule rule = classes()
                .that().implement(MessagingDomainPlugin.class)
                .should().haveSimpleNameEndingWith("MessagingPlugin");
        rule.check(classes);
    }

    // ─── 禁止再用已删除的 PermissionConstants ───

    @Test
    void no_class_may_import_deleted_PermissionConstants() {
        ArchRule rule = noClasses()
                .should().dependOnClassesThat()
                .haveFullyQualifiedName("com.school.management.common.constants.PermissionConstants");
        rule.check(classes);
    }

    // ─── 业务代码访问约束 ───

    @Test
    void business_code_must_not_depend_on_industry_plugin_impls_directly() {
        // 业务层 (**平台顶层的** application/domain/interfaces) 只能依赖 SPI 接口,
        // 禁止直接 import 具体插件实现类 (如 StudentPlugin / DormitoryMessagingPlugin).
        //
        // 例外白名单: 允许依赖纯常量契约类 — 类名以下列后缀结尾:
        //   Constants / TriggerPoints / Relations / Permissions
        //
        // 原则: 业务代码 + 核心插件可引用契约常量(文字语义稳定),
        //       但不能引用行业插件的实现 Bean(会制造硬耦合,违反可拆卸性).
        //
        // Phase 3.5 后注意: 匹配用精确前缀 com.school.management.{domain,application,interfaces}..
        // 而非通配 ..domain.. / ..application.. / ..interfaces.., 否则教育插件内部自己
        // (plugins.education.domain.calendar 等) 会被误判为业务层而和自己发生"跨层依赖".
        com.tngtech.archunit.base.DescribedPredicate<com.tngtech.archunit.core.domain.JavaClass> nonContractClass =
            new com.tngtech.archunit.base.DescribedPredicate<>("行业插件的非契约实现类(非 Constants/TriggerPoints/Relations/Permissions)") {
                @Override
                public boolean test(com.tngtech.archunit.core.domain.JavaClass c) {
                    String name = c.getSimpleName();
                    return !name.endsWith("Constants")
                        && !name.endsWith("TriggerPoints")
                        && !name.endsWith("Relations")
                        && !name.endsWith("Permissions");
                }
            };

        ArchRule rule = noClasses()
                .that().resideInAnyPackage(
                    "com.school.management.domain..",
                    "com.school.management.application..",
                    "com.school.management.interfaces..")
                .should().dependOnClassesThat(
                    com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage("..plugins.education..")
                        .and(nonContractClass)
                );
        rule.check(classes);
    }
}
