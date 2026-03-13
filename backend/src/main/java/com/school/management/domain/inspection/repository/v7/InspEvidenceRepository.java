package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.execution.InspEvidence;

import java.util.List;
import java.util.Optional;

public interface InspEvidenceRepository {

    InspEvidence save(InspEvidence evidence);

    Optional<InspEvidence> findById(Long id);

    List<InspEvidence> findBySubmissionId(Long submissionId);

    List<InspEvidence> findByDetailId(Long detailId);

    void deleteById(Long id);

    void deleteBySubmissionId(Long submissionId);
}
