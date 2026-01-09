package com.school.management.domain.access.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Permission 领域模型测试")
class PermissionTest {

    @Nested
    @DisplayName("创建权限")
    class CreatePermissionTests {

        @Test
        @DisplayName("应成功创建操作权限")
        void shouldCreateOperationPermission() {
            // When
            Permission permission = Permission.create("user", "create", "创建用户", "允许创建新用户");

            // Then
            assertThat(permission.getPermissionCode()).isEqualTo("user:create");
            assertThat(permission.getPermissionName()).isEqualTo("创建用户");
            assertThat(permission.getResource()).isEqualTo("user");
            assertThat(permission.getAction()).isEqualTo("create");
            assertThat(permission.getType()).isEqualTo(PermissionType.OPERATION);
            assertThat(permission.getIsEnabled()).isTrue();
        }

        @Test
        @DisplayName("应成功使用 Builder 创建权限")
        void shouldCreateWithBuilder() {
            // When
            Permission permission = Permission.builder()
                    .permissionCode("inspection:view")
                    .permissionName("查看检查")
                    .description("查看检查记录")
                    .resource("inspection")
                    .action("view")
                    .type(PermissionType.OPERATION)
                    .sortOrder(10)
                    .build();

            // Then
            assertThat(permission.getPermissionCode()).isEqualTo("inspection:view");
            assertThat(permission.getSortOrder()).isEqualTo(10);
        }

        @Test
        @DisplayName("权限编码不能为空")
        void shouldRejectNullPermissionCode() {
            assertThatThrownBy(() -> Permission.builder()
                    .permissionCode(null)
                    .permissionName("测试")
                    .build())
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Permission code");
        }

        @Test
        @DisplayName("权限名称不能为空")
        void shouldRejectNullPermissionName() {
            assertThatThrownBy(() -> Permission.builder()
                    .permissionCode("test:code")
                    .permissionName(null)
                    .build())
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Permission name");
        }
    }

    @Nested
    @DisplayName("权限类型")
    class PermissionTypeTests {

        @Test
        @DisplayName("默认类型为 OPERATION")
        void shouldDefaultToOperation() {
            Permission permission = Permission.builder()
                    .permissionCode("test:action")
                    .permissionName("测试操作")
                    .build();

            assertThat(permission.getType()).isEqualTo(PermissionType.OPERATION);
        }

        @Test
        @DisplayName("应支持菜单类型")
        void shouldSupportMenuType() {
            Permission permission = Permission.builder()
                    .permissionCode("menu:dashboard")
                    .permissionName("仪表盘")
                    .type(PermissionType.MENU)
                    .path("/dashboard")
                    .component("Dashboard")
                    .icon("dashboard")
                    .build();

            assertThat(permission.getType()).isEqualTo(PermissionType.MENU);
            assertThat(permission.getPath()).isEqualTo("/dashboard");
        }
    }

    @Nested
    @DisplayName("默认值")
    class DefaultValueTests {

        @Test
        @DisplayName("默认启用状态为 true")
        void shouldBeEnabledByDefault() {
            Permission permission = Permission.builder()
                    .permissionCode("test:action")
                    .permissionName("测试")
                    .build();

            assertThat(permission.getIsEnabled()).isTrue();
        }

        @Test
        @DisplayName("默认排序为 0")
        void shouldHaveDefaultSortOrder() {
            Permission permission = Permission.builder()
                    .permissionCode("test:action")
                    .permissionName("测试")
                    .build();

            assertThat(permission.getSortOrder()).isEqualTo(0);
        }
    }
}
