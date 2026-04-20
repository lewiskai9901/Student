package com.school.management.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Phase 6.7: REST Controller 授权覆盖度守护.
 *
 * 规则: interfaces.rest 下每个 @RestController 必须满足下列之一:
 *   1. class-level 标注 @PreAuthorize 或 @CasbinAccess
 *   2. 至少一个 endpoint method 标注 @PreAuthorize 或 @CasbinAccess
 *
 * 例外 (白名单): AuthController 的 /login /refresh 是 pre-auth public endpoints,
 *                只要 class 本身有其它 secured method 即满足规则.
 *
 * 目的: 防止新增 controller "裸奔" (即全 public, 任何人可调用的 zero-auth 状态).
 */
class ArchUnitControllerAuthTest {

    private static JavaClasses classes;

    /** 业务端点 mapping 注解 — 带这些的 method 才算 "endpoint" */
    private static final Set<String> MAPPING_ANNOTATIONS = Set.of(
        "org.springframework.web.bind.annotation.GetMapping",
        "org.springframework.web.bind.annotation.PostMapping",
        "org.springframework.web.bind.annotation.PutMapping",
        "org.springframework.web.bind.annotation.DeleteMapping",
        "org.springframework.web.bind.annotation.PatchMapping",
        "org.springframework.web.bind.annotation.RequestMapping"
    );

    /** 授权注解 */
    private static final Set<String> AUTH_ANNOTATIONS = Set.of(
        "org.springframework.security.access.prepost.PreAuthorize",
        "org.springframework.security.access.prepost.PostAuthorize",
        "com.school.management.infrastructure.casbin.CasbinAccess"
    );

    @BeforeAll
    static void loadClasses() {
        classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.school.management.interfaces.rest");
    }

    @Test
    void every_rest_controller_must_have_at_least_one_auth_annotation() {
        ArchRule rule = classes()
            .that().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
            .should(haveAtLeastOneAuthAnnotation())
            .because("Phase 6.7: 每个 @RestController 必须至少有一个 @PreAuthorize/@CasbinAccess " +
                     "(class-level 或 method-level), 防止 zero-auth 裸奔 controller.");
        rule.check(classes);
    }

    private static ArchCondition<JavaClass> haveAtLeastOneAuthAnnotation() {
        return new ArchCondition<>("have at least one auth annotation on class or any mapped method") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                // 1. class-level 授权
                if (hasAnyAnnotation(item.getAnnotations().stream().map(a -> a.getRawType().getFullName()).toList(), AUTH_ANNOTATIONS)) {
                    return;
                }
                // 2. 任一 endpoint method 授权
                for (JavaMethod method : item.getMethods()) {
                    var methodAnns = method.getAnnotations().stream().map(a -> a.getRawType().getFullName()).toList();
                    boolean isEndpoint = hasAnyAnnotation(methodAnns, MAPPING_ANNOTATIONS);
                    if (isEndpoint && hasAnyAnnotation(methodAnns, AUTH_ANNOTATIONS)) {
                        return;
                    }
                }
                // 3. 无保护
                events.add(SimpleConditionEvent.violated(item,
                    String.format("Controller %s 没有任何 @PreAuthorize/@CasbinAccess 保护",
                                  item.getFullName())));
            }
        };
    }

    private static boolean hasAnyAnnotation(java.util.List<String> found, Set<String> lookFor) {
        for (String a : found) if (lookFor.contains(a)) return true;
        return false;
    }
}
