package com.school.management.application.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TriggerPipelineHealthCheckTest {

    @Mock JdbcTemplate jdbc;
    @InjectMocks TriggerPipelineHealthCheck health;

    @Test
    void reportsHealthyWhenAllTablesPopulated() {
        when(jdbc.queryForObject(anyString(), eq(Long.class))).thenReturn(5L);
        health.checkOnStartup();
        assertThat(health.isHealthy()).isTrue();
        assertThat(health.getMissingTables()).isEmpty();
    }

    @Test
    void reportsUnhealthyWhenTriggerPointsEmpty() {
        when(jdbc.queryForObject(contains("trigger_points"), eq(Long.class))).thenReturn(0L);
        when(jdbc.queryForObject(contains("entity_event_types"), eq(Long.class))).thenReturn(5L);
        when(jdbc.queryForObject(contains("event_triggers"), eq(Long.class))).thenReturn(5L);
        when(jdbc.queryForObject(contains("msg_subscription_rules"), eq(Long.class))).thenReturn(5L);
        health.checkOnStartup();
        assertThat(health.isHealthy()).isFalse();
        assertThat(health.getMissingTables()).anyMatch(s -> s.contains("trigger_points"));
    }

    @Test
    void reportsUnhealthyOnQueryFailure() {
        when(jdbc.queryForObject(anyString(), eq(Long.class)))
            .thenThrow(new RuntimeException("table not found"));
        health.checkOnStartup();
        assertThat(health.isHealthy()).isFalse();
        assertThat(health.getMissingTables()).hasSize(4);
    }
}
