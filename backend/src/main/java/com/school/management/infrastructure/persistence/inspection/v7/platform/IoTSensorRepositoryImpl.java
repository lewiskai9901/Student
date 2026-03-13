package com.school.management.infrastructure.persistence.inspection.v7.platform;

import com.school.management.domain.inspection.model.v7.platform.IoTSensor;
import com.school.management.domain.inspection.repository.v7.IoTSensorRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class IoTSensorRepositoryImpl implements IoTSensorRepository {

    private final IoTSensorMapper mapper;

    public IoTSensorRepositoryImpl(IoTSensorMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public IoTSensor save(IoTSensor sensor) {
        IoTSensorPO po = toPO(sensor);
        if (sensor.getId() == null) {
            mapper.insert(po);
            sensor.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return sensor;
    }

    @Override
    public Optional<IoTSensor> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public Optional<IoTSensor> findBySensorCode(String sensorCode) {
        return Optional.ofNullable(mapper.findBySensorCode(sensorCode)).map(this::toDomain);
    }

    @Override
    public List<IoTSensor> findAll() {
        return mapper.selectList(null).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<IoTSensor> findActive() {
        return mapper.findActive().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<IoTSensor> findByPlaceId(Long placeId) {
        return mapper.findByPlaceId(placeId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private IoTSensorPO toPO(IoTSensor d) {
        IoTSensorPO po = new IoTSensorPO();
        po.setId(d.getId());
        po.setTenantId(d.getTenantId() != null ? d.getTenantId() : 0L);
        po.setSensorCode(d.getSensorCode());
        po.setSensorName(d.getSensorName());
        po.setSensorType(d.getSensorType());
        po.setLocationName(d.getLocationName());
        po.setPlaceId(d.getPlaceId());
        po.setMqttTopic(d.getMqttTopic());
        po.setDataUnit(d.getDataUnit());
        po.setIsActive(d.getIsActive());
        po.setLastReading(d.getLastReading());
        po.setLastReadingAt(d.getLastReadingAt());
        po.setCreatedAt(d.getCreatedAt());
        return po;
    }

    private IoTSensor toDomain(IoTSensorPO po) {
        return IoTSensor.reconstruct(IoTSensor.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .sensorCode(po.getSensorCode())
                .sensorName(po.getSensorName())
                .sensorType(po.getSensorType())
                .locationName(po.getLocationName())
                .placeId(po.getPlaceId())
                .mqttTopic(po.getMqttTopic())
                .dataUnit(po.getDataUnit())
                .isActive(po.getIsActive())
                .lastReading(po.getLastReading())
                .lastReadingAt(po.getLastReadingAt())
                .createdAt(po.getCreatedAt()));
    }
}
