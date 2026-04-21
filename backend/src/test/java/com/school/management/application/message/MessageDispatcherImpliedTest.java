package com.school.management.application.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.application.access.AuthorizationService;
import com.school.management.domain.message.repository.MsgNotificationRepository;
import com.school.management.domain.message.repository.MsgSubscriptionRuleRepository;
import com.school.management.domain.message.repository.MsgTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * M1.2 — MessageDispatcher.resolveByRelation 走 AuthorizationService.findSubjectsWithRelation,
 * 不再裸 SQL — 确保 implied 派生关系的订阅者能收到.
 *
 * 注: resolveByRelation 是 private, 用反射调用测试以验证分发逻辑.
 */
class MessageDispatcherImpliedTest {

    private AuthorizationService authService;
    private MessageDispatcher dispatcher;

    @BeforeEach
    void setup() {
        authService = mock(AuthorizationService.class);
        dispatcher = new MessageDispatcher(
            mock(MsgSubscriptionRuleRepository.class),
            mock(MsgTemplateRepository.class),
            mock(MsgNotificationRepository.class),
            mock(JdbcTemplate.class),
            new ObjectMapper(),
            authService);
    }

    @SuppressWarnings("unchecked")
    private Set<Long> invokeResolveByRelation(String targetConfig, Long subjectId, String subjectType) throws Exception {
        Method m = MessageDispatcher.class.getDeclaredMethod(
            "resolveByRelation", String.class, Long.class, String.class);
        m.setAccessible(true);
        return (Set<Long>) m.invoke(dispatcher, targetConfig, subjectId, subjectType);
    }

    @Test
    void inward_callsAuthorizationServiceWithExpandImpliedTrue() throws Exception {
        when(authService.findSubjectsWithRelation(
            eq("place"), eq(101L), eq("viewer"), eq("user"), eq(true)))
            .thenReturn(List.of(42L, 77L));

        String config = "{\"relation\":\"viewer\",\"resource_type\":\"place\"}";
        Set<Long> ids = invokeResolveByRelation(config, 101L, "PLACE");

        assertThat(ids).containsExactlyInAnyOrder(42L, 77L);
        verify(authService).findSubjectsWithRelation("place", 101L, "viewer", "user", true);
    }

    @Test
    void inward_includeDeputies_callsAuthServiceTwice() throws Exception {
        when(authService.findSubjectsWithRelation(eq("org_unit"), eq(5L), eq("admin"), eq("user"), eq(true)))
            .thenReturn(List.of(1L, 2L));
        when(authService.findSubjectsWithRelation(eq("org_unit"), eq(5L), eq("deputy"), eq("user"), eq(true)))
            .thenReturn(List.of(3L));

        String config = "{\"relation\":\"admin\",\"resource_type\":\"org_unit\",\"include_deputies\":true}";
        Set<Long> ids = invokeResolveByRelation(config, 5L, "ORG_UNIT");

        assertThat(ids).containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    void inward_returnsEmpty_whenSubjectIdNull() throws Exception {
        Set<Long> ids = invokeResolveByRelation("{\"relation\":\"viewer\"}", null, "USER");
        assertThat(ids).isEmpty();
        verify(authService, never()).findSubjectsWithRelation(
            any(), any(), any(), anyBoolean());
        verify(authService, never()).findSubjectsWithRelation(
            any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void outward_doesNotCallAuthService_usesDirectJdbc() throws Exception {
        // outward 路径仍用裸 JDBC (见 dispatcher 里的注释), 不会走 AuthorizationService
        String config = "{\"relation\":\"guardian_of\",\"resource_type\":\"user\",\"direction\":\"outward\"}";
        invokeResolveByRelation(config, 100L, "USER");
        verify(authService, never()).findSubjectsWithRelation(
            any(), any(), any(), anyBoolean());
        verify(authService, never()).findSubjectsWithRelation(
            any(), any(), any(), any(), anyBoolean());
    }
}
