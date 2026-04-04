package com.school.management.domain.calendar.repository;

import com.school.management.domain.calendar.model.aggregate.Semester;
import com.school.management.domain.shared.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SemesterRepository extends Repository<Semester, Long> {
    Semester save(Semester semester);
    Optional<Semester> findById(Long id);
    Optional<Semester> findBySemesterCode(String semesterCode);
    Optional<Semester> findCurrentSemester();
    boolean existsBySemesterCode(String semesterCode);
    boolean existsBySemesterCodeAndIdNot(String semesterCode, Long excludeId);
    List<Semester> findByDateRange(LocalDate startDate, LocalDate endDate);
    List<Semester> findByStartYear(Integer startYear);
    List<Semester> findAllActive();
    List<Semester> findAllOrderByStartDateDesc();
    void deleteById(Long id);
    List<Semester> findAll(int page, int size);
    long count();
    void clearAllCurrentFlags();
}
