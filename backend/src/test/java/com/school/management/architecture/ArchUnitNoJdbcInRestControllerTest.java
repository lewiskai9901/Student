package com.school.management.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * 全局守护: interfaces/rest/** 下任何 controller 都不得依赖 JdbcTemplate (N2, 2026-05-20).
 *
 * <p>背景: 此前 jdbc 反 DDD 守护是「按模块」的 — ArchUnitAssetControllerGuardTest /
 * ArchUnitEducationRestGuardTest / ArchUnitEventControllerGuardTest 各自只盯自己的包.
 * 结果 interfaces/rest/system/** 等从未被守, jdbc 在那里重新长回来 (审计发现 19 个
 * controller 共 ~123 处 jdbc 调用). N2.2 已清零 6 个 system/ controller, baseline 19→13.
 *
 * <p>本测试是全局 ratchet: BASELINE 记录现存违规 controller, 任何 PR 不得
 * 新增违规 (新增 = test 红), 也不得让 baseline 失效 (修好一个就必须从 baseline
 * 删一个, 否则 test 红提醒). baseline 只能单调减少, 目标清零.
 *
 * <p>用各模块专属守 (asset/education/event 已清零) 仍保留 — 它们守的是「已清零必须
 * 维持 0」, 本测试守的是「全局不许新增 + 存量单调下降」.
 */
class ArchUnitNoJdbcInRestControllerTest {

    /**
     * 存量违规 baseline — 已全部清零 (N2.2 + N2.3, 2026-05-20). 必须保持为空:
     * interfaces/rest/** 下任何 controller 依赖 JdbcTemplate 一律失败, 不允许加豁免.
     * jdbc CRUD 必须走 application/ 的 *ApplicationService.
     */
    private static final Set<String> JDBC_CONTROLLER_BASELINE = Set.of();

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.school.management");
    }

    @Test
    void noRestController_dependsOnJdbcTemplate_beyondBaseline() {
        String jdbcName = JdbcTemplate.class.getName();
        Set<String> actual = new TreeSet<>();
        for (JavaClass c : classes) {
            if (!c.getPackageName().contains(".interfaces.rest")) continue;
            boolean dependsOnJdbc = c.getDirectDependenciesFromSelf().stream()
                .anyMatch(d -> d.getTargetClass().getName().equals(jdbcName));
            if (dependsOnJdbc) actual.add(c.getName());
        }

        Set<String> newViolations = new TreeSet<>(actual);
        newViolations.removeAll(JDBC_CONTROLLER_BASELINE);

        Set<String> staleBaseline = new TreeSet<>(JDBC_CONTROLLER_BASELINE);
        staleBaseline.removeAll(actual);

        if (!newViolations.isEmpty()) {
            fail(String.format(
                "%n%d 个 REST controller 新增了对 JdbcTemplate 的直接依赖 (反 DDD):%n  %s%n"
                + "修复: 把 jdbc CRUD 下沉到 application/ 的 *ApplicationService, controller 只留 HTTP 绑定.",
                newViolations.size(), String.join("%n  ", newViolations)));
        }
        if (!staleBaseline.isEmpty()) {
            fail(String.format(
                "%n%d 个 controller 已不再依赖 JdbcTemplate — 请从 JDBC_CONTROLLER_BASELINE 删除 (ratchet 单调下降):%n  %s",
                staleBaseline.size(), String.join("%n  ", staleBaseline)));
        }
    }
}
