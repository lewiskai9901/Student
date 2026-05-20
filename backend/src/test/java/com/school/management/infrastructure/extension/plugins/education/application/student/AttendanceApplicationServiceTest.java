package com.school.management.infrastructure.extension.plugins.education.application.student;

import com.school.management.infrastructure.access.OrgScopeHelper;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AttendanceApplicationService 单测 — 验证考勤记录 CRUD 的 SQL 行为
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AttendanceApplicationService 测试")
class AttendanceApplicationServiceTest {

    @Mock
    private JdbcTemplate jdbc;

    @Mock
    private OrgScopeHelper orgScopeHelper;

    @InjectMocks
    private AttendanceApplicationService service;

    @BeforeEach
    void setUpOrgScope() {
        // 数据权限默认放行 — clause = "" (no-op), 单个 org 校验 = true
        lenient().when(orgScopeHelper.orgScopeClause(anyString())).thenReturn("");
        lenient().when(orgScopeHelper.isOrgAllowed(any())).thenReturn(true);
        lenient().when(orgScopeHelper.isUnbounded()).thenReturn(true);
    }

    @Nested
    @DisplayName("考勤记录 CRUD")
    class RecordCrudTests {

        @Test
        @DisplayName("createRecord 应组装 INSERT 并传入正确顺序参数")
        void shouldInsertRecord() {
            when(jdbc.update(anyString(),
                    any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any())).thenReturn(1);

            int n = service.createRecord(
                    1L, 2L, 3L, 4L, "2024-09-01",
                    2, 1, 1, "MANUAL", "OK", 99L);

            assertThat(n).isEqualTo(1);
            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).update(sqlCap.capture(),
                    eq(1L), eq(2L), eq(3L), eq(4L), eq("2024-09-01"),
                    eq(2), eq(1), eq(1), eq("MANUAL"), eq("OK"), eq(99L));
            assertThat(sqlCap.getValue()).contains("INSERT INTO attendance_records");
        }

        @Test
        @DisplayName("listRecords 应组装 SELECT 并加 LIMIT")
        void shouldListWithPaging() {
            when(jdbc.queryForList(anyString(), any(Object[].class)))
                    .thenReturn(Collections.emptyList());

            service.listRecords(1L, null, null, null, null, null, null, null, null, 2, 20);

            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).queryForList(sqlCap.capture(), any(Object[].class));
            String sql = sqlCap.getValue();
            assertThat(sql).contains("SELECT ar.id");
            assertThat(sql).contains("LIMIT ? OFFSET ?");
        }

        @Test
        @DisplayName("listRecords 带学生ID应在 SQL 中添加过滤")
        void shouldFilterByStudent() {
            when(jdbc.queryForList(anyString(), any(Object[].class)))
                    .thenReturn(Collections.emptyList());

            service.listRecords(1L, null, 999L, null, null, null, null, null, null, 1, 10);

            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).queryForList(sqlCap.capture(), any(Object[].class));
            assertThat(sqlCap.getValue()).contains("ar.student_id = ?");
        }

        @Test
        @DisplayName("listRecords 仅传 semesterId 时 SQL 不含可选过滤")
        void shouldNotFilterWhenOptionalParamsNull() {
            when(jdbc.queryForList(anyString(), any(Object[].class)))
                    .thenReturn(Collections.emptyList());

            service.listRecords(1L, null, null, null, null, null, null, null, null, 1, 10);

            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            verify(jdbc).queryForList(sqlCap.capture(), any(Object[].class));
            String sql = sqlCap.getValue();
            assertThat(sql).contains("ar.semester_id = ?");
            assertThat(sql).doesNotContain("ar.student_id = ?");
            assertThat(sql).doesNotContain("ar.course_id = ?");
        }
    }
}
