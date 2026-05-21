package com.school.management.infrastructure.extension.plugins.core;

import com.school.management.infrastructure.extension.PermissionProvider.PermissionDef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CorePermissionProvider 单元测试")
class CorePermissionProviderTest {

    private final CorePermissionProvider provider = new CorePermissionProvider();

    @Test
    @DisplayName("模块码与模块名固定为 core / 通用核心")
    void moduleMetadata() {
        assertThat(provider.getModuleCode()).isEqualTo("core");
        assertThat(provider.getModuleName()).isEqualTo("通用核心");
    }

    @Test
    @DisplayName("权限列表非空且每条 code/name 均非空白")
    void permissionsNonBlank() {
        List<PermissionDef> perms = provider.getPermissions();

        assertThat(perms).isNotEmpty();
        assertThat(perms).allSatisfy(p -> {
            assertThat(p.code()).isNotBlank();
            assertThat(p.name()).isNotBlank();
            assertThat(p.resourceType()).isPositive();
        });
    }

    @Test
    @DisplayName("权限码无重复 (重复声明属真实 bug)")
    void noDuplicateCodes() {
        List<PermissionDef> perms = provider.getPermissions();
        Set<String> codes = perms.stream()
                .map(PermissionDef::code)
                .collect(Collectors.toCollection(HashSet::new));

        assertThat(codes)
                .as("权限码集合大小应等于列表长度, 否则存在重复 of(...) 声明")
                .hasSize(perms.size());
    }

    @Test
    @DisplayName("包含已知核心权限码")
    void containsKnownCodes() {
        Set<String> codes = provider.getPermissions().stream()
                .map(PermissionDef::code)
                .collect(Collectors.toSet());

        assertThat(codes).contains("system:admin", "system:user:view",
                "task:create", "workflow:deploy");
    }
}
