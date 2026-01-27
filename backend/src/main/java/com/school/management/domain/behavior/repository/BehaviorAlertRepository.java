package com.school.management.domain.behavior.repository;

import com.school.management.domain.behavior.model.BehaviorAlert;
import com.school.management.domain.shared.Repository;

import java.util.List;

/**
 * Repository interface for BehaviorAlert entity.
 */
public interface BehaviorAlertRepository extends Repository<BehaviorAlert, Long> {

    List<BehaviorAlert> findByStudentId(Long studentId);

    List<BehaviorAlert> findUnhandledByClassId(Long classId);

    long countUnhandledByClassId(Long classId);

    List<BehaviorAlert> findByClassId(Long classId);
}
