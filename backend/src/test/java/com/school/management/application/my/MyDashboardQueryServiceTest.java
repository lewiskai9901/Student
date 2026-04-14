package com.school.management.application.my;

import com.school.management.interfaces.rest.my.MyDashboardController.DashboardSummaryDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 单元测试 {@link MyDashboardQueryService} 的纯逻辑部分:
 * <ul>
 *   <li>角色优先级 — 决定一个老师兼任多角色时在"我的班级"里显示哪一个</li>
 *   <li>DashboardSummaryDTO 空值保护 — DB 返回 null 时前端期望 0 而非 NPE</li>
 * </ul>
 *
 * <p>未覆盖:
 * <ul>
 *   <li>SQL 语义(JOIN/WHERE 正确性)— 需 IT harness,见 AccessRelationRepositoryImplIT 注释</li>
 *   <li>RowMapper 字段映射 — 需构造假 ResultSet,成本高于价值</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class MyDashboardQueryServiceTest {

    @Nested
    @DisplayName("roleRank — 业务优先级 (班主任 > 副班主任 > 辅导员 > 科任)")
    class RolePriority {

        @Test
        @DisplayName("HEAD_TEACHER 最高优先")
        void headTeacher_winsOverOthers() {
            assertThat(MyDashboardQueryService.preferHigherRole("HEAD_TEACHER", "SUBJECT_TEACHER"))
                    .isEqualTo("HEAD_TEACHER");
            assertThat(MyDashboardQueryService.preferHigherRole("SUBJECT_TEACHER", "HEAD_TEACHER"))
                    .isEqualTo("HEAD_TEACHER");
        }

        @Test
        @DisplayName("副班主任两种拼写都识别")
        void deputyHead_bothSpellings() {
            assertThat(MyDashboardQueryService.roleRank("DEPUTY_HEAD"))
                    .isEqualTo(MyDashboardQueryService.roleRank("DEPUTY_HEAD_TEACHER"));
            assertThat(MyDashboardQueryService.preferHigherRole("DEPUTY_HEAD", "COUNSELOR"))
                    .isEqualTo("DEPUTY_HEAD");
        }

        @Test
        @DisplayName("未知角色 rank=99,任何已知角色都比它高")
        void unknownRole_lowestPriority() {
            assertThat(MyDashboardQueryService.roleRank("NONSENSE")).isEqualTo(99);
            assertThat(MyDashboardQueryService.preferHigherRole("SUBJECT_TEACHER", "NONSENSE"))
                    .isEqualTo("SUBJECT_TEACHER");
        }

        @Test
        @DisplayName("null 角色 rank=99,被任何具名角色覆盖")
        void nullRole_treatedAsUnknown() {
            assertThat(MyDashboardQueryService.roleRank(null)).isEqualTo(99);
            assertThat(MyDashboardQueryService.preferHigherRole(null, "COUNSELOR"))
                    .isEqualTo("COUNSELOR");
        }

        @Test
        @DisplayName("完整排序链: HEAD > DEPUTY > COUNSELOR > SUBJECT")
        void fullOrdering() {
            int head = MyDashboardQueryService.roleRank("HEAD_TEACHER");
            int deputy = MyDashboardQueryService.roleRank("DEPUTY_HEAD");
            int counselor = MyDashboardQueryService.roleRank("COUNSELOR");
            int subject = MyDashboardQueryService.roleRank("SUBJECT_TEACHER");
            assertThat(head).isLessThan(deputy);
            assertThat(deputy).isLessThan(counselor);
            assertThat(counselor).isLessThan(subject);
        }
    }

    @Nested
    @DisplayName("getSummary — null 值映射为 0")
    class SummaryNullSafety {

        @Mock
        JdbcTemplate jdbcTemplate;

        @Test
        @DisplayName("DB 返回 null 时 DTO 字段为 0,不抛 NPE")
        void allNulls_mapToZero() {
            when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(Object[].class)))
                    .thenReturn(null);

            DashboardSummaryDTO summary = new MyDashboardQueryService(jdbcTemplate).getSummary(100L);

            assertThat(summary.todayLessons()).isZero();
            assertThat(summary.weeklyHoursCurrent()).isZero();
            assertThat(summary.weeklyHoursTotal()).isZero();
            assertThat(summary.substituteRequests()).isZero();
        }
    }
}
