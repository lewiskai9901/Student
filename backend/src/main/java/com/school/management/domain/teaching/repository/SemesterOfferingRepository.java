package com.school.management.domain.teaching.repository;

import com.school.management.domain.teaching.model.offering.SemesterOffering;
import java.util.List;
import java.util.Optional;

public interface SemesterOfferingRepository {
    SemesterOffering save(SemesterOffering offering);
    Optional<SemesterOffering> findById(Long id);
    List<SemesterOffering> findBySemesterId(Long semesterId);
    List<SemesterOffering> findBySemesterIdAndGrade(Long semesterId, String grade);
    void deleteById(Long id);
}
