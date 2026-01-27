package com.school.management.domain.inspection.service;

import java.math.BigDecimal;

/**
 * Value object representing a class's allocation in a physical space.
 */
public class ClassAllocation {

    private final Long classId;
    private final String className;
    private final Long orgUnitId;
    private final String orgUnitName;
    private final int studentCount;
    private final BigDecimal ratio;

    public ClassAllocation(Long classId, String className, Long orgUnitId, String orgUnitName,
                           int studentCount, BigDecimal ratio) {
        this.classId = classId;
        this.className = className;
        this.orgUnitId = orgUnitId;
        this.orgUnitName = orgUnitName;
        this.studentCount = studentCount;
        this.ratio = ratio;
    }

    public Long getClassId() { return classId; }
    public String getClassName() { return className; }
    public Long getOrgUnitId() { return orgUnitId; }
    public String getOrgUnitName() { return orgUnitName; }
    public int getStudentCount() { return studentCount; }
    public BigDecimal getRatio() { return ratio; }
}
