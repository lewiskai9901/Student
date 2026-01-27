package com.school.management.infrastructure.persistence.schedule;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.schedule.model.*;
import com.school.management.domain.schedule.repository.SchedulePolicyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MyBatis Plus implementation of SchedulePolicyRepository.
 */
@Slf4j
@Repository
public class SchedulePolicyRepositoryImpl implements SchedulePolicyRepository {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final SchedulePolicyMapper policyMapper;

    public SchedulePolicyRepositoryImpl(SchedulePolicyMapper policyMapper) {
        this.policyMapper = policyMapper;
    }

    @Override
    public SchedulePolicy save(SchedulePolicy policy) {
        SchedulePolicyPO po = toPO(policy);

        if (policy.getId() == null) {
            policyMapper.insert(po);
            policy.setId(po.getId());
        } else {
            policyMapper.updateById(po);
        }

        return policy;
    }

    @Override
    public Optional<SchedulePolicy> findById(Long id) {
        SchedulePolicyPO po = policyMapper.selectById(id);
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(po));
    }

    @Override
    public Optional<SchedulePolicy> findByPolicyCode(String policyCode) {
        SchedulePolicyPO po = policyMapper.findByPolicyCode(policyCode);
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(po));
    }

    @Override
    public List<SchedulePolicy> findEnabled() {
        return policyMapper.findEnabled().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<SchedulePolicy> findByTemplateId(Long templateId) {
        return policyMapper.findByTemplateId(templateId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<SchedulePolicy> findAll() {
        return policyMapper.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(SchedulePolicy policy) {
        if (policy != null && policy.getId() != null) {
            policyMapper.deleteById(policy.getId());
        }
    }

    // ==================== Mapping Methods ====================

    private SchedulePolicyPO toPO(SchedulePolicy domain) {
        SchedulePolicyPO po = new SchedulePolicyPO();
        po.setId(domain.getId());
        po.setPolicyCode(domain.getPolicyCode());
        po.setPolicyName(domain.getPolicyName());
        po.setPolicyType(domain.getPolicyType() != null ? domain.getPolicyType().name() : null);
        po.setRotationAlgorithm(domain.getRotationAlgorithm() != null ? domain.getRotationAlgorithm().name() : null);
        po.setTemplateId(domain.getTemplateId());
        po.setInspectorPool(toJson(domain.getInspectorPool()));
        po.setScheduleConfig(domain.getScheduleConfig());
        po.setExcludedDates(toJson(domain.getExcludedDates()));
        po.setIsEnabled(domain.isEnabled());
        po.setCreatedBy(domain.getCreatedBy());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }

    private SchedulePolicy toDomain(SchedulePolicyPO po) {
        return SchedulePolicy.reconstruct()
                .id(po.getId())
                .policyCode(po.getPolicyCode())
                .policyName(po.getPolicyName())
                .policyType(po.getPolicyType() != null ? PolicyType.valueOf(po.getPolicyType()) : null)
                .rotationAlgorithm(po.getRotationAlgorithm() != null ? RotationAlgorithm.valueOf(po.getRotationAlgorithm()) : null)
                .templateId(po.getTemplateId())
                .inspectorPool(parseJsonLongList(po.getInspectorPool()))
                .scheduleConfig(po.getScheduleConfig())
                .excludedDates(parseJsonStringList(po.getExcludedDates()))
                .isEnabled(po.getIsEnabled() != null && po.getIsEnabled())
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    // ==================== JSON Helpers ====================

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return JSON.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize value to JSON: {}", e.getMessage());
            return null;
        }
    }

    private List<Long> parseJsonLongList(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return JSON.readValue(json, new TypeReference<List<Long>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse JSON Long list: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<String> parseJsonStringList(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return JSON.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse JSON String list: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
