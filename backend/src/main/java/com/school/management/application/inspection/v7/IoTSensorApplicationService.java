package com.school.management.application.inspection.v7;

import com.school.management.domain.inspection.model.v7.platform.IoTSensor;
import com.school.management.domain.inspection.model.v7.platform.ItemSensorBinding;
import com.school.management.domain.inspection.model.v7.platform.SensorReading;
import com.school.management.domain.inspection.repository.v7.IoTSensorRepository;
import com.school.management.domain.inspection.repository.v7.ItemSensorBindingRepository;
import com.school.management.domain.inspection.repository.v7.SensorReadingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class IoTSensorApplicationService {

    private final IoTSensorRepository sensorRepository;
    private final SensorReadingRepository readingRepository;
    private final ItemSensorBindingRepository bindingRepository;

    // ========== Sensor CRUD ==========

    @Transactional
    public IoTSensor registerSensor(String sensorCode, String sensorName, String sensorType,
                                     String locationName, Long placeId, String mqttTopic, String dataUnit) {
        sensorRepository.findBySensorCode(sensorCode).ifPresent(existing -> {
            throw new IllegalArgumentException("传感器编码已存在: " + sensorCode);
        });
        IoTSensor sensor = IoTSensor.create(sensorCode, sensorName, sensorType, locationName, placeId, mqttTopic, dataUnit);
        IoTSensor saved = sensorRepository.save(sensor);
        log.info("Registered IoT sensor: code={}, name={}", sensorCode, sensorName);
        return saved;
    }

    @Transactional
    public IoTSensor updateSensor(Long id, String sensorName, String sensorType,
                                   String locationName, Long placeId, String mqttTopic, String dataUnit) {
        IoTSensor sensor = sensorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("传感器不存在: " + id));
        sensor.updateInfo(sensorName, sensorType, locationName, placeId, mqttTopic, dataUnit);
        return sensorRepository.save(sensor);
    }

    @Transactional
    public void activateSensor(Long id) {
        IoTSensor sensor = sensorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("传感器不存在: " + id));
        sensor.activate();
        sensorRepository.save(sensor);
    }

    @Transactional
    public void deactivateSensor(Long id) {
        IoTSensor sensor = sensorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("传感器不存在: " + id));
        sensor.deactivate();
        sensorRepository.save(sensor);
    }

    @Transactional(readOnly = true)
    public List<IoTSensor> getAllSensors() {
        return sensorRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<IoTSensor> getActiveSensors() {
        return sensorRepository.findActive();
    }

    @Transactional(readOnly = true)
    public IoTSensor getSensor(Long id) {
        return sensorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("传感器不存在: " + id));
    }

    @Transactional(readOnly = true)
    public List<IoTSensor> getSensorsByPlace(Long placeId) {
        return sensorRepository.findByPlaceId(placeId);
    }

    @Transactional
    public void deleteSensor(Long id) {
        sensorRepository.deleteById(id);
        log.info("Deleted IoT sensor: id={}", id);
    }

    // ========== Sensor Readings ==========

    @Transactional
    public SensorReading recordReading(Long sensorId, BigDecimal value, String unit) {
        IoTSensor sensor = sensorRepository.findById(sensorId)
                .orElseThrow(() -> new IllegalArgumentException("传感器不存在: " + sensorId));
        sensor.updateReading(value);
        sensorRepository.save(sensor);

        SensorReading reading = SensorReading.create(sensorId, value, unit != null ? unit : sensor.getDataUnit());
        return readingRepository.save(reading);
    }

    @Transactional(readOnly = true)
    public List<SensorReading> getReadings(Long sensorId, int limit) {
        return readingRepository.findBySensorId(sensorId, limit);
    }

    @Transactional(readOnly = true)
    public List<SensorReading> getReadingsBetween(Long sensorId, LocalDateTime from, LocalDateTime to) {
        return readingRepository.findBySensorIdBetween(sensorId, from, to);
    }

    // ========== Bindings ==========

    @Transactional
    public ItemSensorBinding createBinding(Long templateItemId, Long sensorId,
                                            Boolean autoFill, Boolean autoScore, String scoringThresholds) {
        ItemSensorBinding binding = ItemSensorBinding.create(templateItemId, sensorId, autoFill, autoScore, scoringThresholds);
        return bindingRepository.save(binding);
    }

    @Transactional(readOnly = true)
    public List<ItemSensorBinding> getBindingsByItem(Long templateItemId) {
        return bindingRepository.findByTemplateItemId(templateItemId);
    }

    @Transactional(readOnly = true)
    public List<ItemSensorBinding> getBindingsBySensor(Long sensorId) {
        return bindingRepository.findBySensorId(sensorId);
    }

    @Transactional
    public void deleteBinding(Long id) {
        bindingRepository.deleteById(id);
    }
}
