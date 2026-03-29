package com.school.management.domain.teaching.model.teachingclass;

public enum TeachingClassType {
    NORMAL(1, "普通"),
    COMBINED(2, "合堂"),
    WALKING(3, "走班");

    private final int code;
    private final String label;
    TeachingClassType(int code, String label) { this.code = code; this.label = label; }
    public int getCode() { return code; }
    public String getLabel() { return label; }
    public static TeachingClassType fromCode(int code) {
        for (TeachingClassType t : values()) if (t.code == code) return t;
        throw new IllegalArgumentException("Unknown TeachingClassType code: " + code);
    }
}
