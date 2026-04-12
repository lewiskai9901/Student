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
 * 规则：
 * - Domain 层不能依赖 Infrastructure 层
 * - Domain 层不能依赖 Interfaces 层
 * - Domain 层不能依赖 Application 层
 * - Application 层不能依赖 Interfaces 层
 *
 * 目标：建立规则集阻止未来的违规；现有违规可用 @Disabled 暂缓清理。
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
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                .check(classes);
    }

    @Test
    void domainLayer_shouldNotDependOnInterfaces() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..interfaces..")
                .check(classes);
    }

    @Test
    void domainLayer_shouldNotDependOnApplication() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..application..")
                .check(classes);
    }

    @Test
    void applicationLayer_shouldNotDependOnInterfaces() {
        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAPackage("..interfaces..")
                .check(classes);
    }
}
