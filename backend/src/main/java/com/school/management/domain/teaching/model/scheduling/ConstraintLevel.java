package com.school.management.domain.teaching.model.scheduling;

public enum ConstraintLevel {
    GLOBAL(1, "全局"), TEACHER(2, "教师"), CLASS(3, "班级"), COURSE(4, "课程");
    private final int code;
    private final String label;
    ConstraintLevel(int code, String label) { this.code = code; this.label = label; }
    public int getCode() { return code; }
    public String getLabel() { return label; }
    public static ConstraintLevel fromCode(int code) {
        for (ConstraintLevel l : values()) if (l.code == code) return l;
        throw new IllegalArgumentException("Unknown ConstraintLevel: " + code);
    }
}
