package com.school.management.domain.teaching.model.scheduling;

public enum ConstraintType {
    TIME_FORBIDDEN("时间禁排"), TIME_FIXED("时间固定"), MAX_DAILY("每日上限"),
    MAX_CONSECUTIVE("最大连排"), ROOM_REQUIRED("教室要求"),
    TIME_PREFERRED("时间偏好"), TIME_AVOIDED("时间回避"), SPREAD_EVEN("均匀分布"),
    MORNING_PRIORITY("上午优先"), COMPACT_SCHEDULE("紧凑排课"),
    MIN_GAP("最小间隔"), ROOM_PREFERRED("教室偏好");
    private final String label;
    ConstraintType(String label) { this.label = label; }
    public String getLabel() { return label; }
}
