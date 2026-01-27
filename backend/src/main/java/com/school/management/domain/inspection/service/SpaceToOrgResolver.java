package com.school.management.domain.inspection.service;

import java.util.List;

/**
 * Domain service interface for resolving physical spaces to organizational units.
 * Maps dormitories/classrooms to classes, and students to their class information.
 */
public interface SpaceToOrgResolver {

    /**
     * Resolves a dormitory to the classes that have students living in it.
     * Returns class allocations with student counts and ratios.
     */
    List<ClassAllocation> resolveDormitory(Long dormitoryId);

    /**
     * Resolves a classroom to its assigned class.
     */
    List<ClassAllocation> resolveClassroom(Long classroomId);

    /**
     * Resolves a single student to their organizational info.
     */
    StudentOrgInfo resolveStudent(Long studentId);

    /**
     * Resolves multiple students to their organizational info.
     */
    List<StudentOrgInfo> resolveStudentBatch(List<Long> studentIds);
}
