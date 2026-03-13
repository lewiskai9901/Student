package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.platform.IoTSensor;

import java.util.List;
import java.util.Optional;

public interface IoTSensorRepository {

    IoTSensor save(IoTSensor sensor);

    Optional<IoTSensor> findById(Long id);

    Optional<IoTSensor> findBySensorCode(String sensorCode);

    List<IoTSensor> findAll();

    List<IoTSensor> findActive();

    List<IoTSensor> findByPlaceId(Long placeId);

    void deleteById(Long id);
}
