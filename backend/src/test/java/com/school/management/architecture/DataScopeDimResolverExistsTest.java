package com.school.management.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * 守护:{@code data_scope_dims.resolver_type} 引用的所有 FQCN 必须真存在,
 * 且 implements {@code DataScopePlugin.DataScopeResolver}.
 *
 * <p>2026-05-16 修复 bug 时发现的 historical mistake:
 * {@code data_scope_dims} 表声明了 5 个 Resolver (BY_MAJOR / BY_GRADE / BY_CLASS /
 * BY_ATTENDING_DOCTOR / BY_WARD), 但对应 Java class 全部不存在.
 * 运行时 {@code PluginDataScopeRouter.loadResolver} 反射 Class.forName 失败,
 * 拦截器降级 SELF, 用户看不到任何数据.
 *
 * <p>此守护在 EDU 维度 (BY_MAJOR / BY_GRADE / BY_CLASS) 已实现后, 防止
 * 后续插件再贡献"PowerPoint 维度" (声明但无实现).
 *
 * <p>注:HEALTH 维度 (BY_ATTENDING_DOCTOR / BY_WARD) 在
 * V20260516_2 migration 已清, 不在此守护范围.
 *
 * <p>守护方式:静态白名单 — 写在 EDU_RESOLVER_FQCNS 里的类必须存在.
 * 不读 DB (避免测试依赖 DB 启动), 由 db migration 保证 data_scope_dims 内容
 * 与白名单一致, P5 阶段把这个改成 plugin Contribution 后强一致.
 */
class DataScopeDimResolverExistsTest {

    /** EDU 插件贡献的 3 个 Resolver 必须存在 */
    private static final Set<String> EDU_RESOLVER_FQCNS = Set.of(
        "com.school.management.infrastructure.extension.plugins.education.scope.MajorDataScopeResolver",
        "com.school.management.infrastructure.extension.plugins.education.scope.GradeDataScopeResolver",
        "com.school.management.infrastructure.extension.plugins.education.scope.ClassDataScopeResolver"
    );

    private static final String DATA_SCOPE_RESOLVER_IFACE =
        "com.school.management.infrastructure.extension.DataScopePlugin$DataScopeResolver";

    @Test
    void all_declared_resolver_fqcns_must_exist_and_implement_DataScopeResolver() {
        JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.school.management");

        List<String> missing = EDU_RESOLVER_FQCNS.stream()
            .filter(fqcn -> !classes.contain(fqcn))
            .collect(Collectors.toList());

        if (!missing.isEmpty()) {
            fail("data_scope_dims.resolver_type 声明的以下 FQCN 不存在 Java class:\n  - "
                + String.join("\n  - ", missing)
                + "\n→ PluginDataScopeRouter 会 Class.forName 失败, 维度静默降级 SELF");
        }

        // 验证 implements DataScopeResolver
        for (String fqcn : EDU_RESOLVER_FQCNS) {
            var javaClass = classes.get(fqcn);
            boolean implementsIface = javaClass.getAllRawInterfaces().stream()
                .anyMatch(i -> i.getFullName().equals(DATA_SCOPE_RESOLVER_IFACE));
            assertTrue(implementsIface,
                fqcn + " 必须 implements DataScopePlugin.DataScopeResolver");
        }
    }
}
