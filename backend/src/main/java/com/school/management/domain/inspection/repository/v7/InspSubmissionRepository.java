package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.execution.InspSubmission;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InspSubmissionRepository {

    InspSubmission save(InspSubmission submission);

    List<InspSubmission> saveAll(List<InspSubmission> submissions);

    Optional<InspSubmission> findById(Long id);

    List<InspSubmission> findByTaskId(Long taskId);

    List<InspSubmission> findByTargetId(Long targetId);

    List<InspSubmission> findModifiedAfter(Long taskId, LocalDateTime since);

    void deleteById(Long id);

    void deleteByTaskId(Long taskId);
}
