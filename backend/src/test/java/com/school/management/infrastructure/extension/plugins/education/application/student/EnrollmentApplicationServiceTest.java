package com.school.management.infrastructure.extension.plugins.education.application.student;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * EnrollmentApplicationService 单测 — 验证招生计划 CRUD 的 SQL 行为
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EnrollmentApplicationService 测试")
class EnrollmentApplicationServiceTest {

    @Mock
    private JdbcTemplate jdbc;

    @InjectMocks
    private EnrollmentApplicationService service;

    @Nested
    @DisplayName("招生计划 CRUD")
    class PlanCrudTests {

        @Test
        @DisplayName("deletePlan 应执行软删除")
        void shouldSoftDeletePlan() {
            when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);

            service.deletePlan(99L);

            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).update(sqlCap.capture(), eq(99L));
            assertThat(sqlCap.getValue()).contains("UPDATE enrollment_plans SET deleted=1");
        }

        @Test
        @DisplayName("publishPlan SQL 应只更新草稿状态的计划")
        void shouldOnlyPublishDraftPlan() {
            when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);

            service.publishPlan(42L);

            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).update(sqlCap.capture(), eq(42L));
            String sql = sqlCap.getValue();
            assertThat(sql).contains("status=1");
            assertThat(sql).contains("status=0"); // WHERE guard
            assertThat(sql).contains("deleted=0");
        }

        @Test
        @DisplayName("createPlan 应组装 INSERT 并填入字段")
        void shouldInsertPlan() {
            Map<String, Object> body = new HashMap<>();
            body.put("academicYear", 2024);
            body.put("majorId", 10L);
            body.put("majorDirectionId", 20L);
            body.put("orgUnitId", 5L);
            body.put("plannedCount", 100);
            body.put("enrollmentTarget", "初中毕业生");
            body.put("remark", "remark");

            when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);

            service.createPlan(body);

            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).update(sqlCap.capture(), any(Object[].class));
            assertThat(sqlCap.getValue()).contains("INSERT INTO enrollment_plans");
        }

        @Test
        @DisplayName("updatePlan 应在 WHERE 中只筛未删除")
        void shouldUpdateOnlyAliveRow() {
            Map<String, Object> body = new HashMap<>();
            body.put("academicYear", 2024);
            body.put("majorId", 10L);
            body.put("plannedCount", 50);

            when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);

            service.updatePlan(7L, body);

            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).update(sqlCap.capture(), any(Object[].class));
            String sql = sqlCap.getValue();
            assertThat(sql).contains("UPDATE enrollment_plans");
            assertThat(sql).contains("deleted=0");
        }
    }

    @Nested
    @DisplayName("入学申请")
    class ApplicationOpsTests {

        @Test
        @DisplayName("deleteApplication 应软删除")
        void shouldSoftDeleteApplication() {
            when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);

            service.deleteApplication(11L);

            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).update(sqlCap.capture(), eq(11L));
            assertThat(sqlCap.getValue()).contains("UPDATE enrollment_applications SET deleted=1");
        }

        @Test
        @DisplayName("updateApplication 应组装 UPDATE 并附 id")
        void shouldUpdateApplication() {
            Map<String, Object> body = new HashMap<>();
            body.put("applicantName", "张三");
            body.put("gender", 1);

            when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);

            service.updateApplication(33L, body);

            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).update(sqlCap.capture(), any(Object[].class));
            assertThat(sqlCap.getValue()).contains("UPDATE enrollment_applications");
        }
    }
}
