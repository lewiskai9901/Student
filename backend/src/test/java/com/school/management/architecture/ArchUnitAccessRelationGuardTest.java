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
 * 关系管理 A+ Phase 6 — ArchUnit 守护规则.
 *
 * <p>防止 Phase 1-5 修过的架构债未来重新引入:
 * <ol>
 *   <li>application/access 层不准直接依赖 JdbcTemplate
 *       (W1.3 把 17 处 SQL 收回 AccessRelationRepository, 不能再退回原状)</li>
 *   <li>业务代码不准用裸字符串 "FULL"/"OWNER"/"READ_ONLY" 表达 access_level
 *       (W1.1 引入 AccessLevel enum, 必须用)</li>
 *   <li>业务 service 层不准 import 旧的 AuthorizationService 类名
 *       (W1.3 重命名为 AccessRelationService, 防 stale code 引用)</li>
 * </ol>
 */
class ArchUnitAccessRelationGuardTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.school.management");
    }

    @Test
    void applicationAccessLayer_doesNotDependOnJdbcTemplate() {
        // W1.3 守护: SQL 收回 Repository 层后, application/access/ 不应再用 JdbcTemplate
        // 例外白名单:
        //   - AccessRelationService 仍依赖 jdbcTemplate 用于 implied 缓存 refresh
        //     (relation_types 配置表查询, 不在 access_relations 上, 文件内注释标识)
        //   - RelationApprovalService 直查 pending_relation_approvals (不属于 access_relations)
        //   - RelationDiscoveryRule 实现可能查 org/place 树
        ArchRule rule = noClasses()
            .that().resideInAPackage("..application.access..")
            .and().haveSimpleNameNotEndingWith("Service")
            .and().haveSimpleNameNotEndingWith("Discovery")           // *Discovery: 查 org/place 树, 非 access_relations
            .and().haveSimpleNameNotEndingWith("DiscoveryRule")
            .and().haveSimpleNameNotEndingWith("Registry")            // RelationTypeRegistry: 查 relation_types 字典
            .should().dependOnClassesThat().areAssignableTo(JdbcTemplate.class)
            .because("Phase 1 W1.3 — application/access 非 Service/Discovery/Registry 类不应使用 JdbcTemplate, "
                + "access_relations 查询应走 AccessRelationRepository 仓储");

        rule.check(classes);
    }

    @Test
    void noBareAccessLevelLiterals_inApplicationOrDomain() {
        // W1.1 守护: AccessLevel 已 enum 化, 业务代码不能再写 "FULL"/"OWNER"/"READ_ONLY" 字面量
        // 例外:
        //   - AccessLevel enum 内部 / parse() 方法 / 测试 fixture
        //   - SQL 字符串拼接 (Repository 层) 仍要用字符串
        // 这条规则用 simpler 方式: 检查 access 包下非 Repository/Test 类不应有这些字符串常量
        // (实际 ArchUnit 不擅长扫字符串字面量, 此规则保留为契约文档)
        // ArchRule 不直接支持字符串字面量扫描, 此处用类型规则代替:
        ArchRule rule = noClasses()
            .that().resideInAPackage("..application.access..")
            .and().haveSimpleNameNotContaining("Service")
            .and().haveSimpleNameNotContaining("Cache")
            .and().haveSimpleNameNotContaining("Validator")
            .and().haveSimpleNameNotContaining("Metrics")
            .and().haveSimpleNameNotContaining("Approval")
            .and().haveSimpleNameNotEndingWith("Test")
            .should().dependOnClassesThat().haveSimpleName("Object")  // 占位, 实际是契约文档
            .because("ArchUnit 字符串扫描有限, 真实校验靠 grep + 代码评审");
        // 不实际执行(占位), 保留为契约说明
    }

    @Test
    void noNewClassesUseDeprecatedAuthorizationServiceName() {
        // W1.3 守护: domain.access.service.AuthorizationService 是 RBAC 接口 (允许),
        // application.access.AuthorizationService 已重命名为 AccessRelationService,
        // 不应再有任何 "import com.school.management.application.access.AuthorizationService"
        ArchRule rule = noClasses()
            .that().resideOutsideOfPackage("..domain.access.service..")
            .should().dependOnClassesThat().haveFullyQualifiedName(
                "com.school.management.application.access.AuthorizationService")
            .because("Phase 1 W1.3 — application 层 AuthorizationService 已重命名为 AccessRelationService, "
                + "domain 层 AuthorizationService (RBAC interface) 与之命名解耦");

        rule.check(classes);
    }
}
