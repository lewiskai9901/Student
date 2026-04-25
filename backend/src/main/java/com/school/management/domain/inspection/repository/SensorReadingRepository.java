package com.school.management.domain.inspection.repository;

import com.school.management.domain.inspection.model.platform.SensorReading;

import java.time.LocalDateTime;
import java.util.List;

public interface SensorReadingRepository {

    SensorReading save(SensorReading reading);

    List<SensorReading> findBySensorId(Long sensorId, int limit);

    List<SensorReading> findBySensorIdBetween(Long sensorId, LocalDateTime from, LocalDateTime to);
}
