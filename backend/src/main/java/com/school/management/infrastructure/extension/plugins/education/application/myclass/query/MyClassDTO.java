package com.school.management.application.myclass.query;

import lombok.Data;
import lombok.Builder;
import java.util.List;

/**
 * 我的班级列表项 DTO
 */
@Data
@Builder
public class MyClassDTO {
    private Long id;
    private String classCode;
    private String className;
    private String shortName;
    private Integer currentSize;
    private Integer standardSize;
    private String status;
    private Integer enrollmentYear;
    private String majorName;
    private String orgUnitName;
    // 当前用户在该班级的角色
    private String myRole;
    // 班级排名（本周/本月）
    private Integer weeklyRank;
    private Integer totalClasses;
    // 本周平均分
    private Double weeklyScore;
    // 近5次得分趋势
    private List<Double> scoreTrend;
}
