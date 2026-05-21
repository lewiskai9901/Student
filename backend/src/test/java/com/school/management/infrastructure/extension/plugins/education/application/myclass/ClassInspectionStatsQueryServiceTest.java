package com.school.management.infrastructure.extension.plugins.education.application.myclass;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * ClassInspectionStatsQueryService 单测 — 验证班级检查统计的聚合/排名/趋势/兜底逻辑.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ClassInspectionStatsQueryService 班级检查统计")
class ClassInspectionStatsQueryServiceTest {

    @Mock
    private JdbcTemplate jdbc;

    @InjectMocks
    private ClassInspectionStatsQueryService service;

    private Map<String, Object> orgAvg(long orgId, double avg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("org_unit_id", orgId);
        m.put("avg_score", avg);
        return m;
    }

    @Nested
    @DisplayName("query — 正常聚合")
    class QueryTests {

        @Test
        @DisplayName("聚合平均分/排名/趋势/申诉/最近记录")
        void shouldAggregateStats() {
            // 平均分
            when(jdbc.queryForObject(contains("AVG(final_score)"), eq(Double.class), eq(7L)))
                .thenReturn(85.34);
            // 排名 — 7L 排第 2
            when(jdbc.queryForList(contains("GROUP BY org_unit_id")))
                .thenReturn(List.of(orgAvg(9L, 90.0), orgAvg(7L, 85.3), orgAvg(3L, 70.0)));
            // 近 30 天趋势
            List<Map<String, Object>> trend = new ArrayList<>();
            Map<String, Object> t1 = new LinkedHashMap<>();
            t1.put("d", "2026-05-01"); t1.put("s", 80.0);
            trend.add(t1);
            when(jdbc.queryForList(contains("GROUP BY DATE(created_at)"), eq(7L))).thenReturn(trend);
            // 较上周变化 (两次 weekAvg, 3 个参数)
            when(jdbc.queryForObject(contains("INTERVAL ? DAY"), eq(Double.class), any(), any(), any()))
                .thenReturn(88.0, 84.0);
            // 待处理申诉
            when(jdbc.queryForObject(contains("inspection_appeals"), eq(Integer.class), eq(7L)))
                .thenReturn(2);
            // 最近记录
            Map<String, Object> rec = new LinkedHashMap<>();
            rec.put("id", 100L); rec.put("created_at", "2026-05-10"); rec.put("final_score", 91.0);
            when(jdbc.queryForList(contains("ORDER BY created_at DESC"), eq(7L)))
                .thenReturn(List.of(rec));

            var stats = service.query(7L);

            assertThat(stats.averageScore()).isEqualTo(85.3);
            assertThat(stats.classRank()).isEqualTo(2);
            assertThat(stats.totalClasses()).isEqualTo(3);
            assertThat(stats.scoreTrend()).isEqualTo(4.0); // 88 - 84
            assertThat(stats.pendingAppeals()).isEqualTo(2);
            assertThat(stats.scoreTrendList()).hasSize(1);
            assertThat(stats.scoreTrendList().get(0).getScore()).isEqualTo(80.0);
            assertThat(stats.recentRecords()).hasSize(1);
            assertThat(stats.recentRecords().get(0).getId()).isEqualTo(100L);
            assertThat(stats.recentRecords().get(0).getScore()).isEqualTo(91.0);
        }

        @Test
        @DisplayName("本班不在排名列表时 classRank=0")
        void shouldGiveRankZeroWhenNotRanked() {
            lenient().when(jdbc.queryForObject(contains("AVG(final_score)"), eq(Double.class), any()))
                .thenReturn(null);
            when(jdbc.queryForList(contains("GROUP BY org_unit_id")))
                .thenReturn(List.of(orgAvg(9L, 90.0)));
            when(jdbc.queryForList(contains("GROUP BY DATE(created_at)"), eq(404L))).thenReturn(new ArrayList<>());
            lenient().when(jdbc.queryForObject(contains("INTERVAL ? DAY"), eq(Double.class), any(), any(), any()))
                .thenReturn(null);
            when(jdbc.queryForObject(contains("inspection_appeals"), eq(Integer.class), any())).thenReturn(0);
            when(jdbc.queryForList(contains("ORDER BY created_at DESC"), eq(404L))).thenReturn(new ArrayList<>());

            var stats = service.query(404L);

            assertThat(stats.classRank()).isEqualTo(0);
            assertThat(stats.totalClasses()).isEqualTo(1);
            assertThat(stats.averageScore()).isEqualTo(0.0); // null avg → 0
        }

        @Test
        @DisplayName("查询抛异常时返回安全空值")
        void shouldFallBackToEmptyOnException() {
            when(jdbc.queryForObject(anyString(), eq(Double.class), any()))
                .thenThrow(new RuntimeException("db down"));

            var stats = service.query(7L);

            assertThat(stats.averageScore()).isEqualTo(0.0);
            assertThat(stats.classRank()).isEqualTo(0);
            assertThat(stats.pendingAppeals()).isEqualTo(0);
            assertThat(stats.scoreTrendList()).isEmpty();
            assertThat(stats.recentRecords()).isEmpty();
        }
    }
}
