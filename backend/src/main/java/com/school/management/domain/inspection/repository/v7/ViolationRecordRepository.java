package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.execution.ViolationRecord;

import java.util.List;
import java.util.Optional;

public interface ViolationRecordRepository {

    ViolationRecord save(ViolationRecord record);

    Optional<ViolationRecord> findById(Long id);

    List<ViolationRecord> findBySubmissionId(Long submissionId);

    List<ViolationRecord> findBySubmissionDetailId(Long submissionDetailId);

    List<ViolationRecord> findByUserId(Long userId);

    void deleteById(Long id);
}
