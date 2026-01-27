package com.school.management.infrastructure.persistence.inspection;

import com.school.management.domain.inspection.service.ClassAllocation;
import com.school.management.domain.inspection.service.SpaceToOrgResolver;
import com.school.management.domain.inspection.service.StudentOrgInfo;
import com.school.management.domain.organization.model.SchoolClass;
import com.school.management.domain.organization.repository.SchoolClassRepository;
import com.school.management.domain.student.model.aggregate.Student;
import com.school.management.domain.student.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Infrastructure implementation of SpaceToOrgResolver.
 * Resolves physical spaces (dormitories/classrooms) to organizational units (classes).
 */
@Service
public class SpaceToOrgResolverImpl implements SpaceToOrgResolver {

    private final StudentRepository studentRepository;
    private final SchoolClassRepository schoolClassRepository;

    public SpaceToOrgResolverImpl(StudentRepository studentRepository,
                                   SchoolClassRepository schoolClassRepository) {
        this.studentRepository = studentRepository;
        this.schoolClassRepository = schoolClassRepository;
    }

    @Override
    public List<ClassAllocation> resolveDormitory(Long dormitoryId) {
        List<Student> students = studentRepository.findByDormitoryId(dormitoryId);
        if (students.isEmpty()) {
            return Collections.emptyList();
        }

        // Group students by classId
        Map<Long, List<Student>> byClass = students.stream()
            .filter(s -> s.getClassId() != null)
            .collect(Collectors.groupingBy(Student::getClassId));

        int totalStudents = students.size();
        List<ClassAllocation> allocations = new ArrayList<>();

        for (Map.Entry<Long, List<Student>> entry : byClass.entrySet()) {
            Long classId = entry.getKey();
            int count = entry.getValue().size();
            BigDecimal ratio = BigDecimal.valueOf(count)
                .divide(BigDecimal.valueOf(totalStudents), 4, RoundingMode.HALF_UP);

            // Fetch class info for orgUnit details
            SchoolClass schoolClass = schoolClassRepository.findById(classId).orElse(null);
            String className = schoolClass != null ? schoolClass.getClassName() : "Unknown";
            Long orgUnitId = schoolClass != null ? schoolClass.getOrgUnitId() : null;
            String orgUnitName = ""; // Will be resolved by caller if needed

            allocations.add(new ClassAllocation(classId, className, orgUnitId, orgUnitName, count, ratio));
        }

        return allocations;
    }

    @Override
    public List<ClassAllocation> resolveClassroom(Long classroomId) {
        // Classroom-to-class resolution depends on the teaching schedule
        // For now, return empty - will be implemented when teaching module is integrated
        return Collections.emptyList();
    }

    @Override
    public StudentOrgInfo resolveStudent(Long studentId) {
        Student student = studentRepository.findById(studentId).orElse(null);
        if (student == null) {
            return null;
        }

        SchoolClass schoolClass = student.getClassId() != null
            ? schoolClassRepository.findById(student.getClassId()).orElse(null)
            : null;

        return new StudentOrgInfo(
            student.getId(),
            student.getName(),
            student.getClassId(),
            schoolClass != null ? schoolClass.getClassName() : null,
            schoolClass != null ? schoolClass.getOrgUnitId() : null,
            null // orgUnitName resolved by caller if needed
        );
    }

    @Override
    public List<StudentOrgInfo> resolveStudentBatch(List<Long> studentIds) {
        return studentIds.stream()
            .map(this::resolveStudent)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
