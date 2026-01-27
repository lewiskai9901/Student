package com.school.management.domain.inspection.service;

import com.school.management.domain.inspection.model.ClassInspectionRecord;
import com.school.management.domain.inspection.model.DepartmentResultStats;

import java.math.BigDecimal;
import java.util.List;

/**
 * Domain service for calculating department-level rankings.
 * Groups class records by org unit and computes composite scores with rankings.
 */
public interface DepartmentRankingService {

    /**
     * Calculates department ranking from class inspection records.
     * Groups by orgUnitId, computes average class score, sorts descending,
     * and assigns rankings with ties getting the same rank.
     *
     * @param records     all class inspection records to rank
     * @param classWeight weight factor for class score in composite calculation
     * @return ranked list of department statistics
     */
    List<DepartmentResultStats> calculateDepartmentRanking(
        List<ClassInspectionRecord> records, BigDecimal classWeight);
}
