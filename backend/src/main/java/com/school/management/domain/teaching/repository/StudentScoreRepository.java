package com.school.management.domain.teaching.repository;

import com.school.management.domain.teaching.model.grade.StudentScore;
import java.util.List;
import java.util.Optional;

public interface StudentScoreRepository {
    StudentScore save(StudentScore score);
    Optional<StudentScore> findById(Long id);
    List<StudentScore> findByBatchId(Long batchId);
    List<StudentScore> findByStudentId(Long studentId, Long semesterId, Long courseId);
    List<StudentScore> findByClassId(Long classId);
    void deleteById(Long id);
}
