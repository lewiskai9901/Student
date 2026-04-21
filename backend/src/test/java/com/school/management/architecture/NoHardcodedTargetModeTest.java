package com.school.management.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 守护: MessageDispatcher 不得出现硬编码 target_mode 字符串字面量.
 * 所有模式必须通过 TargetModeResolver SPI 贡献, 通过 Map 查找 dispatch.
 *
 * 4 个 core resolver 类 (BySubjectTargetMode 等) 内部 modeCode() 返回 literal 是合法的,
 * 本 test 只扫 MessageDispatcher 文件, 不扫 resolver 实现文件.
 */
class NoHardcodedTargetModeTest {

    private static final String[] FORBIDDEN = {
        "\"BY_SUBJECT\"", "\"BY_ROLE\"", "\"BY_RELATION\"", "\"BY_FEATURE\""
    };

    @Test
    void messageDispatcherHasNoHardcodedTargetMode() throws IOException {
        Path file = Path.of("src/main/java/com/school/management/application/message/MessageDispatcher.java");
        assertThat(file).as("MessageDispatcher.java exists").isRegularFile();
        String content = Files.readString(file);

        List<String> violations = new ArrayList<>();
        String[] lines = content.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int commentIdx = line.indexOf("//");
            String code = commentIdx >= 0 ? line.substring(0, commentIdx) : line;
            for (String lit : FORBIDDEN) {
                if (code.contains(lit)) {
                    violations.add((i + 1) + ": " + line.trim());
                }
            }
        }

        assertThat(violations)
            .as("MessageDispatcher 应通过 TargetModeResolver SPI 而非硬编码字符串")
            .isEmpty();
    }
}
