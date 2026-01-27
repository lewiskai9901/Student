package com.school.management.infrastructure.persistence.schedule;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.schedule.model.*;
import com.school.management.domain.schedule.repository.ScheduleExecutionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MyBatis Plus implementation of ScheduleExecutionRepository.
 */
@Slf4j
@Repository
public class ScheduleExecutionRepositoryImpl implements ScheduleExecutionRepository {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final ScheduleExecutionMapper executionMapper;

    public ScheduleExecutionRepositoryImpl(ScheduleExecutionMapper executionMapper) {
        this.executionMapper = executionMapper;
    }

    @Override
    public ScheduleExecution save(ScheduleExecution execution) {
        ScheduleExecutionPO po = toPO(execution);

        if (execution.getId() == null) {
            executionMapper.insert(po);
            execution.setId(po.getId());
        } else {
            executionMapper.updateById(po);
        }

        return execution;
    }

    @Override
    public Optional<ScheduleExecution> findById(Long id) {
        ScheduleExecutionPO po = executionMapper.selectById(id);
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(po));
    }

    @Override
    public List<ScheduleExecution> findByPolicyIdAndDate(Long policyId, LocalDate date) {
        return executionMapper.findByPolicyIdAndDate(policyId, date).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ScheduleExecution> findByDate(LocalDate date) {
        return executionMapper.findByDate(date).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ScheduleExecution> findByStatus(String status) {
        return executionMapper.findByStatus(status).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ScheduleExecution> findByDateRange(LocalDate startDate, LocalDate endDate) {
        return executionMapper.findByDateRange(startDate, endDate).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // ==================== Mapping Methods ====================

    private ScheduleExecutionPO toPO(ScheduleExecution domain) {
        ScheduleExecutionPO po = new ScheduleExecutionPO();
        po.setId(domain.getId());
        po.setPolicyId(domain.getPolicyId());
        po.setExecutionDate(domain.getExecutionDate());
        po.setAssignedInspectors(toJson(domain.getAssignedInspectors()));
        po.setSessionId(domain.getSessionId());
        po.setStatus(domain.getStatus() != null ? domain.getStatus().name() : null);
        po.setFailureReason(domain.getFailureReason());
        po.setCreatedAt(domain.getCreatedAt());
        return po;
    }

    private ScheduleExecution toDomain(ScheduleExecutionPO po) {
        return ScheduleExecution.reconstruct()
                .id(po.getId())
                .policyId(po.getPolicyId())
                .executionDate(po.getExecutionDate())
                .assignedInspectors(parseJsonLongList(po.getAssignedInspectors()))
                .sessionId(po.getSessionId())
                .status(po.getStatus() != null ? ExecutionStatus.valueOf(po.getStatus()) : ExecutionStatus.PLANNED)
                .failureReason(po.getFailureReason())
                .createdAt(po.getCreatedAt())
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
}
