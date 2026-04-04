package com.school.management.domain.calendar.repository;

import com.school.management.domain.calendar.model.aggregate.AcademicYear;
import java.util.List;
import java.util.Optional;

public interface AcademicYearRepository {
    AcademicYear save(AcademicYear year);
    Optional<AcademicYear> findById(Long id);
    Optional<AcademicYear> findCurrent();
    List<AcademicYear> findAllActive();
    void deleteById(Long id);
    void clearAllCurrentFlags();
}
