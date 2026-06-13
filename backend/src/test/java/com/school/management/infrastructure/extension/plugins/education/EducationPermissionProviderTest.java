package com.school.management.infrastructure.extension.plugins.education;

import com.school.management.infrastructure.extension.PermissionDef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EducationPermissionProvider 单元测试 (双轨收敛: 纯 holder static API)")
class EducationPermissionProviderTest {

    @Test
    @DisplayName("模块码与模块名固定为 education / 教育行业")
    void moduleMetadata() {
        assertThat(EducationPermissionProvider.MODULE_CODE).isEqualTo("education");
        assertThat(EducationPermissionProvider.MODULE_NAME).isEqualTo("教育行业");
    }

    @Test
    @DisplayName("权限列表非空且每条 code/name 均非空白")
    void permissionsNonBlank() {
        List<PermissionDef> perms = EducationPermissionProvider.permissions();

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
        List<PermissionDef> perms = EducationPermissionProvider.permissions();
        Set<String> codes = perms.stream()
                .map(PermissionDef::code)
                .collect(Collectors.toCollection(HashSet::new));

        assertThat(codes)
                .as("权限码集合大小应等于列表长度, 否则存在重复 of(...) 声明")
                .hasSize(perms.size());
    }

    @Test
    @DisplayName("包含已知教育行业权限码")
    void containsKnownCodes() {
        Set<String> codes = EducationPermissionProvider.permissions().stream()
                .map(PermissionDef::code)
                .collect(Collectors.toSet());

        assertThat(codes).contains("student:info:view", "academic:major:view",
                "teaching:schedule:view", "calendar:view");
    }
}
