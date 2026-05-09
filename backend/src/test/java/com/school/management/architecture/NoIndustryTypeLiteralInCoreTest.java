package com.school.management.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 守护: core Java 代码 (非 plugins.* / 非 inspection.* 目录)
 * 不得出现行业类型字符串字面量.
 *
 * 这些类型码由对应行业插件 (EducationPlugin 等) 在启动时
 * 通过 EntityTypeContribution 动态注册到 entity_type_configs 表. core 代码
 * 不应感知, 也不能硬编码引用.
 *
 * 追加新行业类型, 在 FORBIDDEN_LITERALS 加条目即可.
 */
class NoIndustryTypeLiteralInCoreTest {

    /** 行业专属类型码 (前后加引号以匹配字面量, 规避中文变量名等) */
    private static final String[] FORBIDDEN_LITERALS = {
        "\"STUDENT\"",
        "\"TEACHER\"",
        "\"CLASS\"",
        "\"GRADE\"",
        "\"MAJOR\"",
        "\"DORMITORY\"",
        "\"CLASSROOM\""
    };

    /** 排除的子包 (插件 / 仍保留在 core 的历史包) */
    private static final String[] EXCLUDED_PATH_FRAGMENTS = {
        "/plugins/",        // 行业插件
        "/test/",           // 测试代码自身可以写
        "/architecture/"    // 本测试文件也会包含字面量
        // 检查平台 (inspection/) 已于 2026-04-25 完成行业去耦, 纳入守护
    };

    @Test
    void coreJavaSourceHasNoIndustryTypeLiteral() throws IOException {
        Path srcRoot = Path.of("src/main/java/com/school/management");
        assertThat(srcRoot).as("src root exists").isDirectory();

        List<String> violations = new ArrayList<>();

        try (Stream<Path> files = Files.walk(srcRoot)) {
            files
                .filter(p -> p.toString().endsWith(".java"))
                .filter(NoIndustryTypeLiteralInCoreTest::isCorePath)
                .forEach(p -> scan(p, violations));
        }

        assertThat(violations)
            .as("core Java 代码包含行业类型字面量 — 应迁至对应插件")
            .isEmpty();
    }

    private static boolean isCorePath(Path p) {
        String s = p.toString().replace('\\', '/');
        for (String frag : EXCLUDED_PATH_FRAGMENTS) {
            if (s.contains(frag)) return false;
        }
        return true;
    }

    private static void scan(Path file, List<String> out) {
        try {
            String content = Files.readString(file);
            // 先移除块注释 /* ... */ (包括 Javadoc /** ... */), 用同等长度空格替换
            // 以保留行号 (换行保留)
            String stripped = stripBlockComments(content);
            String[] lines = stripped.split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                // 去掉单行注释 //
                int commentIdx = line.indexOf("//");
                String code = commentIdx >= 0 ? line.substring(0, commentIdx) : line;
                for (String lit : FORBIDDEN_LITERALS) {
                    if (code.contains(lit)) {
                        out.add(file + ":" + (i + 1) + "  → " + lit + "  // " + line.trim());
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + file, e);
        }
    }

    /**
     * 移除 /* ... *&#47; 块注释, 保留换行以维持行号对应.
     * 非注释区的 '/' '*' 字符原样保留.
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
                    sb.append("  "); // 吃掉 */, 换两个空格
                    i++;
                    inBlock = false;
                } else if (c == '\n') {
                    sb.append('\n');
                } else {
                    sb.append(' ');
                }
                continue;
            }

            // 不在块注释内: 处理字符串/字符字面量, 避免误伤 "/*"
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
