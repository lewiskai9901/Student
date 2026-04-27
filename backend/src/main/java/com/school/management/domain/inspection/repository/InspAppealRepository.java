package com.school.management.domain.inspection.repository;

import com.school.management.domain.inspection.model.appeal.AppealStatus;
import com.school.management.domain.inspection.model.appeal.InspAppeal;

import java.util.List;
import java.util.Optional;

public interface InspAppealRepository {

    InspAppeal save(InspAppeal appeal);

    Optional<InspAppeal> findById(Long id);

    Optional<InspAppeal> findByAppealCode(String appealCode);

    List<InspAppeal> findBySubmitterUserId(Long submitterUserId);

    List<InspAppeal> findByStatus(AppealStatus status);

    List<InspAppeal> findBySubmissionDetailId(Long submissionDetailId);

    List<InspAppeal> findByProjectId(Long projectId);
}
