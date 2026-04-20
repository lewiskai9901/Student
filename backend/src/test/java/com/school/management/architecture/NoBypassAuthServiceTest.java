package com.school.management.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 守护 application/** 不得绕过 AuthorizationService 直访 AccessRelationRepository
 * / AccessRelationMapper 做授权判断.
 *
 * 允许直访的类 (白名单): 做 CRUD lifecycle 管理, 不是授权判断, 手工审核过.
 * 新增的 application/** 类如果沾这俩 repo 会被此测试拦住, 必须明确分析:
 *   - 如果是"判断谁是否有某关系" → 走 AuthorizationService.checkDirect / check
 *   - 如果是"领域 lifecycle 管理" → 加到白名单并在类级 javadoc 解释 (不要无脑加)
 */
class NoBypassAuthServiceTest {

    // 合法 CRUD lifecycle callsites (手工审核通过), 新加请慎重
    // 这些类做领域副作用 (创建/删除时绑关系, 清理), 不是授权判断。
    //   - OrgUnitApplicationService      : 删组织时级联清关系
    //   - OrgMemberService               : 成员 add/remove lifecycle (exists 判断已迁至 AuthorizationService)
    //   - UniversalPlaceApplicationService: 入住/退房写关系
    //   - UserApplicationService         : 用户创建/删除时的关系管理
    private static final String WHITELIST_REGEX =
        "com\\.school\\.management\\.application\\.organization\\.OrgUnitApplicationService"
        + "|com\\.school\\.management\\.application\\.organization\\.OrgMemberService"
        + "|com\\.school\\.management\\.application\\.place\\.UniversalPlaceApplicationService"
        + "|com\\.school\\.management\\.application\\.user\\.UserApplicationService";

    @Test
    void applicationLayerDoesNotBypassAuthServiceForJudgment() {
        JavaClasses imported = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("com.school.management.application");

        ArchRule rule = noClasses()
            .that().resideInAPackage("..application..")
            .and().resideOutsideOfPackage("..application.access..")
            .and().haveNameNotMatching(WHITELIST_REGEX)
            .should().dependOnClassesThat().haveSimpleNameEndingWith("AccessRelationMapper")
            .orShould().dependOnClassesThat().haveSimpleNameEndingWith("AccessRelationRepository");

        rule.check(imported);
    }
}
