package com.school.management.application.user;

import com.school.management.application.user.command.CreateUserCommand;
import com.school.management.application.user.command.UpdateUserCommand;
import com.school.management.domain.access.repository.AccessRelationRepository;
import com.school.management.domain.access.repository.RoleRepository;
import com.school.management.domain.place.repository.UniversalPlaceOccupantRepository;
import com.school.management.domain.place.repository.UniversalPlaceRepository;
import com.school.management.domain.shared.event.DomainEventPublisher;
import com.school.management.domain.user.model.aggregate.User;
import com.school.management.domain.user.model.valueobject.UserStatus;
import com.school.management.domain.user.repository.UserRepository;
import com.school.management.domain.user.repository.UserTypeRepository;
import com.school.management.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserApplicationService 测试")
class UserApplicationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserTypeRepository userTypeRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private AccessRelationRepository accessRelationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private DomainEventPublisher eventPublisher;

    @Mock
    private UniversalPlaceOccupantRepository occupantRepository;

    @Mock
    private UniversalPlaceRepository placeRepository;

    @InjectMocks
    private UserApplicationService service;

    @BeforeEach
    void setUp() {
        // 设置默认密码
        ReflectionTestUtils.setField(service, "defaultPassword", "Pwd@123456");
    }

    // ==================== 测试数据工厂方法 ====================

    private User createTestUser(Long id, String username, String realName) {
        return User.reconstruct(
                id,
                username,
                "encodedPassword",
                realName,
                "13800138000",
                "test@example.com",
                null, // avatar
                "EMP001",
                1, // male
                LocalDate.of(1990, 1, 1),
                "110101199001011234",
                null, // primaryOrgUnitId
                "TEACHER", // userTypeCode
                UserStatus.ENABLED,
                null, // lastLoginTime
                null, // lastLoginIp
                null, // passwordChangedAt
                null, // wechatOpenid
                false, // allowMultipleDevices
                Arrays.asList(1L, 2L), // roleIds
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private User createDisabledUser(Long id, String username) {
        return User.reconstruct(
                id,
                username,
                "encodedPassword",
                "禁用用户",
                null, null, null, null, null, null, null,
                null, // primaryOrgUnitId
                "TEACHER", // userTypeCode
                UserStatus.DISABLED,
                null, null, null, null, false,
                Collections.emptyList(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    // ==================== 创建用户测试 ====================

    @Nested
    @DisplayName("创建用户")
    class CreateUserTests {

        @Test
        @DisplayName("应成功创建用户")
        void shouldCreateUserSuccessfully() {
            // Given
            CreateUserCommand command = CreateUserCommand.builder()
                    .username("testuser")
                    .password("password123")
                    .realName("测试用户")
                    .phone("13800138001")
                    .email("testuser@example.com")
                    .orgUnitId(1L)
                    .userTypeCode("TEACHER")
                    .build();

            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                ReflectionTestUtils.setField(u, "id", 1L);
                return u;
            });

            // When
            User result = service.createUser(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("testuser");
            assertThat(result.getRealName()).isEqualTo("测试用户");
            verify(userRepository).save(any(User.class));
            verify(passwordEncoder).encode("password123");
        }

        @Test
        @DisplayName("使用默认密码创建用户")
        void shouldCreateUserWithDefaultPassword() {
            // Given
            CreateUserCommand command = CreateUserCommand.builder()
                    .username("testuser")
                    .password(null) // 使用默认密码
                    .realName("测试用户")
                    .orgUnitId(1L)
                    .build();

            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(passwordEncoder.encode("Pwd@123456")).thenReturn("encodedDefaultPassword");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                ReflectionTestUtils.setField(u, "id", 1L);
                return u;
            });

            // When
            User result = service.createUser(command);

            // Then
            assertThat(result).isNotNull();
            verify(passwordEncoder).encode("Pwd@123456");
        }

        @Test
        @DisplayName("创建带角色的用户")
        void shouldCreateUserWithRoles() {
            // Given
            CreateUserCommand command = CreateUserCommand.builder()
                    .username("testuser")
                    .password("password123")
                    .realName("测试用户")
                    .orgUnitId(1L)
                    .roleIds(Arrays.asList(1L, 2L))
                    .build();

            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                ReflectionTestUtils.setField(u, "id", 1L);
                return u;
            });

            // When
            User result = service.createUser(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getRoleIds()).containsExactly(1L, 2L);
        }

        @Test
        @DisplayName("用户名已存在时应抛出异常")
        void shouldThrowWhenUsernameExists() {
            // Given
            CreateUserCommand command = CreateUserCommand.builder()
                    .username("existinguser")
                    .password("password123")
                    .realName("测试用户")
                    .build();

            when(userRepository.existsByUsername("existinguser")).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> service.createUser(command))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户名已存在");

            verify(userRepository, never()).save(any());
        }
    }

    // ==================== 更新用户测试 ====================

    @Nested
    @DisplayName("更新用户")
    class UpdateUserTests {

        @Test
        @DisplayName("应成功更新用户信息")
        void shouldUpdateUserSuccessfully() {
            // Given
            User existingUser = createTestUser(1L, "testuser", "原姓名");
            UpdateUserCommand command = UpdateUserCommand.builder()
                    .realName("新姓名")
                    .phone("13900139001")
                    .email("newemail@example.com")
                    .orgUnitId(2L)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
            when(userRepository.save(any(User.class))).thenReturn(existingUser);

            // When
            User result = service.updateUser(1L, command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getRealName()).isEqualTo("新姓名");
            assertThat(result.getPhone()).isEqualTo("13900139001");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("更新用户角色")
        void shouldUpdateUserRoles() {
            // Given
            User existingUser = createTestUser(1L, "testuser", "测试用户");
            UpdateUserCommand command = UpdateUserCommand.builder()
                    .realName("测试用户")
                    .roleIds(Arrays.asList(3L, 4L))
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
            when(userRepository.save(any(User.class))).thenReturn(existingUser);

            // When
            User result = service.updateUser(1L, command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getRoleIds()).containsExactly(3L, 4L);
        }

        @Test
        @DisplayName("更新不存在的用户应抛出异常")
        void shouldThrowWhenUserNotFound() {
            // Given
            UpdateUserCommand command = UpdateUserCommand.builder()
                    .realName("新姓名")
                    .build();

            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> service.updateUser(999L, command))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户不存在");
        }
    }

    // ==================== 状态管理测试 ====================

    @Nested
    @DisplayName("状态管理")
    class StatusManagementTests {

        @Test
        @DisplayName("应成功启用用户")
        void shouldEnableUserSuccessfully() {
            // Given
            User disabledUser = createDisabledUser(1L, "testuser");
            when(userRepository.findById(1L)).thenReturn(Optional.of(disabledUser));
            when(userRepository.save(any(User.class))).thenReturn(disabledUser);

            // When
            User result = service.enableUser(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(UserStatus.ENABLED);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("应成功禁用用户")
        void shouldDisableUserSuccessfully() {
            // Given
            User enabledUser = createTestUser(1L, "testuser", "测试用户");
            when(userRepository.findById(1L)).thenReturn(Optional.of(enabledUser));
            when(userRepository.save(any(User.class))).thenReturn(enabledUser);

            // When
            User result = service.disableUser(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(UserStatus.DISABLED);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("启用不存在的用户应抛出异常")
        void shouldThrowWhenEnableNonExistentUser() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> service.enableUser(999L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户不存在");
        }
    }

    // ==================== 密码管理测试 ====================

    @Nested
    @DisplayName("密码管理")
    class PasswordManagementTests {

        @Test
        @DisplayName("应成功重置密码")
        void shouldResetPasswordSuccessfully() {
            // Given
            User user = createTestUser(1L, "testuser", "测试用户");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode("Pwd@123456")).thenReturn("newEncodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(user);

            // When
            String newPassword = service.resetPassword(1L);

            // Then
            assertThat(newPassword).isEqualTo("Pwd@123456");
            verify(userRepository).save(any(User.class));
            verify(passwordEncoder).encode("Pwd@123456");
        }

        @Test
        @DisplayName("应成功修改密码")
        void shouldChangePasswordSuccessfully() {
            // Given
            User user = createTestUser(1L, "testuser", "测试用户");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("oldPassword", "encodedPassword")).thenReturn(true);
            when(passwordEncoder.encode("newPassword")).thenReturn("newEncodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(user);

            // When
            service.changePassword(1L, "oldPassword", "newPassword");

            // Then
            verify(userRepository).save(any(User.class));
            verify(passwordEncoder).encode("newPassword");
        }

        @Test
        @DisplayName("原密码错误时应抛出异常")
        void shouldThrowWhenOldPasswordIncorrect() {
            // Given
            User user = createTestUser(1L, "testuser", "测试用户");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> service.changePassword(1L, "wrongPassword", "newPassword"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("原密码错误");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("重置不存在用户的密码应抛出异常")
        void shouldThrowWhenResetPasswordForNonExistentUser() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> service.resetPassword(999L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户不存在");
        }
    }

    // ==================== 删除操作测试 ====================

    @Nested
    @DisplayName("删除操作")
    class DeleteTests {

        @Test
        @DisplayName("应成功删除用户")
        void shouldDeleteUserSuccessfully() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(createTestUser(1L, "testuser", "测试用户")));

            // When
            service.deleteUser(1L);

            // Then
            verify(userRepository).deleteById(1L);
        }

        @Test
        @DisplayName("删除不存在的用户应抛出异常")
        void shouldThrowWhenDeleteNonExistentUser() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> service.deleteUser(999L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户不存在");

            verify(userRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("应成功批量删除用户")
        void shouldDeleteUsersInBatch() {
            // Given
            List<Long> userIds = Arrays.asList(1L, 2L, 3L);

            // When
            service.deleteUsers(userIds);

            // Then
            verify(userRepository).deleteByIds(userIds);
        }
    }

    // ==================== 查询操作测试 ====================

    @Nested
    @DisplayName("查询操作")
    class QueryTests {

        @Test
        @DisplayName("应根据ID获取用户")
        void shouldGetUserById() {
            // Given
            User user = createTestUser(1L, "testuser", "测试用户");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            // When
            Optional<User> result = service.getUser(1L);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("用户不存在时应返回空")
        void shouldReturnEmptyWhenUserNotFound() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When
            Optional<User> result = service.getUser(999L);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("应根据用户名获取用户")
        void shouldGetUserByUsername() {
            // Given
            User user = createTestUser(1L, "testuser", "测试用户");
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

            // When
            Optional<User> result = service.getUserByUsername("testuser");

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("应根据组织单元获取用户列表")
        void shouldGetUsersByOrgUnit() {
            // Given
            User user1 = createTestUser(1L, "user1", "用户1");
            User user2 = createTestUser(2L, "user2", "用户2");
            when(userRepository.findByOrgUnitId(1L)).thenReturn(Arrays.asList(user1, user2));

            // When
            List<User> result = service.getUsersByOrgUnit(1L);

            // Then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("应检查用户名是否存在")
        void shouldCheckUsernameExists() {
            // Given
            when(userRepository.existsByUsername("existinguser")).thenReturn(true);

            // When
            boolean result = service.existsUsername("existinguser", null);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("应检查用户名是否存在（排除指定ID）")
        void shouldCheckUsernameExistsExcludingId() {
            // Given
            when(userRepository.existsByUsernameAndIdNot("username", 1L)).thenReturn(false);

            // When
            boolean result = service.existsUsername("username", 1L);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("应获取简单用户列表")
        void shouldGetSimpleUserList() {
            // Given
            User user = createTestUser(1L, "testuser", "测试用户");
            when(userRepository.findSimpleList("测试")).thenReturn(Collections.singletonList(user));

            // When
            List<User> result = service.getSimpleUserList("测试");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRealName()).isEqualTo("测试用户");
        }

        @Test
        @DisplayName("应获取所有用户")
        void shouldGetAllUsers() {
            // Given
            User user1 = createTestUser(1L, "user1", "用户1");
            User user2 = createTestUser(2L, "user2", "用户2");
            when(userRepository.findAllUsers()).thenReturn(Arrays.asList(user1, user2));

            // When
            List<User> result = service.getAllUsers();

            // Then
            assertThat(result).hasSize(2);
        }
    }

    // ==================== 角色分配测试 ====================

    @Nested
    @DisplayName("角色分配")
    class RoleAssignmentTests {

        @Test
        @DisplayName("应成功分配角色")
        void shouldAssignRolesSuccessfully() {
            // Given
            User user = createTestUser(1L, "testuser", "测试用户");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);

            // When
            service.assignRoles(1L, Arrays.asList(3L, 4L, 5L));

            // Then
            assertThat(user.getRoleIds()).containsExactly(3L, 4L, 5L);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("应获取用户角色ID列表")
        void shouldGetUserRoleIds() {
            // Given
            User user = createTestUser(1L, "testuser", "测试用户");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            // When
            List<Long> result = service.getUserRoleIds(1L);

            // Then
            assertThat(result).containsExactly(1L, 2L);
        }

        @Test
        @DisplayName("分配角色给不存在的用户应抛出异常")
        void shouldThrowWhenAssignRolesToNonExistentUser() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> service.assignRoles(999L, Arrays.asList(1L, 2L)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户不存在");
        }
    }

    // ==================== 登录记录测试 ====================

    @Nested
    @DisplayName("登录记录")
    class LoginRecordTests {

        @Test
        @DisplayName("应成功记录登录信息")
        void shouldRecordLoginSuccessfully() {
            // Given
            User user = createTestUser(1L, "testuser", "测试用户");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);

            // When
            service.recordLogin(1L, "192.168.1.100");

            // Then
            assertThat(user.getLastLoginIp()).isEqualTo("192.168.1.100");
            assertThat(user.getLastLoginTime()).isNotNull();
            verify(userRepository).save(any(User.class));
        }
    }

    // ==================== 微信绑定测试 ====================

    @Nested
    @DisplayName("微信绑定")
    class WechatBindingTests {

        @Test
        @DisplayName("应成功绑定微信")
        void shouldBindWechatSuccessfully() {
            // Given
            User user = createTestUser(1L, "testuser", "测试用户");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);

            // When
            service.bindWechat(1L, "wechat_openid_123");

            // Then
            assertThat(user.getWechatOpenid()).isEqualTo("wechat_openid_123");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("应成功解绑微信")
        void shouldUnbindWechatSuccessfully() {
            // Given
            User user = User.reconstruct(
                    1L, "testuser", "password", "测试用户",
                    null, null, null, null, null, null, null,
                    null, // primaryOrgUnitId
                    "TEACHER", // userTypeCode
                    UserStatus.ENABLED,
                    null, null, null,
                    "wechat_openid_123", // 已绑定微信
                    false, Collections.emptyList(),
                    LocalDateTime.now(), LocalDateTime.now()
            );
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);

            // When
            service.unbindWechat(1L);

            // Then
            assertThat(user.getWechatOpenid()).isNull();
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("应根据微信OpenID获取用户")
        void shouldGetUserByWechatOpenid() {
            // Given
            User user = createTestUser(1L, "testuser", "测试用户");
            when(userRepository.findByWechatOpenid("wechat_openid_123")).thenReturn(Optional.of(user));

            // When
            Optional<User> result = service.getUserByWechatOpenid("wechat_openid_123");

            // Then
            assertThat(result).isPresent();
        }
    }

    // ==================== 分页查询测试 ====================

    @Nested
    @DisplayName("分页查询")
    class PaginationTests {

        @Test
        @DisplayName("应分页查询用户")
        void shouldGetUsersPage() {
            // Given
            User user1 = createTestUser(1L, "user1", "用户1");
            User user2 = createTestUser(2L, "user2", "用户2");
            when(userRepository.findPagedWithConditions(0, 10, null, null, null, null, null))
                    .thenReturn(Arrays.asList(user1, user2));

            // When
            List<User> result = service.getUsersPage(0, 10, null, null, null, null, null);

            // Then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("应条件分页查询用户")
        void shouldGetUsersPageWithConditions() {
            // Given
            User user = createTestUser(1L, "testuser", "测试用户");
            when(userRepository.findPagedWithConditions(0, 10, "test", "测试", null, 1L, 1))
                    .thenReturn(Collections.singletonList(user));

            // When
            List<User> result = service.getUsersPage(0, 10, "test", "测试", null, 1L, 1);

            // Then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("应统计用户总数")
        void shouldCountUsers() {
            // Given
            when(userRepository.countWithConditions(null, null, null, null, null)).thenReturn(100L);

            // When
            long result = service.countUsers(null, null, null, null, null);

            // Then
            assertThat(result).isEqualTo(100L);
        }

        @Test
        @DisplayName("应条件统计用户总数")
        void shouldCountUsersWithConditions() {
            // Given
            when(userRepository.countWithConditions("test", "测试", null, 1L, 1)).thenReturn(5L);

            // When
            long result = service.countUsers("test", "测试", null, 1L, 1);

            // Then
            assertThat(result).isEqualTo(5L);
        }
    }
}
