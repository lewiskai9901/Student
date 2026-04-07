package com.school.management.domain.teaching.repository;

import com.school.management.domain.teaching.model.offering.ClassCourseAssignment;
import java.util.List;
import java.util.Optional;

public interface ClassCourseAssignmentRepository {
    ClassCourseAssignment save(ClassCourseAssignment assignment);
    Optional<ClassCourseAssignment> findById(Long id);
    List<ClassCourseAssignment> findBySemesterIdAndClassId(Long semesterId, Long orgUnitId);
    List<ClassCourseAssignment> findBySemesterId(Long semesterId);
    List<ClassCourseAssignment> findByOfferingId(Long offeringId);
    void deleteById(Long id);
}
