package com.school.management.architecture;

import com.school.management.common.annotation.PublicEndpoint;
import com.school.management.infrastructure.casbin.CasbinAccess;
import com.tngtech.archunit.base.DescribedPredicate;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

/**
 * 守护规则: REST endpoint 都必须有 @PreAuthorize / @CasbinAccess / @PublicEndpoint 之一 (M3.1, 2026-05-20).
 *
 * <p>避免 MyClassController 类似的"忘记加权限"事故 — 该 controller 之前 4 端点
 * 全部裸奔 ~6 个月, 靠业务 service userId 过滤兜底 (脆弱).
 *
 * <p>白名单 (PublicEndpoint 注解): 登录 / 注册 / 健康检查 / OpenAPI 等公开端点.
 */
class ArchUnitAllRestEndpointsProtectedTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.school.management");
    }

    private static final List<Class<? extends java.lang.annotation.Annotation>> HTTP_MAPPINGS = Arrays.asList(
        GetMapping.class, PostMapping.class, PutMapping.class,
        DeleteMapping.class, PatchMapping.class, RequestMapping.class
    );

    @Test
    void allRestEndpoints_haveAuthOrPublicAnnotation() {
        ArchRule rule = methods()
            .that(new DescribedPredicate<JavaMethod>("are REST endpoints") {
                @Override
                public boolean test(JavaMethod m) {
                    for (Class<? extends java.lang.annotation.Annotation> a : HTTP_MAPPINGS) {
                        if (m.isAnnotatedWith(a)) return true;
                    }
                    return false;
                }
            })
            .and(new DescribedPredicate<JavaMethod>("in a class annotated with @RestController or @Controller") {
                @Override
                public boolean test(JavaMethod m) {
                    return m.getOwner().isAnnotatedWith(RestController.class)
                        || m.getOwner().isAnnotatedWith(org.springframework.stereotype.Controller.class);
                }
            })
            .should(new ArchCondition<JavaMethod>("be annotated with @PreAuthorize, @CasbinAccess, or @PublicEndpoint") {
                @Override
                public void check(JavaMethod method, ConditionEvents events) {
                    boolean hasAuth = method.isAnnotatedWith(PreAuthorize.class)
                        || method.isAnnotatedWith(CasbinAccess.class)
                        || method.isAnnotatedWith(PublicEndpoint.class)
                        || method.getOwner().isAnnotatedWith(PreAuthorize.class)
                        || method.getOwner().isAnnotatedWith(CasbinAccess.class)
                        || method.getOwner().isAnnotatedWith(PublicEndpoint.class);
                    if (!hasAuth) {
                        events.add(SimpleConditionEvent.violated(method,
                            "REST endpoint " + method.getFullName()
                                + " has no @PreAuthorize / @CasbinAccess / @PublicEndpoint annotation"));
                    }
                }
            })
            .because("M3.1 — 防 MyClassController 同款事故: REST 端点必须显式声明保护策略或 @PublicEndpoint");

        rule.check(classes);
    }
}
