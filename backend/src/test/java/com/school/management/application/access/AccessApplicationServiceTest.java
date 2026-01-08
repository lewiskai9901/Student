package com.school.management.application.access;

import com.school.management.domain.access.model.*;
import com.school.management.domain.access.repository.PermissionRepository;
import com.school.management.domain.access.repository.RoleRepository;
import com.school.management.domain.access.repository.UserRoleRepository;
import com.school.management.domain.access.service.AuthorizationService;
import com.school.management.domain.shared.event.DomainEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AccessApplicationService unit tests.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AccessApplicationService tests")
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

    private AccessApplicationService applicationService;

    private static final Long CREATOR_ID = 1L;

    @BeforeEach
    void setUp() {
        applicationService = new AccessApplicationService(
                permissionRepository,
                roleRepository,
                userRoleRepository,
                authorizationService,
                eventPublisher
        );
    }

    @Nested
    @DisplayName("Permission tests")
    class PermissionTests {

        @Test
        @DisplayName("Create permission successfully")
        void shouldCreatePermission() {
            CreatePermissionCommand command = CreatePermissionCommand.builder()
                    .permissionCode("user:create")
                    .permissionName("Create User")
                    .description("Permission to create users")
                    .resource("user")
                    .action("create")
                    .type(PermissionType.API)
                    .build();

            Permission savedPermission = Permission.builder()
                    .id(100L)
                    .permissionCode("user:create")
                    .permissionName("Create User")
                    .description("Permission to create users")
                    .resource("user")
                    .action("create")
                    .type(PermissionType.API)
                    .build();

            when(permissionRepository.existsByPermissionCode("user:create")).thenReturn(false);
            when(permissionRepository.save(any(Permission.class))).thenReturn(savedPermission);

            Permission result = applicationService.createPermission(command);

            assertNotNull(result);
            assertEquals("user:create", result.getPermissionCode());
            assertEquals("Create User", result.getPermissionName());

            verify(permissionRepository).existsByPermissionCode("user:create");
            verify(permissionRepository).save(any(Permission.class));
        }

        @Test
        @DisplayName("Throw exception when creating duplicate permission code")
        void shouldThrowExceptionForDuplicatePermissionCode() {
            CreatePermissionCommand command = CreatePermissionCommand.builder()
                    .permissionCode("user:create")
                    .permissionName("Create User")
                    .build();

            when(permissionRepository.existsByPermissionCode("user:create")).thenReturn(true);

            assertThrows(IllegalArgumentException.class, () ->
                    applicationService.createPermission(command)
            );

            verify(permissionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Get permission by ID")
        void shouldGetPermissionById() {
            Permission permission = Permission.builder()
                    .id(100L)
                    .permissionCode("user:view")
                    .permissionName("View User")
                    .build();

            when(permissionRepository.findById(100L)).thenReturn(Optional.of(permission));

            Optional<Permission> result = applicationService.getPermission(100L);

            assertTrue(result.isPresent());
            assertEquals("user:view", result.get().getPermissionCode());
        }

        @Test
        @DisplayName("List all permissions")
        void shouldListAllPermissions() {
            List<Permission> permissions = Arrays.asList(
                    Permission.builder().id(1L).permissionCode("user:view").permissionName("View User").build(),
                    Permission.builder().id(2L).permissionCode("user:create").permissionName("Create User").build()
            );

            when(permissionRepository.findAllEnabled()).thenReturn(permissions);

            List<Permission> result = applicationService.listAllPermissions();

            assertEquals(2, result.size());
        }
    }

    @Nested
    @DisplayName("Role tests")
    class RoleTests {

        @Test
        @DisplayName("Create role successfully")
        void shouldCreateRole() {
            CreateRoleCommand command = CreateRoleCommand.builder()
                    .roleCode("ROLE_MANAGER")
                    .roleName("Manager")
                    .description("Manager role")
                    .roleType(RoleType.CUSTOM)
                    .createdBy(CREATOR_ID)
                    .build();

            Role savedRole = Role.builder()
                    .id(100L)
                    .roleCode("ROLE_MANAGER")
                    .roleName("Manager")
                    .description("Manager role")
                    .roleType(RoleType.CUSTOM)
                    .build();

            when(roleRepository.existsByRoleCode("ROLE_MANAGER")).thenReturn(false);
            when(roleRepository.save(any(Role.class))).thenReturn(savedRole);

            Role result = applicationService.createRole(command);

            assertNotNull(result);
            assertEquals("ROLE_MANAGER", result.getRoleCode());

            verify(roleRepository).existsByRoleCode("ROLE_MANAGER");
            verify(roleRepository).save(any(Role.class));
        }

        @Test
        @DisplayName("Throw exception when creating duplicate role code")
        void shouldThrowExceptionForDuplicateRoleCode() {
            CreateRoleCommand command = CreateRoleCommand.builder()
                    .roleCode("ROLE_MANAGER")
                    .roleName("Manager")
                    .roleType(RoleType.CUSTOM)
                    .createdBy(CREATOR_ID)
                    .build();

            when(roleRepository.existsByRoleCode("ROLE_MANAGER")).thenReturn(true);

            assertThrows(IllegalArgumentException.class, () ->
                    applicationService.createRole(command)
            );

            verify(roleRepository, never()).save(any());
        }

        @Test
        @DisplayName("Update role successfully")
        void shouldUpdateRole() {
            Role existingRole = Role.builder()
                    .id(100L)
                    .roleCode("ROLE_MANAGER")
                    .roleName("Manager")
                    .roleType(RoleType.CUSTOM)
                    .build();

            UpdateRoleCommand command = UpdateRoleCommand.builder()
                    .roleName("Senior Manager")
                    .description("Updated description")
                    .dataScope(DataScope.DEPARTMENT)
                    .build();

            when(roleRepository.findById(100L)).thenReturn(Optional.of(existingRole));
            when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

            Role result = applicationService.updateRole(100L, command);

            assertNotNull(result);
            assertEquals("Senior Manager", result.getRoleName());
            assertEquals(DataScope.DEPARTMENT, result.getDataScope());
        }

        @Test
        @DisplayName("Cannot delete system role")
        void shouldNotDeleteSystemRole() {
            Role systemRole = Role.builder()
                    .id(100L)
                    .roleCode("ROLE_ADMIN")
                    .roleName("Admin")
                    .isSystem(true)
                    .build();

            when(roleRepository.findById(100L)).thenReturn(Optional.of(systemRole));

            assertThrows(IllegalStateException.class, () ->
                    applicationService.deleteRole(100L)
            );

            verify(roleRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Grant permissions to role")
        void shouldGrantPermissionsToRole() {
            Role role = Role.builder()
                    .id(100L)
                    .roleCode("ROLE_MANAGER")
                    .roleName("Manager")
                    .isSystem(false)
                    .build();

            Set<Long> permissionIds = new HashSet<>(Arrays.asList(1L, 2L, 3L));

            when(roleRepository.findById(100L)).thenReturn(Optional.of(role));
            when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRoleRepository.findByRoleId(100L)).thenReturn(Collections.emptyList());

            Role result = applicationService.grantPermissions(100L, permissionIds);

            assertNotNull(result);
            assertEquals(3, result.getPermissionIds().size());

            verify(roleRepository).save(any(Role.class));
        }
    }

    @Nested
    @DisplayName("User Role tests")
    class UserRoleTests {

        @Test
        @DisplayName("Assign role to user")
        void shouldAssignRoleToUser() {
            Long userId = 10L;
            Long roleId = 100L;
            Long assignedBy = CREATOR_ID;

            UserRole savedUserRole = UserRole.assign(userId, roleId, assignedBy);

            when(userRoleRepository.existsByUserIdAndRoleId(userId, roleId)).thenReturn(false);
            when(userRoleRepository.save(any(UserRole.class))).thenReturn(savedUserRole);

            UserRole result = applicationService.assignRoleToUser(userId, roleId, assignedBy);

            assertNotNull(result);
            assertEquals(userId, result.getUserId());
            assertEquals(roleId, result.getRoleId());

            verify(eventPublisher).publish(any());
            verify(authorizationService).refreshCache(userId);
        }

        @Test
        @DisplayName("Throw exception when user already has role")
        void shouldThrowExceptionWhenUserAlreadyHasRole() {
            Long userId = 10L;
            Long roleId = 100L;

            when(userRoleRepository.existsByUserIdAndRoleId(userId, roleId)).thenReturn(true);

            assertThrows(IllegalArgumentException.class, () ->
                    applicationService.assignRoleToUser(userId, roleId, CREATOR_ID)
            );

            verify(userRoleRepository, never()).save(any());
        }

        @Test
        @DisplayName("Remove role from user")
        void shouldRemoveRoleFromUser() {
            Long userId = 10L;
            Long roleId = 100L;

            doNothing().when(userRoleRepository).deleteByUserIdAndRoleId(userId, roleId);
            doNothing().when(authorizationService).refreshCache(userId);

            applicationService.removeRoleFromUser(userId, roleId);

            verify(userRoleRepository).deleteByUserIdAndRoleId(userId, roleId);
            verify(authorizationService).refreshCache(userId);
        }

        @Test
        @DisplayName("Get user roles")
        void shouldGetUserRoles() {
            Long userId = 10L;
            List<UserRole> userRoles = Arrays.asList(
                    UserRole.assign(userId, 100L, CREATOR_ID),
                    UserRole.assign(userId, 101L, CREATOR_ID)
            );
            List<Role> roles = Arrays.asList(
                    Role.builder().id(100L).roleCode("ROLE_A").roleName("Role A").build(),
                    Role.builder().id(101L).roleCode("ROLE_B").roleName("Role B").build()
            );

            when(userRoleRepository.findActiveByUserId(userId)).thenReturn(userRoles);
            when(roleRepository.findByIds(any())).thenReturn(roles);

            List<Role> result = applicationService.getUserRoles(userId);

            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("Get user permissions")
        void shouldGetUserPermissions() {
            Long userId = 10L;
            Set<String> permissions = new HashSet<>(Arrays.asList("user:view", "user:create"));

            when(authorizationService.getPermissions(userId)).thenReturn(permissions);

            Set<String> result = applicationService.getUserPermissions(userId);

            assertEquals(2, result.size());
            assertTrue(result.contains("user:view"));
            assertTrue(result.contains("user:create"));
        }
    }
}
