package com.school.management.domain.inspection.repository.v6;

import com.school.management.domain.inspection.model.v6.InspectionTarget;
import com.school.management.domain.inspection.model.v6.TargetStatus;
import com.school.management.domain.inspection.model.v6.TargetType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * V6检查目标仓储接口
 */
public interface InspectionTargetRepository {

    InspectionTarget save(InspectionTarget target);

    void saveAll(List<InspectionTarget> targets);

    Optional<InspectionTarget> findById(Long id);

    List<InspectionTarget> findByTaskId(Long taskId);

    List<InspectionTarget> findByTaskIdAndStatus(Long taskId, TargetStatus status);

    Optional<InspectionTarget> findByTaskAndTarget(Long taskId, TargetType targetType, Long targetId);

    List<InspectionTarget> findByOrgUnitId(Long orgUnitId);

    List<InspectionTarget> findByClassId(Long classId);

    boolean lockTarget(Long id, Long lockedBy);

    boolean unlockTarget(Long id);

    void completeTarget(Long id);

    void skipTarget(Long id, String skipReason);

    void addDeduction(Long id, BigDecimal deduction);

    void addBonus(Long id, BigDecimal bonus);

    int countByTaskId(Long taskId);

    int countCompletedByTaskId(Long taskId);

    int countSkippedByTaskId(Long taskId);
}
