package com.school.management.domain.access.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 角色聚合根单元测试
 * 测试角色的创建、权限管理、启用/禁用、级别比较等功能
 */
@DisplayName("角色聚合根测试")
class RoleTest {

    private static final Long CREATOR_ID = 1L;
    private static final Long PERMISSION_1 = 100L;
    private static final Long PERMISSION_2 = 101L;
    private static final Long PERMISSION_3 = 102L;

    private Role role;

    @BeforeEach
    void setUp() {
        role = createValidRole();
    }

    private Role createValidRole() {
        return Role.create(
            "ROLE_MANAGER",
            "部门经理",
            "负责部门日常管理",
            "CUSTOM",
            CREATOR_ID
        );
    }

    @Nested
    @DisplayName("创建角色测试")
    class CreateRoleTest {

        @Test
        @DisplayName("成功创建角色")
        void shouldCreateRoleSuccessfully() {
            assertNotNull(role);
            assertEquals("ROLE_MANAGER", role.getRoleCode());
            assertEquals("部门经理", role.getRoleName());
            assertEquals("负责部门日常管理", role.getDescription());
            assertEquals("CUSTOM", role.getRoleType());
            assertTrue(role.getIsEnabled());
            assertFalse(role.getIsSystem());
            assertEquals(100, role.getLevel());
            assertEquals(CREATOR_ID, role.getCreatedBy());
            assertNotNull(role.getCreatedAt());
            assertTrue(role.getPermissionIds().isEmpty());
            assertFalse(role.getDomainEvents().isEmpty());
        }

        @Test
        @DisplayName("创建系统角色")
        void shouldCreateSystemRole() {
            Role systemRole = Role.builder()
                .roleCode("ROLE_ADMIN")
                .roleName("系统管理员")
                .roleType("SYSTEM_ADMIN")
                .isSystem(true)
                .level(1)
                .createdBy(CREATOR_ID)
                .build();

            assertTrue(systemRole.getIsSystem());
            assertEquals("SYSTEM_ADMIN", systemRole.getRoleType());
            assertEquals(1, systemRole.getLevel());
        }

        @Test
        @DisplayName("创建角色时缺少编码应抛出异常")
        void shouldFailWhenRoleCodeIsNull() {
            assertThrows(NullPointerException.class, () ->
                Role.create(null, "部门经理", "描述", "CUSTOM", CREATOR_ID)
            );
        }

        @Test
        @DisplayName("创建角色时缺少名称应抛出异常")
        void shouldFailWhenRoleNameIsNull() {
            assertThrows(NullPointerException.class, () ->
                Role.create("ROLE_MANAGER", null, "描述", "CUSTOM", CREATOR_ID)
            );
        }

        @Test
        @DisplayName("创建角色时编码为空字符串应抛出异常")
        void shouldFailWhenRoleCodeIsBlank() {
            assertThrows(IllegalArgumentException.class, () ->
                Role.builder()
                    .roleCode("  ")
                    .roleName("部门经理")
                    .build()
            );
        }

        @Test
        @DisplayName("创建角色时编码过长应抛出异常")
        void shouldFailWhenRoleCodeTooLong() {
            String longCode = "A".repeat(51);
            assertThrows(IllegalArgumentException.class, () ->
                Role.builder()
                    .roleCode(longCode)
                    .roleName("部门经理")
                    .build()
            );
        }
    }

    @Nested
    @DisplayName("权限管理测试")
    class PermissionManagementTest {

        @Test
        @DisplayName("授予权限")
        void shouldGrantPermission() {
            role.clearDomainEvents();

            role.grantPermission(PERMISSION_1);

            assertTrue(role.hasPermission(PERMISSION_1));
            assertEquals(1, role.getPermissionIds().size());
            assertFalse(role.getDomainEvents().isEmpty());
        }

        @Test
        @DisplayName("授予多个权限")
        void shouldGrantMultiplePermissions() {
            role.grantPermission(PERMISSION_1);
            role.grantPermission(PERMISSION_2);
            role.grantPermission(PERMISSION_3);

            assertEquals(3, role.getPermissionIds().size());
            assertTrue(role.hasPermission(PERMISSION_1));
            assertTrue(role.hasPermission(PERMISSION_2));
            assertTrue(role.hasPermission(PERMISSION_3));
        }

        @Test
        @DisplayName("重复授予权限不会增加")
        void shouldNotDuplicatePermission() {
            role.grantPermission(PERMISSION_1);
            role.grantPermission(PERMISSION_1);

            assertEquals(1, role.getPermissionIds().size());
        }

        @Test
        @DisplayName("撤销权限")
        void shouldRevokePermission() {
            role.grantPermission(PERMISSION_1);
            role.grantPermission(PERMISSION_2);
            role.clearDomainEvents();

            role.revokePermission(PERMISSION_1);

            assertFalse(role.hasPermission(PERMISSION_1));
            assertTrue(role.hasPermission(PERMISSION_2));
            assertEquals(1, role.getPermissionIds().size());
            assertFalse(role.getDomainEvents().isEmpty());
        }

        @Test
        @DisplayName("批量设置权限")
        void shouldSetPermissions() {
            role.grantPermission(PERMISSION_1);
            Set<Long> newPermissions = new HashSet<>();
            newPermissions.add(PERMISSION_2);
            newPermissions.add(PERMISSION_3);
            role.clearDomainEvents();

            role.setPermissions(newPermissions);

            assertEquals(2, role.getPermissionIds().size());
            assertFalse(role.hasPermission(PERMISSION_1));
            assertTrue(role.hasPermission(PERMISSION_2));
            assertTrue(role.hasPermission(PERMISSION_3));
            assertFalse(role.getDomainEvents().isEmpty());
        }

        @Test
        @DisplayName("系统角色不能修改权限 - 授予")
        void shouldFailToGrantPermissionToSystemRole() {
            Role systemRole = Role.builder()
                .roleCode("ROLE_ADMIN")
                .roleName("系统管理员")
                .isSystem(true)
                .build();

            assertThrows(IllegalStateException.class, () ->
                systemRole.grantPermission(PERMISSION_1)
            );
        }

        @Test
        @DisplayName("系统角色不能修改权限 - 撤销")
        void shouldFailToRevokePermissionFromSystemRole() {
            Set<Long> initialPermissions = new HashSet<>();
            initialPermissions.add(PERMISSION_1);

            Role systemRole = Role.builder()
                .roleCode("ROLE_ADMIN")
                .roleName("系统管理员")
                .isSystem(true)
                .permissionIds(initialPermissions)
                .build();

            assertThrows(IllegalStateException.class, () ->
                systemRole.revokePermission(PERMISSION_1)
            );
        }

        @Test
        @DisplayName("系统角色不能修改权限 - 批量设置")
        void shouldFailToSetPermissionsForSystemRole() {
            Role systemRole = Role.builder()
                .roleCode("ROLE_ADMIN")
                .roleName("系统管理员")
                .isSystem(true)
                .build();

            assertThrows(IllegalStateException.class, () ->
                systemRole.setPermissions(new HashSet<>())
            );
        }
    }

    @Nested
    @DisplayName("启用/禁用测试")
    class EnableDisableTest {

        @Test
        @DisplayName("禁用角色")
        void shouldDisableRole() {
            role.disable();

            assertFalse(role.getIsEnabled());
        }

        @Test
        @DisplayName("启用角色")
        void shouldEnableRole() {
            role.disable();
            role.enable();

            assertTrue(role.getIsEnabled());
        }

        @Test
        @DisplayName("系统角色不能禁用")
        void shouldFailToDisableSystemRole() {
            Role systemRole = Role.builder()
                .roleCode("ROLE_ADMIN")
                .roleName("系统管理员")
                .isSystem(true)
                .build();

            assertThrows(IllegalStateException.class, systemRole::disable);
        }
    }

    @Nested
    @DisplayName("角色级别比较测试")
    class RoleLevelComparisonTest {

        @Test
        @DisplayName("高级别角色优于低级别角色")
        void shouldOutrankLowerLevelRole() {
            Role adminRole = Role.builder()
                .roleCode("ROLE_ADMIN")
                .roleName("管理员")
                .level(10)
                .build();

            Role userRole = Role.builder()
                .roleCode("ROLE_USER")
                .roleName("普通用户")
                .level(100)
                .build();

            assertTrue(adminRole.outranks(userRole));
            assertFalse(userRole.outranks(adminRole));
        }

        @Test
        @DisplayName("同级别角色不互相优于")
        void shouldNotOutrankSameLevelRole() {
            Role role1 = Role.builder()
                .roleCode("ROLE_1")
                .roleName("角色1")
                .level(50)
                .build();

            Role role2 = Role.builder()
                .roleCode("ROLE_2")
                .roleName("角色2")
                .level(50)
                .build();

            assertFalse(role1.outranks(role2));
            assertFalse(role2.outranks(role1));
        }
    }

    @Nested
    @DisplayName("更新角色信息测试")
    class UpdateRoleInfoTest {

        @Test
        @DisplayName("更新角色信息")
        void shouldUpdateRoleInfo() {
            role.updateInfo("高级部门经理", "负责多个部门的管理");

            assertEquals("高级部门经理", role.getRoleName());
            assertEquals("负责多个部门的管理", role.getDescription());
        }

        @Test
        @DisplayName("更新时空名称保持原值")
        void shouldKeepOriginalNameWhenUpdateWithBlank() {
            role.updateInfo("", "新描述");

            assertEquals("部门经理", role.getRoleName());
            assertEquals("新描述", role.getDescription());
        }

        @Test
        @DisplayName("更新时null名称保持原值")
        void shouldKeepOriginalNameWhenUpdateWithNull() {
            role.updateInfo(null, "新描述");

            assertEquals("部门经理", role.getRoleName());
        }
    }

    @Nested
    @DisplayName("Builder测试")
    class BuilderTest {

        @Test
        @DisplayName("使用Builder构建完整对象")
        void shouldBuildRoleWithAllFields() {
            Set<Long> permissions = new HashSet<>();
            permissions.add(PERMISSION_1);
            permissions.add(PERMISSION_2);

            Role built = Role.builder()
                .id(1L)
                .roleCode("ROLE_CUSTOM")
                .roleName("自定义角色")
                .description("测试角色")
                .roleType("CUSTOM")
                .level(50)
                .isSystem(false)
                .isEnabled(true)
                .createdBy(CREATOR_ID)
                .permissionIds(permissions)
                .build();

            assertEquals(1L, built.getId());
            assertEquals("ROLE_CUSTOM", built.getRoleCode());
            assertEquals(50, built.getLevel());
            assertEquals(2, built.getPermissionIds().size());
        }

        @Test
        @DisplayName("默认值验证")
        void shouldHaveDefaultValues() {
            Role minimal = Role.builder()
                .roleCode("ROLE_MIN")
                .roleName("最小角色")
                .build();

            assertEquals("CUSTOM", minimal.getRoleType());
            assertEquals(100, minimal.getLevel());
            assertFalse(minimal.getIsSystem());
            assertTrue(minimal.getIsEnabled());
            assertTrue(minimal.getPermissionIds().isEmpty());
        }
    }
}
