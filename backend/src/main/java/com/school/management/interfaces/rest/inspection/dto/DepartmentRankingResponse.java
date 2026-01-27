package com.school.management.interfaces.rest.inspection.dto;

import com.school.management.domain.inspection.model.DepartmentResultStats;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DepartmentRankingResponse {

    private Long orgUnitId;
    private String orgUnitName;
    private BigDecimal averageClassScore;
    private int classCount;
    private BigDecimal compositeScore;
    private int ranking;

    public static DepartmentRankingResponse fromDomain(DepartmentResultStats stats) {
        DepartmentRankingResponse r = new DepartmentRankingResponse();
        r.setOrgUnitId(stats.getOrgUnitId());
        r.setOrgUnitName(stats.getOrgUnitName());
        r.setAverageClassScore(stats.getAverageClassScore());
        r.setClassCount(stats.getClassCount());
        r.setCompositeScore(stats.getCompositeScore());
        r.setRanking(stats.getRanking());
        return r;
    }
}
