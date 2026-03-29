package com.school.management.domain.teaching.repository;

import com.school.management.domain.teaching.model.teachingclass.TeachingClass;
import java.util.List;
import java.util.Optional;

public interface TeachingClassRepository {
    TeachingClass save(TeachingClass teachingClass);
    Optional<TeachingClass> findById(Long id);
    List<TeachingClass> findBySemesterId(Long semesterId);
    List<TeachingClass> findBySemesterIdAndCourseId(Long semesterId, Long courseId);
    void deleteById(Long id);
}
