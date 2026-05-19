package com.school.management.infrastructure.extension.plugins.education.application.teaching.scheduling;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ScheduleInstanceApplicationService 单测 — 验证 substitute / cancel / restore / hours 分组
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduleInstanceApplicationService 测试")
class ScheduleInstanceApplicationServiceTest {

    @Mock
    private JdbcTemplate jdbc;

    @InjectMocks
    private ScheduleInstanceApplicationService service;

    @Nested
    @DisplayName("代课/取消/恢复")
    class WriteOperationsTests {

        @Test
        @DisplayName("代课 — original_teacher 入档, status=4")
        void shouldSubstituteTeacher() {
            when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);

            service.substituteTeacher(100L, 222L, "请假");

            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).update(sqlCap.capture(),
                    /* newTeacherId */ org.mockito.ArgumentMatchers.eq(222L),
                    /* reason */ org.mockito.ArgumentMatchers.eq("请假"),
                    /* instanceId */ org.mockito.ArgumentMatchers.eq(100L));
            String sql = sqlCap.getValue();
            assertThat(sql).contains("original_teacher_id = teacher_id");
            assertThat(sql).contains("status = 4");
        }

        @Test
        @DisplayName("取消实例 — status=1, 携带原因")
        void shouldCancelInstance() {
            when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);

            service.cancelInstance(99L, "停电");

            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).update(sqlCap.capture(),
                    org.mockito.ArgumentMatchers.eq("停电"),
                    org.mockito.ArgumentMatchers.eq(99L));
            assertThat(sqlCap.getValue()).contains("status = 1");
            assertThat(sqlCap.getValue()).contains("cancel_reason = ?");
        }

        @Test
        @DisplayName("恢复实例 — status=0, cancel_reason 清空")
        void shouldRestoreInstance() {
            when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);

            service.restoreInstance(99L);

            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).update(sqlCap.capture(), org.mockito.ArgumentMatchers.eq(99L));
            assertThat(sqlCap.getValue()).contains("status = 0");
            assertThat(sqlCap.getValue()).contains("cancel_reason = NULL");
        }
    }

    @Nested
    @DisplayName("实例服务可用性")
    class ServiceAvailabilityTests {

        @Test
        @DisplayName("未注入 instanceService 时 generateInstances 抛异常")
        void shouldFailGenerateWhenServiceMissing() {
            assertThatThrownBy(() -> service.generateInstances(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("未启用");
        }

        @Test
        @DisplayName("未注入 instanceService 时 applyCalendarEvent 抛异常")
        void shouldFailApplyEventWhenServiceMissing() {
            assertThatThrownBy(() -> service.applyCalendarEvent(1L))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("isInstanceServiceEnabled 在未注入时返回 false")
        void shouldReturnFalseWhenServiceMissing() {
            assertThat(service.isInstanceServiceEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("课时统计")
    class HoursStatsTests {

        @Test
        @DisplayName("非法 groupBy 抛异常")
        void shouldFailOnInvalidGroupBy() {
            assertThatThrownBy(() -> service.hoursStatistics(1L, "invalid", null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("无效的 groupBy");
        }

        @Test
        @DisplayName("按教师分组 SQL 包含 users 联结")
        void shouldUseTeacherJoinForGroupByTeacher() {
            when(jdbc.queryForList(anyString(), any(Object[].class)))
                    .thenReturn(Collections.emptyList());

            service.hoursStatistics(1L, "teacher", null, null, null);

            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).queryForList(sqlCap.capture(), any(Object[].class));
            String sql = sqlCap.getValue();
            assertThat(sql).contains("si.teacher_id");
            assertThat(sql).contains("LEFT JOIN users u");
        }

        @Test
        @DisplayName("按课程分组 SQL 包含 courses 联结")
        void shouldUseCourseJoinForGroupByCourse() {
            when(jdbc.queryForList(anyString(), any(Object[].class)))
                    .thenReturn(Collections.emptyList());

            service.hoursStatistics(1L, "course", null, null, null);

            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).queryForList(sqlCap.capture(), any(Object[].class));
            assertThat(sqlCap.getValue()).contains("LEFT JOIN courses c");
        }
    }
}
