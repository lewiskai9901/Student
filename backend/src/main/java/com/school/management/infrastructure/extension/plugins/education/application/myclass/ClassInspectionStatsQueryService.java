package com.school.management.infrastructure.extension.plugins.education.application.myclass;

import com.school.management.infrastructure.extension.plugins.education.application.myclass.query.MyClassOverviewDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 班级检查数据查询 — 为"我的班级"概览页提供检查得分/排名/趋势/申诉等统计.
 *
 * <p>数据来源是检查模块的 {@code insp_submissions} / {@code inspection_appeals}
 * (班级即 org_unit, 按 {@code org_unit_id} 关联). 之前 {@code MyClassApplicationService}
 * 这些字段全硬编码 0 ({@code // TODO: 从检查记录服务获取}) — 本服务把它们接上.
 *
 * <p>跨模块只读: 走 JdbcTemplate 读检查表 (无对应 DDD 仓储). 调用方
 * {@code MyClassApplicationService.getClassOverview} 已做 validateAccess 鉴权.
 * 无检查数据的班级各项自然为 0/空, 这是真实结果, 非 bug.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClassInspectionStatsQueryService {

    private final JdbcTemplate jdbc;

    /** 班级检查统计聚合结果. */
    public record ClassInspectionStats(
            Double averageScore,
            Integer classRank,
            Integer totalClasses,
            Double scoreTrend,
            Integer pendingAppeals,
            List<MyClassOverviewDTO.ScoreTrendItem> scoreTrendList,
            List<MyClassOverviewDTO.RecentCheckRecord> recentRecords) {

        /** 无数据时的安全默认 (各项 0/空), 用于查询异常兜底. */
        static ClassInspectionStats empty() {
            return new ClassInspectionStats(0.0, 0, 0, 0.0, 0,
                    new ArrayList<>(), new ArrayList<>());
        }
    }

    public ClassInspectionStats query(Long orgUnitId) {
        try {
            return doQuery(orgUnitId);
        } catch (Exception e) {
            log.warn("[ClassInspectionStats] 查询班级 {} 检查统计失败: {}", orgUnitId, e.getMessage());
            return ClassInspectionStats.empty();
        }
    }

    private ClassInspectionStats doQuery(Long orgUnitId) {
        // 1) 本班全部已完成检查的平均分
        Double avg = jdbc.queryForObject(
            "SELECT AVG(final_score) FROM insp_submissions " +
            "WHERE org_unit_id = ? AND deleted = 0 AND final_score IS NOT NULL",
            Double.class, orgUnitId);
        double averageScore = avg != null ? round1(avg) : 0.0;

        // 2) 排名 — 在所有有检查数据的 org 里按平均分排
        List<Map<String, Object>> allOrgAvgs = jdbc.queryForList(
            "SELECT org_unit_id, AVG(final_score) AS avg_score FROM insp_submissions " +
            "WHERE deleted = 0 AND final_score IS NOT NULL AND org_unit_id IS NOT NULL " +
            "GROUP BY org_unit_id ORDER BY avg_score DESC");
        int totalClasses = allOrgAvgs.size();
        int classRank = 0;
        for (int i = 0; i < allOrgAvgs.size(); i++) {
            Object oid = allOrgAvgs.get(i).get("org_unit_id");
            if (oid != null && orgUnitId.equals(((Number) oid).longValue())) {
                classRank = i + 1;
                break;
            }
        }

        // 3) 近 30 天按日趋势
        List<Map<String, Object>> trendRows = jdbc.queryForList(
            "SELECT DATE(created_at) AS d, AVG(final_score) AS s FROM insp_submissions " +
            "WHERE org_unit_id = ? AND deleted = 0 AND final_score IS NOT NULL " +
            "  AND created_at >= DATE_SUB(CURDATE(), INTERVAL 30 DAY) " +
            "GROUP BY DATE(created_at) ORDER BY d",
            orgUnitId);
        List<MyClassOverviewDTO.ScoreTrendItem> trendList = new ArrayList<>();
        for (Map<String, Object> r : trendRows) {
            Object s = r.get("s");
            trendList.add(MyClassOverviewDTO.ScoreTrendItem.builder()
                .date(String.valueOf(r.get("d")))
                .score(s != null ? round1(((Number) s).doubleValue()) : 0.0)
                .build());
        }

        // 4) 较上周变化 = 最近 7 天均分 − 前 7 天均分
        double scoreTrend = round1(weekAvg(orgUnitId, 7, 0) - weekAvg(orgUnitId, 14, 7));

        // 5) 待处理申诉数
        Integer pending = jdbc.queryForObject(
            "SELECT COUNT(*) FROM inspection_appeals " +
            "WHERE org_unit_id = ? AND status = 'PENDING' AND deleted = 0",
            Integer.class, orgUnitId);
        int pendingAppeals = pending != null ? pending : 0;

        // 6) 最近 5 条检查记录
        List<Map<String, Object>> recentRows = jdbc.queryForList(
            "SELECT id, created_at, final_score FROM insp_submissions " +
            "WHERE org_unit_id = ? AND deleted = 0 AND final_score IS NOT NULL " +
            "ORDER BY created_at DESC LIMIT 5",
            orgUnitId);
        List<MyClassOverviewDTO.RecentCheckRecord> recentRecords = new ArrayList<>();
        for (Map<String, Object> r : recentRows) {
            Object sc = r.get("final_score");
            recentRecords.add(MyClassOverviewDTO.RecentCheckRecord.builder()
                .id(((Number) r.get("id")).longValue())
                .checkDate(String.valueOf(r.get("created_at")))
                .checkType("检查记录")
                .score(sc != null ? round1(((Number) sc).doubleValue()) : 0.0)
                .rank(null)
                .build());
        }

        return new ClassInspectionStats(averageScore, classRank, totalClasses,
                scoreTrend, pendingAppeals, trendList, recentRecords);
    }

    /** [fromDaysAgo, toDaysAgo) 区间内本班均分; 无数据返回 0. */
    private double weekAvg(Long orgUnitId, int fromDaysAgo, int toDaysAgo) {
        Double v = jdbc.queryForObject(
            "SELECT AVG(final_score) FROM insp_submissions " +
            "WHERE org_unit_id = ? AND deleted = 0 AND final_score IS NOT NULL " +
            "  AND created_at >= DATE_SUB(CURDATE(), INTERVAL ? DAY) " +
            "  AND created_at <  DATE_SUB(CURDATE(), INTERVAL ? DAY)",
            Double.class, orgUnitId, fromDaysAgo, toDaysAgo);
        return v != null ? v : 0.0;
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
