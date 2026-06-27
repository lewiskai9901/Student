package com.school.management.application.plugin;

import com.school.management.infrastructure.extension.event.PermissionsRefreshedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PluginLifecycleService 级联禁用/恢复单测.
 */
class PluginLifecycleServiceTest {

    private JdbcTemplate jdbc;
    private ApplicationEventPublisher publisher;
    private PluginLifecycleService service;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        publisher = mock(ApplicationEventPublisher.class);
        service = new PluginLifecycleService(jdbc, publisher);
    }

    @Test
    void disable_cascadesAllTablesAndPublishesEvent() {
        // dependency check: no dependents
        when(jdbc.queryForList(anyString(), eq(String.class), any(), any()))
            .thenReturn(List.of());
        // 主表 UPDATE (1 个尾参 code) succeeded
        when(jdbc.update(startsWith("UPDATE plugin_packages SET enabled=0"), eq("EDU")))
            .thenReturn(1);
        // 级联 cascadePluginEnabled 用参数化 value: update(sql, value=0, code) → 2 个尾参
        when(jdbc.update(startsWith("UPDATE "), eq(0), eq("EDU"))).thenReturn(3);

        service.disable("EDU");

        // 9 贡献表 + 1 msg_subscription_rules = 至少 10 次参数化级联 UPDATE (value=0)
        verify(jdbc, atLeast(10)).update(anyString(), eq(0), eq("EDU"));
        // event published
        ArgumentCaptor<PermissionsRefreshedEvent> cap =
            ArgumentCaptor.forClass(PermissionsRefreshedEvent.class);
        verify(publisher).publishEvent(cap.capture());
        assertTrue(cap.getValue().getSourceName().startsWith("PLUGIN_DISABLE:EDU"));
    }

    @Test
    void disable_coreThrows() {
        assertThrows(IllegalStateException.class, () -> service.disable("CORE"));
        verifyNoInteractions(publisher);
    }

    @Test
    void disable_throwsIfDependedOn() {
        // dependency check returns dependents
        when(jdbc.queryForList(anyString(), eq(String.class), any(), any()))
            .thenReturn(List.of("DEPENDENT_X"));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> service.disable("CORE_REF"));
        assertTrue(ex.getMessage().contains("DEPENDENT_X"));
        // 未进入级联阶段
        verify(jdbc, never()).update(startsWith("UPDATE plugin_packages SET enabled=0"), eq("CORE_REF"));
    }

    @Test
    void disable_throwsIfPluginNotFound() {
        when(jdbc.queryForList(anyString(), eq(String.class), any(), any()))
            .thenReturn(List.of());
        when(jdbc.update(startsWith("UPDATE plugin_packages SET enabled=0"), eq("NOPE")))
            .thenReturn(0);

        assertThrows(IllegalStateException.class, () -> service.disable("NOPE"));
    }

    @Test
    void enable_cascadesAllTablesToEnabled1() {
        when(jdbc.update(startsWith("UPDATE plugin_packages SET enabled=1"), eq("EDU")))
            .thenReturn(1);
        // 级联恢复: update(sql, value=1, code)
        when(jdbc.update(startsWith("UPDATE "), eq(1), eq("EDU"))).thenReturn(3);

        service.enable("EDU");

        verify(jdbc, atLeast(10)).update(anyString(), eq(1), eq("EDU"));
        ArgumentCaptor<PermissionsRefreshedEvent> cap =
            ArgumentCaptor.forClass(PermissionsRefreshedEvent.class);
        verify(publisher).publishEvent(cap.capture());
        assertTrue(cap.getValue().getSourceName().startsWith("PLUGIN_ENABLE:EDU"));
    }
}
