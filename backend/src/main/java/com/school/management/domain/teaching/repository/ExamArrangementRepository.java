package com.school.management.domain.teaching.repository;

import com.school.management.domain.teaching.model.exam.ExamArrangement;
import java.util.List;
import java.util.Optional;

public interface ExamArrangementRepository {
    ExamArrangement save(ExamArrangement arrangement);
    Optional<ExamArrangement> findById(Long id);
    List<ExamArrangement> findByBatchId(Long batchId);
    void deleteById(Long id);
    void deleteByBatchIdAndId(Long batchId, Long id);
}
