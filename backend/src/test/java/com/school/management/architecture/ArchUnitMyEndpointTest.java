package com.school.management.architecture;

import com.school.management.common.util.SecurityUtils;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaParameter;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

/**
 * /my/* endpoint 强约束：
 *   门 3 — 任何映射到 /my/ 开头路径的 Controller 方法必须
 *     1) 调用 SecurityUtils.requireCurrentUserId() 获取身份
 *     2) 不得接收 teacherId / userId 等身份参数（防止横向越权）
 *
 * 参见 docs/plans/2026-04-14-my-dashboard-design.md §5 三道门
 */
class ArchUnitMyEndpointTest {

    private static final Set<String> FORBIDDEN_IDENTITY_PARAMS = Set.of(
            "teacherId", "userId", "operatorId", "subjectId"
    );

    private static JavaClasses classes;

    @BeforeAll
    static void loadClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.school.management");
    }

    @Test
    void myEndpoints_mustCallRequireCurrentUserId() {
        methods()
                .that(isMyEndpoint())
                .should(callRequireCurrentUserId())
                .check(classes);
    }

    @Test
    void myEndpoints_mustNotAcceptIdentityRequestParam() {
        methods()
                .that(isMyEndpoint())
                .should(notHaveIdentityRequestParam())
                .check(classes);
    }

    // ================= Predicates =================

    private static DescribedPredicate<JavaMethod> isMyEndpoint() {
        return new DescribedPredicate<>("映射到 /my/ 开头路径") {
            @Override
            public boolean test(JavaMethod method) {
                String methodPath = mappingPath(method);
                if (methodPath == null) return false;
                String classPath = classMappingPath(method);
                String full = join(classPath, methodPath);
                return full.startsWith("/my/") || full.equals("/my");
            }
        };
    }

    // ================= Conditions =================

    private static ArchCondition<JavaMethod> callRequireCurrentUserId() {
        return new ArchCondition<>("调用 SecurityUtils.requireCurrentUserId()") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                boolean calls = method.getMethodCallsFromSelf().stream()
                        .anyMatch(c -> c.getTarget().getOwner().isEquivalentTo(SecurityUtils.class)
                                && "requireCurrentUserId".equals(c.getTarget().getName()));
                if (!calls) {
                    events.add(SimpleConditionEvent.violated(method,
                            method.getFullName() + " 未调用 SecurityUtils.requireCurrentUserId()，/my/* 方法必须由接口内部硬取当前用户"));
                }
            }
        };
    }

    private static ArchCondition<JavaMethod> notHaveIdentityRequestParam() {
        return new ArchCondition<>("不得接收 teacherId/userId 等身份参数") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                for (JavaParameter p : method.getParameters()) {
                    if (!p.isAnnotatedWith(RequestParam.class)) continue;
                    String name = p.getAnnotationOfType(RequestParam.class).value();
                    if ((name == null || name.isEmpty())) {
                        name = p.getAnnotationOfType(RequestParam.class).name();
                    }
                    if (name != null && FORBIDDEN_IDENTITY_PARAMS.contains(name)) {
                        events.add(SimpleConditionEvent.violated(method,
                                method.getFullName() + " 不得接收身份参数 @RequestParam " + name
                                        + "，请通过 SecurityUtils.requireCurrentUserId() 获取"));
                    }
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
