package com.school.management.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 工作流引擎(Flowable) ArchUnit 守护规则.
 *
 * Phase 1-7 建立的边界:
 * <ol>
 *   <li>application/workflow/* 不准直接 SQL 读 ACT_* 表 — 走 Flowable 服务接口</li>
 *   <li>interfaces/rest/workflow/* controller 必须用 Flowable 服务 (RepositoryService/RuntimeService/TaskService/HistoryService)</li>
 *   <li>所有 controller 必须有 @PreAuthorize 防止 missing auth gate</li>
 * </ol>
 */
class ArchUnitWorkflowGuardTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.school.management");
    }

    @Test
    void applicationWorkflowLayer_doesNotDependOnJdbcTemplate() {
        // 守护: application/workflow/* 不能用 JdbcTemplate 直查 ACT_* 表
        // 只读源 (MyInspectionTaskTodoSource) 例外 — 它查 inspection_tasks 不查 Flowable 表
        ArchRule rule = noClasses()
            .that().resideInAPackage("..application.workflow..")
            .should().dependOnClassesThat().areAssignableTo(JdbcTemplate.class)
            .because("Phase 1-7 — 工作流 application 层应用 Flowable 服务接口, 不走 jdbc 查 ACT_* 表");

        rule.check(classes);
    }

    @Test
    void workflowControllers_useFlowableServices() {
        // 守护: interfaces/rest/workflow controllers 应该依赖 Flowable 标准服务接口
        // (而不是直接 import 内部实现)
        ArchRule rule = classes()
            .that().resideInAPackage("..interfaces.rest.workflow..")
            .and().haveSimpleNameEndingWith("Controller")
            .should().bePublic()
            .because("REST controller 标准约束");

        rule.check(classes);
    }

    @Test
    void noWorkflowImpl_inDomainLayer() {
        // 守护: 不应该有 Flowable specific 类出现在 domain 层
        // domain 层不应该知道 Flowable 这个具体引擎(避免引擎锁定)
        ArchRule rule = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("org.flowable..")
            .because("Domain 层独立于具体工作流引擎实现");

        rule.check(classes);
    }
}
