package com.school.management.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 防回归:检查业务代码里不能再有 {@code triggerService.fire("XXX", ...)} 裸字符串.
 *
 * 必须使用 {@code *TriggerPoints} 常量类的常量引用,如:
 * <pre>
 *   triggerService.fire(EducationTriggerPoints.DORM_CHECKIN, ctx);  // ✓
 *   triggerService.fire("DORM_CHECKIN", ctx);                        // ✗
 * </pre>
 *
 * 裸字符串 ArchUnit 不支持(只能检查类签名),用文件扫描 + 正则.
 */
class NoMagicTriggerStringTest {

    private static final Pattern MAGIC_PATTERN =
            Pattern.compile("triggerService\\.fire\\s*\\(\\s*\"([^\"]+)\"");

    private static final Path SOURCE_ROOT = Path.of("src/main/java");

    @Test
    void no_magic_trigger_point_strings_in_business_code() throws IOException {
        if (!Files.exists(SOURCE_ROOT)) {
            System.err.println("源码目录不存在,跳过: " + SOURCE_ROOT.toAbsolutePath());
            return;
        }

        List<String> violations = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(SOURCE_ROOT)) {
            walk.filter(p -> p.toString().endsWith(".java"))
                // 测试辅助类可以放裸字符串
                .filter(p -> !p.toString().contains("/test/"))
                .forEach(p -> scanFile(p, violations));
        }

        assertTrue(violations.isEmpty(),
                "发现 " + violations.size() + " 处 triggerService.fire 裸字符串,请改用常量:\n"
                + String.join("\n", violations)
                + "\n\n请引用 *TriggerPoints 常量类(如 EducationTriggerPoints.DORM_CHECKIN)."
        );
    }

    private void scanFile(Path file, List<String> violations) {
        try {
            List<String> lines = Files.readAllLines(file);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                Matcher m = MAGIC_PATTERN.matcher(line);
                if (m.find()) {
                    violations.add(String.format("  %s:%d  %s",
                            file, i + 1, line.trim()));
                }
            }
        } catch (IOException e) {
            System.err.println("读文件失败: " + file + " - " + e.getMessage());
        }
    }
}
