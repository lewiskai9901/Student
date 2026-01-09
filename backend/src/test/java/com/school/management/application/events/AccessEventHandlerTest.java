package com.school.management.application.events;

import com.school.management.domain.access.event.RoleCreatedEvent;
import com.school.management.domain.access.event.RolePermissionsChangedEvent;
import com.school.management.domain.access.event.UserRoleAssignedEvent;
import com.school.management.domain.access.model.Role;
import com.school.management.domain.access.model.RoleType;
import com.school.management.infrastructure.event.DomainEventStore;
import com.school.management.infrastructure.external.NotificationService;
import com.school.management.service.OperationLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.HashSet;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * AccessEventHandler 单元测试
 * 注意: CasbinEnforcerService 是可选依赖，这里不测试其集成
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AccessEventHandler 测试")
class AccessEventHandlerTest {

    @Mock
    private DomainEventStore eventStore;

    @Mock
    private NotificationService notificationService;

    @Mock
    private OperationLogService operationLogService;

    @InjectMocks
    private AccessEventHandler handler;

    @Nested
    @DisplayName("处理角色创建事件")
    class HandleRoleCreatedEventTests {

        private Role testRole;

        @BeforeEach
        void setUp() {
            testRole = Role.builder()
                    .id(1L)
                    .roleCode("ADMIN")
                    .roleName("管理员")
                    .roleType(RoleType.CUSTOM)
                    .build();
        }

        @Test
        @DisplayName("应存储事件并记录操作日志")
        void shouldStoreEventAndSaveLog() {
            // Given
            RoleCreatedEvent event = new RoleCreatedEvent(testRole);

            // When
            handler.handle(event);

            // Then
            verify(eventStore).store(event);
            verify(operationLogService).saveLog(any());
        }
    }

    @Nested
    @DisplayName("处理角色权限变更事件")
    class HandleRolePermissionsChangedEventTests {

        private Role testRole;

        @BeforeEach
        void setUp() {
            testRole = Role.builder()
                    .id(1L)
                    .roleCode("ADMIN")
                    .roleName("管理员")
                    .roleType(RoleType.CUSTOM)
                    .build();
        }

        @Test
        @DisplayName("应存储事件并记录操作日志")
        void shouldStoreEventAndSaveLog() {
            // Given
            Set<Long> oldPermissions = new HashSet<>(Set.of(5L, 6L));
            Set<Long> newPermissions = new HashSet<>(Set.of(10L, 11L));
            RolePermissionsChangedEvent event = new RolePermissionsChangedEvent(
                    testRole, oldPermissions, newPermissions
            );

            // When
            handler.handle(event);

            // Then
            verify(eventStore).store(event);
            verify(operationLogService).saveLog(any());
        }
    }

    @Nested
    @DisplayName("处理用户角色分配事件")
    class HandleUserRoleAssignedEventTests {

        @Test
        @DisplayName("应存储事件并发送通知")
        void shouldStoreEventAndSendNotification() {
            // Given
            UserRoleAssignedEvent event = new UserRoleAssignedEvent(
                    100L, 1L, null, 999L
            );

            // When
            handler.handle(event);

            // Then
            verify(eventStore).store(event);
            verify(notificationService).sendInAppMessage(
                    eq(100L),
                    anyString(),
                    anyString(),
                    eq(NotificationService.MessageType.SYSTEM_NOTICE)
            );
            verify(operationLogService).saveLog(any());
        }
    }
}
