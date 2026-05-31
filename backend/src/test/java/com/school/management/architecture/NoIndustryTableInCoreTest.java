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

    /** 行业扩展表名 (用户档案表). */
    private static final String[] FORBIDDEN_TABLES = {
        "user_student",
        "user_teacher",
        "user_counselor"
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
     * 已知遗留豁免 (按文件名结尾匹配). 这些核心文件直接查 user_teacher, 是归属统一重构
     * <b>之前</b>就存在的债务 —— 整个教师档案服务+控制器本应迁入 education 插件
     * (项目规则: 教育业务代码放 plugins/education/**). 该迁移是独立大工程, 不在本守护任务范围.
     * 守护先收口防 <b>新增</b> 泄漏 + 已删列残留; 此豁免随后续"档案服务下沉插件"任务清零.
     *
     * <p>TODO(arch): 将 TeacherProfileApplicationService / TeacherProfileController 迁入
     * plugins/education/ 后删除此豁免.
     */
    private static final String[] LEGACY_EXEMPT_FILES = {
        "TeacherProfileApplicationService.java"
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
