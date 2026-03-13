package com.school.management.infrastructure.persistence.inspection.v7.platform;

import com.school.management.domain.inspection.model.v7.platform.ItemSensorBinding;
import com.school.management.domain.inspection.repository.v7.ItemSensorBindingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ItemSensorBindingRepositoryImpl implements ItemSensorBindingRepository {

    private final ItemSensorBindingMapper mapper;

    public ItemSensorBindingRepositoryImpl(ItemSensorBindingMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ItemSensorBinding save(ItemSensorBinding binding) {
        ItemSensorBindingPO po = toPO(binding);
        if (binding.getId() == null) {
            mapper.insert(po);
            binding.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return binding;
    }

    @Override
    public Optional<ItemSensorBinding> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<ItemSensorBinding> findByTemplateItemId(Long templateItemId) {
        return mapper.findByTemplateItemId(templateItemId).stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<ItemSensorBinding> findBySensorId(Long sensorId) {
        return mapper.findBySensorId(sensorId).stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private ItemSensorBindingPO toPO(ItemSensorBinding d) {
        ItemSensorBindingPO po = new ItemSensorBindingPO();
        po.setId(d.getId());
        po.setTenantId(d.getTenantId() != null ? d.getTenantId() : 0L);
        po.setTemplateItemId(d.getTemplateItemId());
        po.setSensorId(d.getSensorId());
        po.setAutoFill(d.getAutoFill());
        po.setAutoScore(d.getAutoScore());
        po.setScoringThresholds(d.getScoringThresholds());
        po.setCreatedAt(d.getCreatedAt());
        return po;
    }

    private ItemSensorBinding toDomain(ItemSensorBindingPO po) {
        return ItemSensorBinding.reconstruct(ItemSensorBinding.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .templateItemId(po.getTemplateItemId())
                .sensorId(po.getSensorId())
                .autoFill(po.getAutoFill())
                .autoScore(po.getAutoScore())
                .scoringThresholds(po.getScoringThresholds())
                .createdAt(po.getCreatedAt()));
    }
}
