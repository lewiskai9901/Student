package com.school.management.domain.inspection.repository;

import com.school.management.domain.inspection.model.execution.SubmissionDetail;

import java.util.List;
import java.util.Optional;

public interface SubmissionDetailRepository {

    SubmissionDetail save(SubmissionDetail detail);

    List<SubmissionDetail> saveAll(List<SubmissionDetail> details);

    Optional<SubmissionDetail> findById(Long id);

    List<SubmissionDetail> findBySubmissionId(Long submissionId);

    List<SubmissionDetail> findFlaggedBySubmissionId(Long submissionId);

    void deleteById(Long id);

    void deleteBySubmissionId(Long submissionId);
}
