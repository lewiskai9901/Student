package com.school.management.architecture;

import com.school.management.infrastructure.casbin.CasbinAccess;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

/**
 * /dashboard/* 接口强约束：
 *   所有映射到 /dashboard 开头路径的 Controller 方法必须显式标注 {@link CasbinAccess}，
 *   防止裸奔接口将"全校聚合统计"泄漏给无权角色（历史事故：commit 324f515c 之前
 *   {@code /dashboard/overview} 零权限控制，teacher01 能直取全校数据）。
 */
class ArchUnitDashboardEndpointTest {

    private static JavaClasses classes;

    @BeforeAll
    static void loadClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.school.management");
    }

    @Test
    void dashboardEndpoints_mustHaveCasbinAccess() {
        methods()
                .that(isDashboardEndpoint())
                .should(beAnnotatedWithCasbinAccess())
                .check(classes);
    }

    // ================= Predicates =================

    private static DescribedPredicate<JavaMethod> isDashboardEndpoint() {
        return new DescribedPredicate<>("映射到 /dashboard 开头路径") {
            @Override
            public boolean test(JavaMethod method) {
                String methodPath = mappingPath(method);
                if (methodPath == null) return false;
                String classPath = classMappingPath(method);
                String full = join(classPath, methodPath);
                return full.startsWith("/dashboard/") || full.equals("/dashboard");
            }
        };
    }

    // ================= Conditions =================

    private static ArchCondition<JavaMethod> beAnnotatedWithCasbinAccess() {
        return new ArchCondition<>("标注 @CasbinAccess") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                if (!method.isAnnotatedWith(CasbinAccess.class)) {
                    events.add(SimpleConditionEvent.violated(method,
                            method.getFullName() + " 未标注 @CasbinAccess，/dashboard/* 接口必须显式声明权限"));
                }
            }
        };
    }

    // ================= Helpers =================

    private static String classMappingPath(JavaMethod m) {
        return m.getOwner().tryGetAnnotationOfType(RequestMapping.class)
                .map(a -> firstOrBlank(a.value()))
                .orElse("");
    }

    private static String mappingPath(JavaMethod m) {
        String[] candidates = new String[]{
                tryValue(m, GetMapping.class),
                tryValue(m, PostMapping.class),
                tryValue(m, PutMapping.class),
                tryValue(m, DeleteMapping.class),
                tryValue(m, PatchMapping.class),
                tryValue(m, RequestMapping.class)
        };
        for (String c : candidates) if (c != null) return c;
        return null;
    }

    private static <A extends java.lang.annotation.Annotation> String tryValue(JavaMethod m, Class<A> ann) {
        return m.tryGetAnnotationOfType(ann)
                .map(a -> {
                    try {
                        String[] value = (String[]) ann.getMethod("value").invoke(a);
                        return firstOrBlank(value);
                    } catch (Exception e) { return null; }
                })
                .orElse(null);
    }

    private static String firstOrBlank(String[] arr) {
        return (arr == null || arr.length == 0) ? "" : arr[0];
    }

    private static String join(String a, String b) {
        String aa = a == null ? "" : a;
        String bb = b == null ? "" : b;
        if (aa.endsWith("/") && bb.startsWith("/")) return aa + bb.substring(1);
        if (!aa.isEmpty() && !aa.endsWith("/") && !bb.startsWith("/")) return aa + "/" + bb;
        return aa + bb;
    }
}
