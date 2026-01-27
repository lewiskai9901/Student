package com.school.management.infrastructure.persistence.inspection;

import com.school.management.domain.inspection.model.ClassInspectionRecord;
import com.school.management.domain.inspection.model.DepartmentResultStats;
import com.school.management.domain.inspection.service.DepartmentRankingService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of DepartmentRankingService.
 * Groups records by orgUnitId, computes average class score, and assigns rankings
 * with same-score ties sharing the same rank.
 */
@Service
public class DepartmentRankingServiceImpl implements DepartmentRankingService {

    @Override
    public List<DepartmentResultStats> calculateDepartmentRanking(
            List<ClassInspectionRecord> records, BigDecimal classWeight) {

        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }

        BigDecimal weight = classWeight != null ? classWeight : BigDecimal.ONE;

        // Group by orgUnitId
        Map<Long, List<ClassInspectionRecord>> byDept = records.stream()
            .filter(r -> r.getOrgUnitId() != null)
            .collect(Collectors.groupingBy(ClassInspectionRecord::getOrgUnitId));

        // Compute stats per department
        List<DeptScore> deptScores = new ArrayList<>();
        for (Map.Entry<Long, List<ClassInspectionRecord>> entry : byDept.entrySet()) {
            Long orgUnitId = entry.getKey();
            List<ClassInspectionRecord> deptRecords = entry.getValue();

            String orgUnitName = deptRecords.get(0).getOrgUnitName();
            int classCount = deptRecords.size();

            BigDecimal avgScore = deptRecords.stream()
                .map(ClassInspectionRecord::getFinalScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(classCount), 2, RoundingMode.HALF_UP);

            BigDecimal compositeScore = avgScore.multiply(weight).setScale(2, RoundingMode.HALF_UP);

            deptScores.add(new DeptScore(orgUnitId, orgUnitName, avgScore, classCount, compositeScore));
        }

        // Sort descending by compositeScore
        deptScores.sort((a, b) -> b.compositeScore.compareTo(a.compositeScore));

        // Assign rankings with ties
        List<DepartmentResultStats> result = new ArrayList<>();
        int rank = 1;
        for (int i = 0; i < deptScores.size(); i++) {
            if (i > 0 && deptScores.get(i).compositeScore.compareTo(deptScores.get(i - 1).compositeScore) != 0) {
                rank = i + 1;
            }
            DeptScore ds = deptScores.get(i);
            result.add(new DepartmentResultStats(
                ds.orgUnitId, ds.orgUnitName, ds.avgScore, ds.classCount, ds.compositeScore, rank
            ));
        }

        return result;
    }

    private static class DeptScore {
        final Long orgUnitId;
        final String orgUnitName;
        final BigDecimal avgScore;
        final int classCount;
        final BigDecimal compositeScore;

        DeptScore(Long orgUnitId, String orgUnitName, BigDecimal avgScore, int classCount, BigDecimal compositeScore) {
            this.orgUnitId = orgUnitId;
            this.orgUnitName = orgUnitName;
            this.avgScore = avgScore;
            this.classCount = classCount;
            this.compositeScore = compositeScore;
        }
    }
}
