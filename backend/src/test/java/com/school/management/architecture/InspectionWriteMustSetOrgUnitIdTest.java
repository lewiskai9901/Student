package com.school.management.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 守护: 13 张声明 org_unit_id 列的 inspection 表, 其 PO + 写入路径必须双重满足:
 *
 *   1. PO 类声明 `orgUnitId` 字段 (类型 Long, MyBatis-Plus @TableField 映射 org_unit_id)
 *   2. 写入路径 (RepositoryImpl.toPO 或 Builder/工厂) 至少存在一处
 *      `setOrgUnitId(` 调用 — 否则 INSERT 时 org_unit_id 永远是 null,
 *      DataPermissionInterceptor 注入的 `org_unit_id IN (...)` 过滤会全部漏掉新行.
 *
 * 表清单来源: database/migrations/V20260501_1__inspection_data_permission_columns.sql
 *
 * 新增 inspection 表需要数据权限过滤? 把表名加到 EXPECTED_TABLES_WITH_ORG_UNIT,
 * 测试会强制 PO + 写入路径补齐.
 *
 * 新增 inspection 表 *不需要* 数据权限过滤 (tenant 级配置/全局规则)?
 * 不动这个测试, 但要在 NotInWhitelistReason 文档里说明.
 */
class InspectionWriteMustSetOrgUnitIdTest {

    /** V20260501_1 migration 列出的 13 张需要 org_unit_id 的表 (escalation_policies 是 tenant 级, 显式排除) */
    private static final String[] EXPECTED_TABLES_WITH_ORG_UNIT = {
        "insp_projects",
        "insp_tasks",
        "insp_submission_details",
        "insp_evidences",
        "insp_project_inspectors",
        "insp_corrective_cases",
        "insp_corrective_subtasks",
        "insp_alerts",
        "insp_alert_rules",
        "insp_violation_records",
        "insp_submission_observations",
        "insp_inspector_summaries",
        "insp_corrective_summaries",
        "insp_item_frequency_summaries"
    };

    private static final Path PERSISTENCE_ROOT =
        Path.of("src/main/java/com/school/management/infrastructure/persistence/inspection");

    private static final Path SRC_ROOT =
        Path.of("src/main/java/com/school/management");

    /** 匹配 `@TableName("insp_xxx")` (允许单/双引号、注解参数、空白) */
    private static final Pattern TABLE_NAME_PATTERN =
        Pattern.compile("@TableName\\s*\\(\\s*(?:value\\s*=\\s*)?[\"']([^\"']+)[\"']");

    /** 匹配 `private ... Long orgUnitId` / `Long orgUnitId;` (带或不带访问修饰符) */
    private static final Pattern ORG_UNIT_FIELD_PATTERN =
        Pattern.compile("\\bLong\\s+orgUnitId\\b");

    @Test
    void everyInspectionPoMustDeclareOrgUnitIdField() throws IOException {
        List<String> violations = new ArrayList<>();
        Set<String> tablesWithPoFound = new LinkedHashSet<>();

        try (Stream<Path> files = Files.walk(PERSISTENCE_ROOT)) {
            files
                .filter(p -> p.toString().endsWith("PO.java"))
                .forEach(p -> {
                    try {
                        String content = Files.readString(p);
                        Matcher m = TABLE_NAME_PATTERN.matcher(content);
                        if (!m.find()) return;
                        String tableName = m.group(1);
                        if (!isExpected(tableName)) return;
                        tablesWithPoFound.add(tableName);
                        if (!ORG_UNIT_FIELD_PATTERN.matcher(content).find()) {
                            violations.add(String.format(
                                "PO %s (@TableName=\"%s\") 缺 `Long orgUnitId` 字段",
                                p.getFileName(), tableName));
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to read " + p, e);
                    }
                });
        }

        // 期望表里若某张找不到对应的 PO, 也算违规 (说明 PO 没建或 @TableName 写错)
        for (String expected : EXPECTED_TABLES_WITH_ORG_UNIT) {
            if (!tablesWithPoFound.contains(expected)) {
                violations.add(String.format(
                    "未找到 @TableName=\"%s\" 的 PO — 是否漏建 PO 或表名拼错?", expected));
            }
        }

        assertThat(violations)
            .as("inspection PO orgUnitId 字段缺失")
            .isEmpty();
    }

    @Test
    void everyInspectionPoMustHaveSetOrgUnitIdCallSomewhere() throws IOException {
        // 收集所有 inspection PO 的简单类名 (e.g., InspProjectPO, InspTaskPO)
        List<String> poClassNames = new ArrayList<>();
        try (Stream<Path> files = Files.walk(PERSISTENCE_ROOT)) {
            files
                .filter(p -> p.toString().endsWith("PO.java"))
                .forEach(p -> {
                    try {
                        String content = Files.readString(p);
                        Matcher m = TABLE_NAME_PATTERN.matcher(content);
                        if (!m.find()) return;
                        String tableName = m.group(1);
                        if (!isExpected(tableName)) return;
                        String fileName = p.getFileName().toString();
                        poClassNames.add(fileName.substring(0, fileName.length() - ".java".length()));
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to read " + p, e);
                    }
                });
        }

        // 收集全部 java 源文件内容 (一次性, 后续多次 grep)
        List<String> allSources = new ArrayList<>();
        try (Stream<Path> files = Files.walk(SRC_ROOT)) {
            files
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p -> !p.toString().replace('\\', '/').contains("/architecture/"))
                .forEach(p -> {
                    try { allSources.add(Files.readString(p)); }
                    catch (IOException e) { throw new RuntimeException(e); }
                });
        }

        // 对每个 PO, 要求至少一处源码同时出现 PO 类名 + .setOrgUnitId(
        // (设 toPO 方法里 `po.setOrgUnitId(...)`, po 是该 PO 类型的局部变量, 类名上下文中能匹配到)
        List<String> violations = new ArrayList<>();
        for (String poClass : poClassNames) {
            boolean found = allSources.stream().anyMatch(src ->
                src.contains(poClass) && src.contains(".setOrgUnitId("));
            if (!found) {
                violations.add(String.format(
                    "%s 的写入路径里找不到 .setOrgUnitId(...) — 新行 INSERT 后 org_unit_id 将是 NULL",
                    poClass));
            }
        }

        assertThat(violations)
            .as("inspection PO 写入路径未 set orgUnitId")
            .isEmpty();
    }

    private static boolean isExpected(String tableName) {
        for (String t : EXPECTED_TABLES_WITH_ORG_UNIT) {
            if (t.equals(tableName)) return true;
        }
        return false;
    }
}
