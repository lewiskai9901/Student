package com.school.management.domain.calendar.repository;

import com.school.management.domain.calendar.model.entity.TeachingWeek;
import java.util.List;

public interface TeachingWeekRepository {
    TeachingWeek save(TeachingWeek week);
    List<TeachingWeek> findBySemesterId(Long semesterId);
    void deleteBySemesterId(Long semesterId);
}
