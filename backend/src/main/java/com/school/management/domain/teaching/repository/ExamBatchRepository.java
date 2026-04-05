package com.school.management.domain.teaching.repository;

import com.school.management.domain.teaching.model.exam.ExamBatch;
import java.util.List;
import java.util.Optional;

public interface ExamBatchRepository {
    ExamBatch save(ExamBatch batch);
    Optional<ExamBatch> findById(Long id);
    List<ExamBatch> findBySemester(Long semesterId, Integer examType, Integer status, int pageNum, int pageSize);
    long count(Long semesterId, Integer examType, Integer status);
    void deleteById(Long id);
}
