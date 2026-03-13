package com.school.management.infrastructure.persistence.inspection.v7.platform;

import com.school.management.domain.inspection.model.v7.platform.SensorReading;
import com.school.management.domain.inspection.repository.v7.SensorReadingRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class SensorReadingRepositoryImpl implements SensorReadingRepository {

    private final SensorReadingMapper mapper;

    public SensorReadingRepositoryImpl(SensorReadingMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public SensorReading save(SensorReading reading) {
        SensorReadingPO po = toPO(reading);
        if (reading.getId() == null) {
            mapper.insert(po);
            reading.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return reading;
    }

    @Override
    public List<SensorReading> findBySensorId(Long sensorId, int limit) {
        return mapper.findBySensorId(sensorId, limit).stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<SensorReading> findBySensorIdBetween(Long sensorId, LocalDateTime from, LocalDateTime to) {
        return mapper.findBySensorIdBetween(sensorId, from, to).stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    private SensorReadingPO toPO(SensorReading d) {
        SensorReadingPO po = new SensorReadingPO();
        po.setId(d.getId());
        po.setSensorId(d.getSensorId());
        po.setReadingValue(d.getReadingValue());
        po.setReadingUnit(d.getReadingUnit());
        po.setRecordedAt(d.getRecordedAt());
        return po;
    }

    private SensorReading toDomain(SensorReadingPO po) {
        return SensorReading.reconstruct(SensorReading.builder()
                .id(po.getId())
                .sensorId(po.getSensorId())
                .readingValue(po.getReadingValue())
                .readingUnit(po.getReadingUnit())
                .recordedAt(po.getRecordedAt()));
    }
}
