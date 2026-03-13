package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.platform.ItemSensorBinding;

import java.util.List;
import java.util.Optional;

public interface ItemSensorBindingRepository {

    ItemSensorBinding save(ItemSensorBinding binding);

    Optional<ItemSensorBinding> findById(Long id);

    List<ItemSensorBinding> findByTemplateItemId(Long templateItemId);

    List<ItemSensorBinding> findBySensorId(Long sensorId);

    void deleteById(Long id);
}
