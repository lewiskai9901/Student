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
 * 事件配置 Controller ArchUnit 守护 (L3, 2026-05-19).
 *
 * <p>L3 重构: interfaces/rest/event/* 三 Controller (EventTriggerController /
 * EventTypeController / TriggerPointController) 31 处直 jdbc CRUD 已下沉到
 * {@code application/event/EventConfigApplicationService}.
 *
 * <p>守护规则: interfaces.rest.event 包不得依赖 JdbcTemplate, 防止后续 PR 偷偷
 * 把 jdbc 写回 Controller 层 (这是反 DDD 集中点之一).
 */
class ArchUnitEventControllerGuardTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.school.management");
    }

    @Test
    void eventControllers_doNotDependOnJdbcTemplate() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..interfaces.rest.event..")
            .should().dependOnClassesThat().areAssignableTo(JdbcTemplate.class)
            .because("L3 — 事件配置 Controller 必须走 EventConfigApplicationService, "
                + "不准直接 jdbc CRUD (这是反 DDD 集中点之一, 已抽 service 后必须守住)");

        rule.check(classes);
    }
}
