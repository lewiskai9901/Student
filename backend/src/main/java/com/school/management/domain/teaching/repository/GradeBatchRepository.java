package com.school.management.domain.teaching.repository;

import com.school.management.domain.teaching.model.grade.GradeBatch;
import java.util.List;
import java.util.Optional;

public interface GradeBatchRepository {
    GradeBatch save(GradeBatch batch);
    Optional<GradeBatch> findById(Long id);
    List<GradeBatch> findBySemester(Long semesterId, Integer gradeType, Integer status, int pageNum, int pageSize);
    long count(Long semesterId, Integer gradeType, Integer status);
    void deleteById(Long id);
}
