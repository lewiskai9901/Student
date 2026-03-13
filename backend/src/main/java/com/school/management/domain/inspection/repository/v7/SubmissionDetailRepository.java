package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.execution.SubmissionDetail;

import java.util.List;
import java.util.Optional;

public interface SubmissionDetailRepository {

    SubmissionDetail save(SubmissionDetail detail);

    Optional<SubmissionDetail> findById(Long id);

    List<SubmissionDetail> findBySubmissionId(Long submissionId);

    List<SubmissionDetail> findFlaggedBySubmissionId(Long submissionId);

    void deleteById(Long id);

    void deleteBySubmissionId(Long submissionId);
}
