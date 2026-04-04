package com.school.management.domain.calendar.repository;

import com.school.management.domain.calendar.model.entity.AcademicEvent;
import java.util.List;
import java.util.Optional;

public interface AcademicEventRepository {
    AcademicEvent save(AcademicEvent event);
    Optional<AcademicEvent> findById(Long id);
    List<AcademicEvent> findAll(Long yearId, Long semesterId, Integer eventType);
    void deleteById(Long id);
}
