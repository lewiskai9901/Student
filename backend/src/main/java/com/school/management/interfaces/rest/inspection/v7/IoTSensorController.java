package com.school.management.interfaces.rest.inspection.v7;

import com.school.management.application.inspection.v7.IoTSensorApplicationService;
import com.school.management.common.result.Result;
import com.school.management.domain.inspection.model.v7.platform.IoTSensor;
import com.school.management.domain.inspection.model.v7.platform.ItemSensorBinding;
import com.school.management.domain.inspection.model.v7.platform.SensorReading;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v7/insp/iot-sensors")
public class IoTSensorController {

    private final IoTSensorApplicationService service;

    // ========== Sensor CRUD ==========

    @GetMapping
    public Result<List<IoTSensor>> list(@RequestParam(required = false) Boolean activeOnly) {
        if (Boolean.TRUE.equals(activeOnly)) {
            return Result.success(service.getActiveSensors());
        }
        return Result.success(service.getAllSensors());
    }

    @GetMapping("/{id}")
    public Result<IoTSensor> get(@PathVariable Long id) {
        return Result.success(service.getSensor(id));
    }

    @GetMapping("/by-place/{placeId}")
    public Result<List<IoTSensor>> getByPlace(@PathVariable Long placeId) {
        return Result.success(service.getSensorsByPlace(placeId));
    }

    @PostMapping
    public Result<IoTSensor> create(@RequestBody Map<String, Object> body) {
        return Result.success(service.registerSensor(
                (String) body.get("sensorCode"),
                (String) body.get("sensorName"),
                (String) body.get("sensorType"),
                (String) body.get("locationName"),
                body.get("placeId") != null ? Long.valueOf(body.get("placeId").toString()) : null,
                (String) body.get("mqttTopic"),
                (String) body.get("dataUnit")
        ));
    }

    @PutMapping("/{id}")
    public Result<IoTSensor> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return Result.success(service.updateSensor(
                id,
                (String) body.get("sensorName"),
                (String) body.get("sensorType"),
                (String) body.get("locationName"),
                body.get("placeId") != null ? Long.valueOf(body.get("placeId").toString()) : null,
                (String) body.get("mqttTopic"),
                (String) body.get("dataUnit")
        ));
    }

    @PutMapping("/{id}/activate")
    public Result<Void> activate(@PathVariable Long id) {
        service.activateSensor(id);
        return Result.success(null);
    }

    @PutMapping("/{id}/deactivate")
    public Result<Void> deactivate(@PathVariable Long id) {
        service.deactivateSensor(id);
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.deleteSensor(id);
        return Result.success(null);
    }

    // ========== Readings ==========

    @GetMapping("/{sensorId}/readings")
    public Result<List<SensorReading>> getReadings(
            @PathVariable Long sensorId,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        if (from != null && to != null) {
            return Result.success(service.getReadingsBetween(sensorId, from, to));
        }
        return Result.success(service.getReadings(sensorId, limit));
    }

    @PostMapping("/{sensorId}/readings")
    public Result<SensorReading> recordReading(@PathVariable Long sensorId, @RequestBody Map<String, Object> body) {
        BigDecimal value = new BigDecimal(body.get("readingValue").toString());
        String unit = (String) body.get("readingUnit");
        return Result.success(service.recordReading(sensorId, value, unit));
    }

    // ========== Bindings ==========

    @GetMapping("/bindings")
    public Result<List<ItemSensorBinding>> getBindings(
            @RequestParam(required = false) Long templateItemId,
            @RequestParam(required = false) Long sensorId) {
        if (templateItemId != null) {
            return Result.success(service.getBindingsByItem(templateItemId));
        } else if (sensorId != null) {
            return Result.success(service.getBindingsBySensor(sensorId));
        }
        return Result.success(List.of());
    }

    @PostMapping("/bindings")
    public Result<ItemSensorBinding> createBinding(@RequestBody Map<String, Object> body) {
        return Result.success(service.createBinding(
                Long.valueOf(body.get("templateItemId").toString()),
                Long.valueOf(body.get("sensorId").toString()),
                body.get("autoFill") != null ? (Boolean) body.get("autoFill") : true,
                body.get("autoScore") != null ? (Boolean) body.get("autoScore") : false,
                (String) body.get("scoringThresholds")
        ));
    }

    @DeleteMapping("/bindings/{id}")
    public Result<Void> deleteBinding(@PathVariable Long id) {
        service.deleteBinding(id);
        return Result.success(null);
    }
}
