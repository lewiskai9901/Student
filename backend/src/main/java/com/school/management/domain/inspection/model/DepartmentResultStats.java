package com.school.management.domain.inspection.model;

import java.math.BigDecimal;

/**
 * Value object representing department-level ranking statistics.
 */
public class DepartmentResultStats {

    private final Long orgUnitId;
    private final String orgUnitName;
    private final BigDecimal averageClassScore;
    private final int classCount;
    private final BigDecimal compositeScore;
    private final int ranking;

    public DepartmentResultStats(Long orgUnitId, String orgUnitName,
                                  BigDecimal averageClassScore, int classCount,
                                  BigDecimal compositeScore, int ranking) {
        this.orgUnitId = orgUnitId;
        this.orgUnitName = orgUnitName;
        this.averageClassScore = averageClassScore;
        this.classCount = classCount;
        this.compositeScore = compositeScore;
        this.ranking = ranking;
    }

    public Long getOrgUnitId() { return orgUnitId; }
    public String getOrgUnitName() { return orgUnitName; }
    public BigDecimal getAverageClassScore() { return averageClassScore; }
    public int getClassCount() { return classCount; }
    public BigDecimal getCompositeScore() { return compositeScore; }
    public int getRanking() { return ranking; }
}
