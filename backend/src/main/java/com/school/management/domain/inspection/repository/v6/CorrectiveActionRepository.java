package com.school.management.domain.inspection.repository.v6;

import com.school.management.domain.inspection.model.v6.CorrectiveAction;
import com.school.management.domain.inspection.model.v6.CorrectiveActionStatus;

import java.util.List;
import java.util.Optional;

/**
 * V6整改记录仓储接口
 */
public interface CorrectiveActionRepository {

    CorrectiveAction save(CorrectiveAction action);

    Optional<CorrectiveAction> findById(Long id);

    Optional<CorrectiveAction> findByActionCode(String actionCode);

    List<CorrectiveAction> findByDetailId(Long detailId);

    List<CorrectiveAction> findByTargetId(Long targetId);

    List<CorrectiveAction> findByTaskId(Long taskId);

    List<CorrectiveAction> findByProjectId(Long projectId);

    List<CorrectiveAction> findByAssigneeId(Long assigneeId);

    List<CorrectiveAction> findByStatus(CorrectiveActionStatus status);

    List<CorrectiveAction> findOverdue();

    List<CorrectiveAction> findByProjectIdAndStatus(Long projectId, CorrectiveActionStatus status);

    void deleteById(Long id);

    long countByProjectIdAndStatus(Long projectId, CorrectiveActionStatus status);
}
