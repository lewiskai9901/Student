package com.school.management.application.access;

import com.school.management.domain.access.model.*;
import com.school.management.domain.access.repository.PermissionRepository;
import com.school.management.domain.access.repository.RoleRepository;
import com.school.management.domain.access.repository.UserRoleRepository;
import com.school.management.domain.access.service.AuthorizationService;
import com.school.management.domain.shared.event.DomainEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccessApplicationService 测试")
class AccessApplicationServiceTest {

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private DomainEventPublisher eventPublisher;

    @InjectMocks
    private AccessApplicationService service;

    @Nested
    @DisplayName("权限操作")
    class PermissionOperationsTests {

        @Test
        @DisplayName("应成功创建权限")
        void shouldCreatePermissionSuccessfully() {
            CreatePermissionCommand command = CreatePermissionCommand.builder()
                    .permissionCode("user:view")
                    .permissionName("查看用户")
                    .description("允许查看用户列表")
                    .resource("user")
                    .action("view")
                    .type(PermissionType.MENU)
                    .build();

            when(permissionRepository.existsByPermissionCode("user:view")).thenReturn(false);
            when(permissionRepository.save(any(Permission.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Permission result = service.createPermission(command);

            assertThat(result).isNotNull();
            assertThat(result.getPermissionCode()).isEqualTo("user:view");
            verify(permissionRepository).save(any(Permission.class));
        }

        @Test
        @DisplayName("权限编码重复时应抛出异常")
        void shouldThrowExceptionWhenPermissionCodeExists() {
            CreatePermissionCommand command = CreatePermissionCommand.builder()
                    .permissionCode("user:view")
                    .permissionName("查看用户")
                    .type(PermissionType.MENU)
                    .build();

            when(permissionRepository.existsByPermissionCode("user:view")).thenReturn(true);

            assertThatThrownBy(() -> service.createPermission(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Permission code already exists");

            verify(permissionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("角色操作")
    class RoleOperationsTests {

        @Test
        @DisplayName("应成功创建角色")
        void shouldCreateRoleSuccessfully() {
            CreateRoleCommand command = CreateRoleCommand.builder()
                    .roleCode("ADMIN")
                    .roleName("管理员")
                    .description("系统管理员角色")
                    .build();

            when(roleRepository.existsByRoleCode("ADMIN")).thenReturn(false);
            when(roleRepository.save(any(Role.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Role result = service.createRole(command);

            assertThat(result).isNotNull();
            assertThat(result.getRoleCode()).isEqualTo("ADMIN");
            verify(roleRepository).save(any(Role.class));
        }

        @Test
        @DisplayName("角色编码重复时应抛出异常")
        void shouldThrowExceptionWhenRoleCodeExists() {
            CreateRoleCommand command = CreateRoleCommand.builder()
                    .roleCode("ADMIN")
                    .roleName("管理员")
                    .build();

            when(roleRepository.existsByRoleCode("ADMIN")).thenReturn(true);

            assertThatThrownBy(() -> service.createRole(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Role code already exists");
        }
    }

    @Nested
    @DisplayName("用户角色分配")
    class UserRoleAssignmentTests {

        @Test
        @DisplayName("应成功获取用户角色")
        void shouldGetUserRolesSuccessfully() {
            Long userId = 1L;
            Long roleId = 10L;

            UserRole userRole = UserRole.assign(userId, roleId, 1L);
            Role adminRole = Role.builder()
                    .roleCode("ADMIN")
                    .roleName("管理员")
                    .build();

            when(userRoleRepository.findActiveByUserId(userId)).thenReturn(List.of(userRole));
            when(roleRepository.findByIds(List.of(roleId))).thenReturn(List.of(adminRole));

            List<Role> result = service.getUserRoles(userId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRoleCode()).isEqualTo("ADMIN");
        }
    }
}
