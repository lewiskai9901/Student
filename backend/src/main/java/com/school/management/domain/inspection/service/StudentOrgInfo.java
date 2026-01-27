package com.school.management.domain.inspection.service;

/**
 * Value object containing a student's organizational information.
 */
public class StudentOrgInfo {

    private final Long studentId;
    private final String studentName;
    private final Long classId;
    private final String className;
    private final Long orgUnitId;
    private final String orgUnitName;

    public StudentOrgInfo(Long studentId, String studentName, Long classId, String className,
                          Long orgUnitId, String orgUnitName) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.classId = classId;
        this.className = className;
        this.orgUnitId = orgUnitId;
        this.orgUnitName = orgUnitName;
    }

    public Long getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public Long getClassId() { return classId; }
    public String getClassName() { return className; }
    public Long getOrgUnitId() { return orgUnitId; }
    public String getOrgUnitName() { return orgUnitName; }
}
