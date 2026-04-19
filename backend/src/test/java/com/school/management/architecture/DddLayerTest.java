package com.school.management.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * DDD 分层依赖约束测试。
 *
 * 规则 (**仅约束平台顶层的 4 层**):
 *   com.school.management.domain.*           不依赖 infrastructure / interfaces / application
 *   com.school.management.application.*      不依赖 interfaces
 *
 * 注意 (Phase 3.5 后): 匹配用精确前缀 {@code com.school.management.domain..}
 * 而非通配 {@code ..domain..}, 否则会误匹配插件本地的 DDD 子包
 * (如 {@code com.school.management.infrastructure.extension.plugins.education.domain.calendar}).
 * 插件内部的分层依赖由插件自身把控, 不应被平台顶层规则殃及.
 */
class DddLayerTest {

    private static JavaClasses classes;

    @BeforeAll
    static void loadClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.school.management");
    }

    @Test
    void domainLayer_shouldNotDependOnInfrastructure() {
        noClasses()
                .that().resideInAPackage("com.school.management.domain..")
                .should().dependOnClassesThat().resideInAPackage("com.school.management.infrastructure..")
                .check(classes);
    }

    @Test
    void domainLayer_shouldNotDependOnInterfaces() {
        noClasses()
                .that().resideInAPackage("com.school.management.domain..")
                .should().dependOnClassesThat().resideInAPackage("com.school.management.interfaces..")
                .check(classes);
    }

    @Test
    void domainLayer_shouldNotDependOnApplication() {
        noClasses()
                .that().resideInAPackage("com.school.management.domain..")
                .should().dependOnClassesThat().resideInAPackage("com.school.management.application..")
                .check(classes);
    }

    @Test
    void applicationLayer_shouldNotDependOnInterfaces() {
        noClasses()
                .that().resideInAPackage("com.school.management.application..")
                .should().dependOnClassesThat().resideInAPackage("com.school.management.interfaces..")
                .check(classes);
    }
}
