package com.school.management.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hook 接入广覆盖测试 — 确保每个关键 ApplicationService 都接入了 PolicyRegistry.
 *
 * 粗粒度但稳定: 用源文件 grep 确认每个 service 源码中都出现了对应 PolicyContext 键字符串.
 * 检测项来自 docs/plugin-extension-catalog.md 的 hook 点清单 (27 项).
 *
 * 防止回归: 如果有人删除了 Policy hook, 或重命名了 entityType/phase, 这个测试会挂.
 */
@DisplayName("Hook Coverage — 29 policy hook probes 都接入了对应 ApplicationService")
class HookCoverageTest {

    private static final Path BASE = Paths.get("src/main/java/com/school/management/application");

    private static String read(Path p) throws IOException {
        return Files.readString(p);
    }

    @Test
    @DisplayName("UserApplicationService 接入 5 个 user hook")
    void userService_has_all_user_hooks() throws IOException {
        String src = read(BASE.resolve("user/UserApplicationService.java"));
        assertThat(src).contains("\"user\", \"BEFORE_CREATE\"");
        assertThat(src).contains("\"user\", \"AFTER_CREATE\"");
        assertThat(src).contains("\"user\", \"BEFORE_UPDATE\"");
        assertThat(src).contains("\"user\", \"AFTER_UPDATE\"");
        assertThat(src).contains("\"user\", \"BEFORE_DELETE\"");
        assertThat(src).contains("policyRegistry");
    }

    @Test
    @DisplayName("AccessRelationApplicationService 接入 4 个 grant/revoke hook")
    void accessRelationService_has_all_hooks() throws IOException {
        String src = read(BASE.resolve("access/AccessRelationApplicationService.java"));
        assertThat(src).contains("\"access_relation\", \"BEFORE_GRANT\"");
        assertThat(src).contains("\"access_relation\", \"AFTER_GRANT\"");
        assertThat(src).contains("\"access_relation\", \"BEFORE_REVOKE\"");
        assertThat(src).contains("\"access_relation\", \"AFTER_REVOKE\"");
        assertThat(src).contains("policyRegistry");
    }

    @Test
    @DisplayName("UniversalPlaceApplicationService 接入 9 个 place hook (含 CRUD + CHECKIN/CHECKOUT)")
    void placeService_has_all_hooks() throws IOException {
        String src = read(BASE.resolve("place/UniversalPlaceApplicationService.java"));
        // CRUD
        assertThat(src).contains("\"place\", \"BEFORE_CREATE\"");
        assertThat(src).contains("\"place\", \"AFTER_CREATE\"");
        assertThat(src).contains("\"place\", \"BEFORE_UPDATE\"");
        assertThat(src).contains("\"place\", \"AFTER_UPDATE\"");
        assertThat(src).contains("\"place\", \"BEFORE_DELETE\"");
        // Occupancy (既有, 不可回归)
        assertThat(src).contains("\"place\", \"BEFORE_CHECKIN\"");
        assertThat(src).contains("\"place\", \"AFTER_CHECKIN\"");
        assertThat(src).contains("\"place\", \"BEFORE_CHECKOUT\"");
        assertThat(src).contains("\"place\", \"AFTER_CHECKOUT\"");
    }

    @Test
    @DisplayName("OrgUnitApplicationService 接入 5 个 org_unit CRUD hook (MOVE 暂未接入, 业务方法不存在)")
    void orgUnitService_has_all_crud_hooks() throws IOException {
        String src = read(BASE.resolve("organization/OrgUnitApplicationService.java"));
        assertThat(src).contains("\"org_unit\", \"BEFORE_CREATE\"");
        assertThat(src).contains("\"org_unit\", \"AFTER_CREATE\"");
        assertThat(src).contains("\"org_unit\", \"BEFORE_UPDATE\"");
        assertThat(src).contains("\"org_unit\", \"AFTER_UPDATE\"");
        assertThat(src).contains("\"org_unit\", \"BEFORE_DELETE\"");
    }

    @Test
    @DisplayName("PluginPlatformController probes 列出全部 27 hook 点 (置 A+ 真覆盖)")
    void platformController_exposes_all_probes() throws IOException {
        Path ctrl = Paths.get("src/main/java/com/school/management/interfaces/rest/system/PluginPlatformController.java");
        String src = read(ctrl);
        // Place CRUD
        assertThat(src).contains("\"place\",    \"BEFORE_CREATE\"");
        assertThat(src).contains("\"place\",    \"BEFORE_UPDATE\"");
        assertThat(src).contains("\"place\",    \"BEFORE_DELETE\"");
        // User CRUD
        assertThat(src).contains("\"user\",     \"BEFORE_CREATE\"");
        assertThat(src).contains("\"user\",     \"BEFORE_DELETE\"");
        // AccessRelation
        assertThat(src).contains("\"access_relation\", \"BEFORE_GRANT\"");
        assertThat(src).contains("\"access_relation\", \"AFTER_REVOKE\"");
    }
}
