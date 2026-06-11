package com.school.management.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 守护: core Java 代码 (非 plugins.* 目录) 不得在 <b>SQL 上下文</b> 中直接引用
 * 行业扩展表 (user_student / user_teacher / user_counselor 等用户档案表).
 *
 * <p>归属统一重构 (2026-05-31) 后, 通用核心一律通过 {@code access_relations} 的
 * {@code member} 关系解析用户归属, 不再读行业档案表的 {@code org_unit_id} 列, 也不应
 * {@code FROM}/{@code JOIN} 这些表来做核心查询. 行业查询应留在对应插件
 * ({@code infrastructure/extension/plugins/**}) 内.
 *
 * <p>判定: 源码行 (去注释后) 含 {@code from <表>} 或 {@code join <表>} (大小写不敏感)
 * 即违规. 注释行 (// 单行、块注释、Javadoc) 放行, 避免误报说明性文字.
 *
 * <p>追加新行业表, 在 {@link #FORBIDDEN_TABLES} 加条目即可.
 */
class NoIndustryTableInCoreTest {

    /** 行业扩展表名 (用户档案表 + 教育业务表). */
    private static final String[] FORBIDDEN_TABLES = {
        // 用户档案表
        "user_student",
        "user_teacher",
        "user_counselor",
        // 教育业务表 — 2026-06-02 教师/教务垂直 + 看板教务统计迁出核心后纳入守护,
        // 防止核心再次直接 FROM/JOIN 这些行业表 (应走插件贡献点 / MembershipResolver)
        "classes",
        "courses",
        "teacher_assignments",
        "teacher_course_qualifications",
        "semesters",
        "teaching_tasks",
        "schedule_instances",
        "schedule_entries",
        "curriculum_plans",
        "student_grades",
        "exam_arrangements",
        // 2026-06-12 dashboard 去教育侵入补充: 学术结构表
        "majors",
        "grades",
        "grade_directors",
        "major_directions"
    };

    /** SQL 上下文关键字 — 只在 from/join 后紧跟表名才算引用. */
    private static final String[] SQL_KEYWORDS = { "from", "join" };

    /** 排除的子包 (插件 / 测试自身 / 本守护文件). */
    private static final String[] EXCLUDED_PATH_FRAGMENTS = {
        "/plugins/",        // 行业插件 — 合法引用行业表
        "/test/",           // 测试代码自身
        "/architecture/"    // 本测试文件包含表名字面量
    };

    /**
     * 已知遗留豁免 (按文件名结尾匹配). 目前为空 —— 2026-06-02 教师档案/任职垂直迁入
     * plugins/education、org-impact 的 classCount 改 OrgImpactContributor 贡献点后,
     * 核心已无任何对教育业务表的 FROM/JOIN, 全部豁免清零。
     */
    private static final String[] LEGACY_EXEMPT_FILES = {
    };

    @Test
    void coreJavaSourceHasNoIndustryTableInSql() throws IOException {
        Path srcRoot = Path.of("src/main/java/com/school/management");
        assertThat(srcRoot).as("src root exists").isDirectory();

        List<String> violations = new ArrayList<>();

        try (Stream<Path> files = Files.walk(srcRoot)) {
            files
                .filter(p -> p.toString().endsWith(".java"))
                .filter(NoIndustryTableInCoreTest::isCorePath)
                .forEach(p -> scan(p, violations));
        }

        assertThat(violations)
            .as("core Java 代码在 SQL 中直接引用行业扩展表 — 核心应走 access_relations member 关系")
            .isEmpty();
    }

    /**
     * 规则 2 (2026-06-12): 核心代码不得出现行业表名的<b>带引号字符串字面量</b>
     * (如 {@code "classes"})。
     *
     * <p>背景: 规则 1 只扫 {@code FROM/JOIN <表>} 文本, 表名作为<b>方法参数</b>传入再
     * 拼接 SQL (如 {@code countByOrgColumn("classes", ...)} → {@code "FROM " + table})
     * 可完全绕过 — DashboardOverviewQueryService 的 majors/classes 统计正是这样漏网的。
     * 核心没有任何正当理由持有行业表名字面量; 需要行业统计走插件贡献点。
     */
    /**
     * 规则 2 的登记豁免 (按文件名结尾匹配) — 均为"基础设施注册表"性质, 非 SQL 查询;
     * 新文件命中一律先修, 勿无脑加豁免:
     * <ul>
     *   <li>RedisConfig — 缓存区名 TTL 注册 (cache region 撞行业表名, 非表引用)</li>
     *   <li>TenantInterceptor — 租户列表清单 (多租户休眠中; 启用时应改插件贡献)</li>
     *   <li>DataPermissionSimulateController — TODO 真侵入: simulate 元数据 switch
     *       硬编码 school_class→classes, 应改 SimulateMeta 贡献点 (独立工作项)</li>
     * </ul>
     */
    private static final String[] QUOTED_LITERAL_EXEMPT_FILES = {
        "RedisConfig.java",
        "TenantInterceptor.java",
        "DataPermissionSimulateController.java"
    };

    @Test
    void coreJavaSourceHasNoQuotedIndustryTableLiteral() throws IOException {
        Path srcRoot = Path.of("src/main/java/com/school/management");
        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(srcRoot)) {
            files
                .filter(p -> p.toString().endsWith(".java"))
                .filter(NoIndustryTableInCoreTest::isCorePath)
                .filter(p -> {
                    String s = p.toString().replace('\\', '/');
                    for (String exempt : QUOTED_LITERAL_EXEMPT_FILES) {
                        if (s.endsWith("/" + exempt)) return false;
                    }
                    return true;
                })
                .forEach(p -> scanQuotedLiterals(p, violations));
        }
        assertThat(violations)
            .as("core Java 代码持有行业表名字符串字面量 — 参数化拼接同样是对行业表的引用, 应走插件贡献点")
            .isEmpty();
    }

    private static void scanQuotedLiterals(Path file, List<String> out) {
        try {
            String stripped = stripBlockComments(Files.readString(file));
            String[] lines = stripped.split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                String trimmed = lines[i].trim();
                if (trimmed.startsWith("*") || trimmed.startsWith("//")) continue;
                int commentIdx = lines[i].indexOf("//");
                String code = commentIdx >= 0 ? lines[i].substring(0, commentIdx) : lines[i];
                for (String table : FORBIDDEN_TABLES) {
                    if (code.contains("\"" + table + "\"")) {
                        out.add(file + ":" + (i + 1) + "  → \"" + table + "\"  // " + trimmed);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + file, e);
        }
    }

    private static boolean isCorePath(Path p) {
        String s = p.toString().replace('\\', '/');
        for (String frag : EXCLUDED_PATH_FRAGMENTS) {
            if (s.contains(frag)) return false;
        }
        for (String exempt : LEGACY_EXEMPT_FILES) {
            if (s.endsWith("/" + exempt) || s.endsWith(exempt)) return false;
        }
        return true;
    }

    private static void scan(Path file, List<String> out) {
        try {
            String content = Files.readString(file);
            // 去块注释 /* ... */ 与 Javadoc, 保留行号
            String stripped = stripBlockComments(content);
            String[] lines = stripped.split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                String original = lines[i];
                String trimmed = original.trim();
                // 防御: Javadoc 续行 (块注释 stripper 若因奇数引号等边界 desync 漏网),
                // 以 '*' 或 '//' 起始的行视为注释, 直接放行.
                if (trimmed.startsWith("*") || trimmed.startsWith("//")) continue;
                // 去单行注释 //
                int commentIdx = original.indexOf("//");
                String code = commentIdx >= 0 ? original.substring(0, commentIdx) : original;
                String lower = code.toLowerCase(Locale.ROOT);
                for (String table : FORBIDDEN_TABLES) {
                    for (String kw : SQL_KEYWORDS) {
                        // 匹配 "from user_student" / "join user_student" (允许多空格)
                        if (containsSqlRef(lower, kw, table)) {
                            out.add(file + ":" + (i + 1) + "  → " + kw + " " + table
                                    + "  // " + original.trim());
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + file, e);
        }
    }

    /**
     * 判定 lower (已小写) 中是否出现 "{keyword} {table}" 的 SQL 引用,
     * 允许 keyword 与 table 间有 1+ 空白. 不要求行首, 兼容多行 SQL 拼接片段.
     */
    private static boolean containsSqlRef(String lower, String keyword, String table) {
        int idx = 0;
        while ((idx = lower.indexOf(keyword, idx)) >= 0) {
            int p = idx + keyword.length();
            // keyword 后必须是空白 (排除 "fromage" 之类), 且前面不是标识符字符 (排除 "subfrom")
            boolean leftOk = idx == 0 || !isIdentChar(lower.charAt(idx - 1));
            if (leftOk && p < lower.length() && Character.isWhitespace(lower.charAt(p))) {
                // 跳过空白
                while (p < lower.length() && Character.isWhitespace(lower.charAt(p))) p++;
                if (lower.startsWith(table, p)) {
                    int after = p + table.length();
                    // table 后必须是非标识符字符或行尾 (排除 user_student_xxx)
                    if (after >= lower.length() || !isIdentChar(lower.charAt(after))) {
                        return true;
                    }
                }
            }
            idx = p;
        }
        return false;
    }

    private static boolean isIdentChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    /**
     * 移除块注释 (含 Javadoc), 保留换行以维持行号. 字符串/字符字面量内的 "/*" 不当注释处理.
     */
    private static String stripBlockComments(String src) {
        StringBuilder sb = new StringBuilder(src.length());
        boolean inBlock = false;
        boolean inString = false;
        boolean inChar = false;
        int n = src.length();
        for (int i = 0; i < n; i++) {
            char c = src.charAt(i);
            char next = i + 1 < n ? src.charAt(i + 1) : '\0';

            if (inBlock) {
                if (c == '*' && next == '/') {
                    sb.append("  ");
                    i++;
                    inBlock = false;
                } else if (c == '\n') {
                    sb.append('\n');
                } else {
                    sb.append(' ');
                }
                continue;
            }

            if (!inChar && c == '"' && (i == 0 || src.charAt(i - 1) != '\\')) {
                inString = !inString;
                sb.append(c);
                continue;
            }
            if (!inString && c == '\'' && (i == 0 || src.charAt(i - 1) != '\\')) {
                inChar = !inChar;
                sb.append(c);
                continue;
            }

            if (!inString && !inChar && c == '/' && next == '*') {
                sb.append("  ");
                i++;
                inBlock = true;
                continue;
            }

            sb.append(c);
        }
        return sb.toString();
    }
}
