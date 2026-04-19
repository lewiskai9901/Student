package com.school.management.architecture;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 插件声明覆盖度测试 — 防止"代码里引用了 X, 但没插件声明 X"的沉默 bug.
 *
 * 同一模式在多个 SPI 都可能失守:
 *   - @CasbinAccess(resource=X, action=Y)      → PermissionProvider 必须声明 X:Y
 *   - triggerService.fire(CONST, ...)          → MessagingDomainPlugin.triggerPoints() 必须 new TriggerPointDef(CONST,...)
 *   - @PreAuthorize("hasRole('X')")            → RolePresetPlugin 必须 RolePresetDef.of("X",...)
 *
 * 实现方式: 纯文本扫 src/main/java, 不走 Spring context (启动快, 无需 DB).
 *
 * 已有的互补测试:
 *   - NoMagicTriggerStringTest  禁止 triggerService.fire("字面量", ...)
 *   - ArchUnitPluginArchitectureTest.no_class_may_import_deleted_PermissionConstants
 *   - memory 约定: 禁 "STUDENT".equals(user.getType()) — 用 hasFeature()
 */
class PluginDeclarationCoverageTest {

    private static final Path SOURCE_ROOT = Paths.get("src/main/java");
    private static List<String> allJavaSource;

    @BeforeAll
    static void loadAllSource() throws IOException {
        try (Stream<Path> stream = Files.walk(SOURCE_ROOT)) {
            allJavaSource = stream
                .filter(p -> p.toString().endsWith(".java"))
                .map(PluginDeclarationCoverageTest::readFileSafe)
                .collect(Collectors.toList());
        }
    }

    private static String readFileSafe(Path p) {
        try { return Files.readString(p); } catch (Exception e) { return ""; }
    }

    // =============================================================
    // Test 1: @CasbinAccess 覆盖度 — 代码里引用的权限必须有 PermissionProvider 声明
    // =============================================================
    @Test
    @DisplayName("每个 @CasbinAccess(resource, action) 都必须有 PermissionProvider 声明对应的 'resource:action' 权限码")
    void every_casbin_access_must_be_declared_by_permission_provider() {
        // 匹配: @CasbinAccess(resource = "XXX", action = "YYY")
        Pattern refPattern = Pattern.compile(
            "@CasbinAccess\\s*\\(\\s*resource\\s*=\\s*\"([^\"]+)\"\\s*,\\s*action\\s*=\\s*\"([^\"]+)\"");
        Set<String> referenced = new TreeSet<>();
        for (String src : allJavaSource) {
            Matcher m = refPattern.matcher(src);
            while (m.find()) {
                referenced.add(m.group(1) + ":" + m.group(2));
            }
        }

        // 扫 PermissionProvider 实现文件, 匹配 of("XXX", ...) 或 PermissionDef.of("XXX", ...)
        Pattern declPattern = Pattern.compile(
            "(?:PermissionDef\\.)?of\\s*\\(\\s*\"([a-z_][a-zA-Z0-9:_\\.-]*)\"");
        Set<String> declared = new TreeSet<>();
        for (String src : allJavaSource) {
            if (!src.contains("implements PermissionProvider")) continue;
            Matcher m = declPattern.matcher(src);
            while (m.find()) declared.add(m.group(1));
        }

        Set<String> missing = new TreeSet<>(referenced);
        missing.removeAll(declared);
        assertTrue(missing.isEmpty(),
            String.format("%n以下 %d 个 @CasbinAccess 权限码在代码里被引用, 但没有任何 PermissionProvider 声明:%n  %s%n" +
                "修复: 将它们加到 CorePermissionProvider / CoreLegacyPermissionProvider / EducationPermissionProvider / EducationLegacyPermissionProvider 中.",
                missing.size(), String.join("%n  ", missing)));
    }

    // =============================================================
    // Test 2: TriggerPoint 覆盖度 — triggerService.fire(X, ...) 里的 X 必须有 TriggerPointDef 声明
    // =============================================================
    @Test
    @DisplayName("triggerService.fire(CONST, ...) 使用的每个常量都必须有 MessagingDomainPlugin.triggerPoints() 声明")
    void every_trigger_fire_must_be_declared_as_trigger_point() {
        // 匹配: triggerService.fire(SYMBOL, ...)  (SYMBOL 是常量名,NoMagicTriggerStringTest 已禁字面量)
        Pattern firePattern = Pattern.compile(
            "triggerService\\.fire\\s*\\(\\s*([A-Z_][A-Z0-9_]*)\\s*,");
        Set<String> fired = new TreeSet<>();
        for (String src : allJavaSource) {
            Matcher m = firePattern.matcher(src);
            while (m.find()) fired.add(m.group(1));
        }

        // 匹配: new TriggerPointDef(SYMBOL, ...)
        Pattern declPattern = Pattern.compile(
            "new\\s+TriggerPointDef\\s*\\(\\s*([A-Z_][A-Z0-9_]*)\\s*,");
        Set<String> declared = new TreeSet<>();
        for (String src : allJavaSource) {
            Matcher m = declPattern.matcher(src);
            while (m.find()) declared.add(m.group(1));
        }

        Set<String> missing = new TreeSet<>(fired);
        missing.removeAll(declared);
        assertTrue(missing.isEmpty(),
            String.format("%n以下 %d 个触发点常量被 triggerService.fire(...) 调用, 但没有任何 MessagingDomainPlugin 声明:%n  %s%n" +
                "后果: triggerService.fire() 会静默 no-op, 消息永远发不出去.%n" +
                "修复: 在对应 MessagingPlugin 的 triggerPoints() 返回列表里加 new TriggerPointDef(XXX, ...).",
                missing.size(), String.join("%n  ", missing)));
    }

    // =============================================================
    // Test 3: Role Code 覆盖度 — hasRole('X') / "X".equals(roleCode) 必须有 RolePresetPlugin 声明
    // =============================================================
    @Test
    @DisplayName("@PreAuthorize(\"hasRole('X')\") 中的 role code 必须有 RolePresetPlugin 声明")
    void every_has_role_must_be_declared_by_role_preset_plugin() {
        // 匹配: hasRole('XXX') 或 hasRole("XXX")
        Pattern rolePattern = Pattern.compile(
            "hasRole\\s*\\(\\s*['\"]([A-Z_][A-Z0-9_]*)['\"]");
        Set<String> referenced = new TreeSet<>();
        for (String src : allJavaSource) {
            Matcher m = rolePattern.matcher(src);
            while (m.find()) referenced.add(m.group(1));
        }

        // 匹配: RolePresetDef.of("XXX", ...)
        Pattern declPattern = Pattern.compile(
            "RolePresetDef\\.of\\s*\\(\\s*\"([A-Z_][A-Z0-9_]*)\"");
        Set<String> declared = new TreeSet<>();
        for (String src : allJavaSource) {
            if (!src.contains("implements RolePresetPlugin")) continue;
            Matcher m = declPattern.matcher(src);
            while (m.find()) declared.add(m.group(1));
        }

        Set<String> missing = new TreeSet<>(referenced);
        missing.removeAll(declared);
        assertTrue(missing.isEmpty(),
            String.format("%n以下 %d 个 role code 被 @PreAuthorize hasRole(...) 引用, 但无 RolePresetPlugin 声明:%n  %s%n" +
                "修复: 加到 CoreRolePresetPlugin / EducationRolePresetPlugin 的 getPresets() 里.",
                missing.size(), String.join("%n  ", missing)));
    }

    // =============================================================
    // Test 4: EntityTypeCode / RelationCode 反模式检查 — memory 约定:
    //   不允许 "STUDENT".equals(user.getType()) 这种字符串匹配, 应该用 hasFeature()
    //   这是已记录的 memory 反模式,此测试只是把它固化为构建期守卫.
    // =============================================================
    @Test
    @DisplayName("业务代码不得用 \"TYPE_CODE\".equals(user.getType/getTypeCode) 等字符串匹配实体类型 (用 hasFeature)")
    void no_string_match_on_entity_type_code() {
        // 精准匹配: "X".equals(user.getType()) 或 "X".equals(user.getTypeCode()) / .getUserType() / .typeCode()
        // 仅扫 application/ interfaces/ domain/ (排除 plugins/ 自己的声明 + config/test)
        Pattern antiPattern = Pattern.compile(
            "\"([A-Z_]+)\"\\s*\\.\\s*equals(?:IgnoreCase)?\\s*\\(\\s*[^)]*\\.(?:getType|getTypeCode|getUserType|typeCode)\\s*\\(\\s*\\)\\s*\\)");
        List<String> hits = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(SOURCE_ROOT)) {
            stream.filter(p -> p.toString().endsWith(".java"))
                .filter(p -> {
                    String s = p.toString().replace('\\','/');
                    return !s.contains("/plugins/") && !s.contains("/extension/plugins/")
                        && !s.contains("/extension/EntityTypePlugin.java"); // 接口自己的 javadoc 里提了反例
                })
                .forEach(p -> {
                    try {
                        String src = Files.readString(p);
                        // 去掉所有 javadoc/行注释, 只扫活代码
                        String code = src.replaceAll("(?s)/\\*.*?\\*/", "")
                                          .replaceAll("(?m)//.*$", "");
                        Matcher m = antiPattern.matcher(code);
                        while (m.find()) hits.add(p.getFileName() + ": \"" + m.group(1) + "\".equals(...getType())");
                    } catch (Exception ignored) {}
                });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        assertTrue(hits.isEmpty(),
            String.format("%n以下 %d 处代码用字符串匹配了实体类型码 — 违反 memory 约定 (应该用 user.hasFeature(\"isLearner\") 等):%n  %s",
                hits.size(), String.join("%n  ", hits)));
    }
}
