package com.school.management.domain.teaching.model;

/**
 * 年级-学期映射
 * 记录某年级在某学期处于培养方案的第几学期
 */
public class CohortSemesterMapping {
    private Long id;
    private Long cohortId;
    private Long semesterId;
    private Integer programSemester; // 培养方案第几学期 (1-8)
    private Long planId;
    private Integer status; // 1=正常 0=跳过

    protected CohortSemesterMapping() {}

    public static CohortSemesterMapping create(Long cohortId, Long semesterId,
                                                Integer programSemester, Long planId) {
        if (cohortId == null) throw new IllegalArgumentException("年级不能为空");
        if (semesterId == null) throw new IllegalArgumentException("学期不能为空");
        if (programSemester == null || programSemester < 1 || programSemester > 8)
            throw new IllegalArgumentException("培养方案学期序号必须在1-8之间");

        CohortSemesterMapping m = new CohortSemesterMapping();
        m.cohortId = cohortId;
        m.semesterId = semesterId;
        m.programSemester = programSemester;
        m.planId = planId;
        m.status = 1;
        return m;
    }

    public static CohortSemesterMapping reconstruct(Long id, Long cohortId, Long semesterId,
                                                     Integer programSemester, Long planId, Integer status) {
        CohortSemesterMapping m = new CohortSemesterMapping();
        m.id = id;
        m.cohortId = cohortId;
        m.semesterId = semesterId;
        m.programSemester = programSemester;
        m.planId = planId;
        m.status = status;
        return m;
    }

    /**
     * 根据入学年份和学期自动计算 programSemester
     * 例: enrollmentYear=2024, semesterStartYear=2025, semesterType=1 → 第3学期
     */
    public static int calculateProgramSemester(int enrollmentYear, int semesterStartYear, int semesterType) {
        int yearDiff = semesterStartYear - enrollmentYear;
        return yearDiff * 2 + semesterType;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCohortId() { return cohortId; }
    public Long getSemesterId() { return semesterId; }
    public Integer getProgramSemester() { return programSemester; }
    public Long getPlanId() { return planId; }
    public Integer getStatus() { return status; }
}
