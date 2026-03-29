package com.school.management.domain.teaching.repository;

import com.school.management.domain.teaching.model.scheduling.ConstraintLevel;
import com.school.management.domain.teaching.model.scheduling.SchedulingConstraint;
import java.util.List;
import java.util.Optional;

public interface SchedulingConstraintRepository {
    SchedulingConstraint save(SchedulingConstraint constraint);
    Optional<SchedulingConstraint> findById(Long id);
    List<SchedulingConstraint> findBySemesterId(Long semesterId);
    List<SchedulingConstraint> findBySemesterIdAndLevel(Long semesterId, ConstraintLevel level);
    List<SchedulingConstraint> findBySemesterIdAndLevelAndTargetId(Long semesterId, ConstraintLevel level, Long targetId);
    List<SchedulingConstraint> findEnabledBySemesterId(Long semesterId);
    void deleteById(Long id);
}
