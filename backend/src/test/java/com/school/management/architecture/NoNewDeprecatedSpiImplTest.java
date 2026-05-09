package com.school.management.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 守护: 禁止新代码 {@code implements} 7 个 @Deprecated 旧 SPI.
 *
 * <p>背景: Phase 2 (commit 7859a347) 把 8 SPI 收敛为
 * {@code PluginPackage + Contribution sealed} 契约, 7 旧 SPI 标 {@code @Deprecated}
 * 作为向下兼容层. 当前 ContributionDispatcher 仅"登记"不写 DB, 实际写入仍由
 * 旧 Registrar 完成 — 旧 SPI 实现仍是干活代码.
 *
 * <p>本测试不强制立刻迁移现存 42 个实现 (baseline 锁定), 但<strong>禁止债务增长</strong>:
 * 新代码 implements 旧 SPI = CI 失败.
 *
 * <p>新插件应该:
 * <pre>
 * &#64;Component
 * public class FooManifest implements PluginPackage {
 *     public Stream&lt;Contribution&gt; contribute() { return Stream.of(...); }
 * }
 * </pre>
 *
 * <p>想从 baseline 移除某文件? 删除该文件 (或迁移到 PluginPackage) 后, 把对应路径
 * 从 {@link #ALLOWED_BASELINE} 移除即可 — 让 baseline 自然收缩.
 */
class NoNewDeprecatedSpiImplTest {

    // Phase 2 W2.2: RelationTypePlugin 已彻底删除并迁移到 PluginPackage.contribute().
    // 剩余 6 个仍向下兼容, 待后续 phase 继续迁移.
    private static final String[] DEPRECATED_SPIS = {
        "EntityTypePlugin",
        "MenuContributionPlugin",
        "MessagingDomainPlugin",
        "DataScopePlugin",
        "PermissionProvider",
        "RolePresetPlugin"
    };

    /**
     * 现存合法实现快照 (2026-05-08). 7 个 SPI 接口自身文件不在此列 —
     * 它们的 javadoc 含 "class Xxx implements YyyPlugin" 示例, 用 class 声明
     * 正则可避免误匹配, 但保险起见排除接口自身路径 (见 {@link #isSpiInterfaceFile}).
     */
    private static final Set<String> ALLOWED_BASELINE = Set.of(
        // EntityTypePlugin (30+)
        "plugins/core/CoreUserTypesPlugin.java",
        "plugins/core/CorePlaceTypesPlugin.java",
        "plugins/education/ClassPlugin.java",
        "plugins/education/ClassroomPlugin.java",
        "plugins/education/CounselorPlugin.java",
        "plugins/education/DepartmentPlugin.java",
        "plugins/education/DormitoryPlugin.java",
        "plugins/education/EducationMiscTypesPlugin.java",
        "plugins/education/EducationPlaceTypesPlugin.java",
        "plugins/education/GradePlugin.java",
        "plugins/education/SchoolPlugin.java",
        "plugins/education/StudentPlugin.java",
        "plugins/education/SuperAdminPlugin.java",
        "plugins/education/TeacherPlugin.java",
        // MessagingDomainPlugin (11)
        "plugins/core/messaging/InspectionMessagingPlugin.java",
        "plugins/core/messaging/NotificationMessagingPlugin.java",
        "plugins/core/messaging/OrganizationMessagingPlugin.java",
        "plugins/core/messaging/PersonnelMessagingPlugin.java",
        "plugins/core/messaging/PlaceMessagingPlugin.java",
        "plugins/education/messaging/AcademicMessagingPlugin.java",
        "plugins/education/messaging/AttendanceMessagingPlugin.java",
        "plugins/education/messaging/AwardMessagingPlugin.java",
        "plugins/education/messaging/DisciplineMessagingPlugin.java",
        // DormitoryMessagingPlugin 已迁移到 PluginPackage (Track M3 reference) — 不在 baseline
        "plugins/education/messaging/EnrollmentMessagingPlugin.java",
        "plugins/education/messaging/GradeMessagingPlugin.java",
        "plugins/education/messaging/TeachingMessagingPlugin.java",
        // MenuContributionPlugin (2)
        "plugins/core/CoreMenuPlugin.java",
        "plugins/education/EducationMenuPlugin.java",
        // DataScopePlugin (1)
        "plugins/education/EducationDataScopePlugin.java",
        // PermissionProvider (2)
        "plugins/core/CorePermissionProvider.java",
        "plugins/education/EducationPermissionProvider.java",
        // RelationTypePlugin: 已在 W2.2 全部删除 — 无 baseline 条目.
        // RolePresetPlugin (2)
        "plugins/core/CoreRolePresetPlugin.java",
        "plugins/education/EducationRolePresetPlugin.java"
    );

    /** 匹配 {@code class Xxx ... implements ... <SpiName>} (允许多接口列表 + 跨行) */
    private static final Pattern CLASS_IMPLEMENTS_PATTERN = Pattern.compile(
        "class\\s+\\w+\\s+(?:[\\w<>,\\s]+\\s+)?implements\\s+(?:[\\w<>.,\\s]+,\\s*)?(" +
        String.join("|", DEPRECATED_SPIS) + ")\\b",
        Pattern.MULTILINE
    );

    private static final Path SRC_ROOT = Path.of("src/main/java");
    private static final Path PLUGINS_ROOT =
        Path.of("src/main/java/com/school/management/infrastructure/extension/plugins");

    @Test
    void noNewImplementersOfDeprecatedSpis() throws IOException {
        Set<String> found = new TreeSet<>();

        try (Stream<Path> files = Files.walk(SRC_ROOT)) {
            files
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p -> !isSpiInterfaceFile(p))  // SPI 自身有 javadoc 示例, 排除
                .forEach(p -> scan(p, found));
        }

        Set<String> newViolations = new TreeSet<>(found);
        newViolations.removeAll(ALLOWED_BASELINE);

        assertThat(newViolations)
            .as("新代码 implements @Deprecated SPI — 应改用 PluginPackage + Contribution\n" +
                "(如确属合理, 把路径加到 NoNewDeprecatedSpiImplTest.ALLOWED_BASELINE 并写明理由)")
            .isEmpty();

        // 同时报告"baseline 已不存在的文件" — 提示开发者收缩 baseline (软提示, 不 fail)
        Set<String> stale = new TreeSet<>(ALLOWED_BASELINE);
        stale.removeAll(found);
        if (!stale.isEmpty()) {
            System.out.println("[NoNewDeprecatedSpiImplTest] baseline 中以下文件已不再 implements 旧 SPI, " +
                    "可从 ALLOWED_BASELINE 移除以收缩兼容层: " + stale);
        }
    }

    private static boolean isSpiInterfaceFile(Path p) {
        String s = p.toString().replace('\\', '/');
        for (String spi : DEPRECATED_SPIS) {
            if (s.endsWith("/infrastructure/extension/" + spi + ".java")) return true;
        }
        return false;
    }

    private static void scan(Path file, Set<String> out) {
        try {
            String content = Files.readString(file);
            if (CLASS_IMPLEMENTS_PATTERN.matcher(content).find()) {
                // 相对化到 plugins/ 下短路径, 与 baseline 对齐
                String s = file.toString().replace('\\', '/');
                int idx = s.indexOf("/extension/plugins/");
                if (idx >= 0) {
                    out.add(s.substring(idx + "/extension/".length()));  // 取 "plugins/..." 起
                } else {
                    // 不在 plugins/ 下却 implements 旧 SPI — 直接报全路径
                    out.add(s);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + file, e);
        }
    }
}
