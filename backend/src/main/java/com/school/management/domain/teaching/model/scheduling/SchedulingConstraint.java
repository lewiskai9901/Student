package com.school.management.domain.teaching.model.scheduling;

import com.school.management.domain.shared.AggregateRoot;
import lombok.Getter;

@Getter
public class SchedulingConstraint extends AggregateRoot<Long> {
    private Long semesterId;
    private String constraintName;
    private ConstraintLevel constraintLevel;
    private Long targetId;
    private String targetName;
    private ConstraintType constraintType;
    private Boolean isHard;
    private Integer priority;
    private String params; // JSON string
    private String effectiveWeeks;
    private Boolean enabled;
    private Long createdBy;

    protected SchedulingConstraint() {}

    public static SchedulingConstraint create(Long semesterId, String constraintName,
            ConstraintLevel level, Long targetId, String targetName,
            ConstraintType type, Boolean isHard, Integer priority,
            String params, String effectiveWeeks, Long createdBy) {
        SchedulingConstraint c = new SchedulingConstraint();
        c.semesterId = semesterId;
        c.constraintName = constraintName;
        c.constraintLevel = level;
        c.targetId = targetId;
        c.targetName = targetName;
        c.constraintType = type;
        c.isHard = isHard != null ? isHard : true;
        c.priority = priority != null ? priority : 50;
        c.params = params;
        c.effectiveWeeks = effectiveWeeks;
        c.enabled = true;
        c.createdBy = createdBy;
        return c;
    }

    public static SchedulingConstraint reconstruct(Long id, Long semesterId, String constraintName,
            ConstraintLevel level, Long targetId, String targetName,
            ConstraintType type, Boolean isHard, Integer priority,
            String params, String effectiveWeeks, Boolean enabled, Long createdBy) {
        SchedulingConstraint c = new SchedulingConstraint();
        c.id = id;
        c.semesterId = semesterId;
        c.constraintName = constraintName;
        c.constraintLevel = level;
        c.targetId = targetId;
        c.targetName = targetName;
        c.constraintType = type;
        c.isHard = isHard;
        c.priority = priority;
        c.params = params;
        c.effectiveWeeks = effectiveWeeks;
        c.enabled = enabled;
        c.createdBy = createdBy;
        return c;
    }

    public void update(String constraintName, Boolean isHard, Integer priority,
            String params, String effectiveWeeks) {
        this.constraintName = constraintName;
        this.isHard = isHard;
        this.priority = priority;
        this.params = params;
        this.effectiveWeeks = effectiveWeeks;
    }

    public void enable() { this.enabled = true; }
    public void disable() { this.enabled = false; }
}
