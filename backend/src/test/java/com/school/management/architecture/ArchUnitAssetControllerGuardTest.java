package com.school.management.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 资产 Controller ArchUnit 守护 (M2, 2026-05-20).
 *
 * <p>M2 重构: interfaces/rest/asset/* 9 Controller 共 101 处直 jdbc CRUD 已下沉
 * 到 application/asset/*ApplicationService (9 个 service). Controller 仅保留
 * HTTP 绑定 + 表现层 enum desc helpers.
 *
 * <p>守护规则: interfaces.rest.asset 包不得依赖 JdbcTemplate, 防止后续 PR 再
 * 在 controller 直 jdbc (反 DDD 集中点 #2, 与 L3 event controller 同款).
 */
class ArchUnitAssetControllerGuardTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.school.management");
    }

    @Test
    void assetControllers_doNotDependOnJdbcTemplate() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..interfaces.rest.asset..")
            .should().dependOnClassesThat().areAssignableTo(JdbcTemplate.class)
            .because("M2 — 资产 Controller 必须走 application/asset/*ApplicationService, "
                + "不准直接 jdbc CRUD (反 DDD 集中点 #2, 已抽 9 个 service 后必须守住)");

        rule.check(classes);
    }
}
