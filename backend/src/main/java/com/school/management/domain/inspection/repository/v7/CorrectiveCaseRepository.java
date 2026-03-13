package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.corrective.CasePriority;
import com.school.management.domain.inspection.model.v7.corrective.CaseStatus;
import com.school.management.domain.inspection.model.v7.corrective.CorrectiveCase;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CorrectiveCaseRepository {

    CorrectiveCase save(CorrectiveCase correctiveCase);

    Optional<CorrectiveCase> findById(Long id);

    Optional<CorrectiveCase> findByCaseCode(String caseCode);

    List<CorrectiveCase> findByProjectId(Long projectId);

    List<CorrectiveCase> findBySubmissionId(Long submissionId);

    List<CorrectiveCase> findByAssigneeId(Long assigneeId);

    List<CorrectiveCase> findByStatus(CaseStatus status);

    List<CorrectiveCase> findByPriority(CasePriority priority);

    List<CorrectiveCase> findOverdue(LocalDateTime now);

    List<CorrectiveCase> findByTaskId(Long taskId);

    List<CorrectiveCase> findAll();

    void deleteById(Long id);
}
