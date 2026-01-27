package com.school.management.domain.schedule.service;

import com.school.management.domain.schedule.model.SchedulePolicy;

import java.time.LocalDate;
import java.util.List;

public interface InspectorRotationService {
    List<Long> selectInspectors(SchedulePolicy policy, LocalDate date);
}
