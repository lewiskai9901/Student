package com.school.management.domain.corrective.repository;

import com.school.management.domain.corrective.model.ActionStatus;
import com.school.management.domain.corrective.model.CorrectiveAction;
import com.school.management.domain.shared.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for CorrectiveAction aggregate.
 */
public interface CorrectiveActionRepository extends Repository<CorrectiveAction, Long> {

    Optional<CorrectiveAction> findByActionCode(String actionCode);

    List<CorrectiveAction> findByStatus(ActionStatus status);

    List<CorrectiveAction> findByClassId(Long classId);

    List<CorrectiveAction> findByAssigneeId(Long assigneeId);

    List<CorrectiveAction> findOverdue();

    long countByStatus(ActionStatus status);

    List<CorrectiveAction> findBySourceAndSourceId(String source, Long sourceId);
}
