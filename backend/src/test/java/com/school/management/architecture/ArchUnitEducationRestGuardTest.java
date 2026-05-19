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
 * 教育插件 REST 层 ArchUnit 守护 (M3.2.6, 2026-05-20).
 *
 * <p>M3.2 重构: 教育插件 6 个 REST controller (Workflow / PeriodConfig /
 * Attendance / Enrollment / AcademicWarning / TeachingSchedule) 共 159 处
 * 直 jdbc CRUD 已下沉到 application 层. integration/rest/asset 同款守.
 *
 * <p>守护: 教育插件 REST 层包不得依赖 JdbcTemplate, 防 PR 偷偷写回 159 jdbc 重复
 * 反 DDD. 这是后端整体 jdbc 反 DDD 集中点 #3 收尾 (#1=event 39, #2=asset 101, #3=edu 159).
 */
class ArchUnitEducationRestGuardTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.school.management");
    }

    @Test
    void educationRestControllers_doNotDependOnJdbcTemplate() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..extension.plugins.education.interfaces.rest..")
            .should().dependOnClassesThat().areAssignableTo(JdbcTemplate.class)
            .because("M3.2 — 教育插件 REST 层必须走 application/ 内的 *ApplicationService, "
                + "不准直接 jdbc CRUD (反 DDD 集中点 #3, 已抽 6 service 后必须守住)");

        rule.check(classes);
    }
}
