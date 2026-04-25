package com.school.management.domain.inspection.repository;

import com.school.management.domain.inspection.model.corrective.CorrectiveSubtask;

import java.util.List;
import java.util.Optional;

public interface CorrectiveSubtaskRepository {

    CorrectiveSubtask save(CorrectiveSubtask subtask);

    Optional<CorrectiveSubtask> findById(Long id);

    List<CorrectiveSubtask> findByCaseId(Long caseId);

    void deleteById(Long id);

    int countByCaseIdAndStatus(Long caseId, String status);
}
