package com.school.management.infrastructure.activity.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.infrastructure.activity.ActivityEventDTO;
import com.school.management.infrastructure.activity.ActivityEventQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 活动事件查询实现
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ActivityEventRepositoryImpl {

    private final ActivityEventMapper activityEventMapper;
    private final ObjectMapper objectMapper;

    /**
     * 分页查询
     */
    public IPage<ActivityEventDTO> queryPage(ActivityEventQuery query) {
        Page<ActivityEventPO> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<ActivityEventPO> wrapper = buildWrapper(query);
        wrapper.orderByDesc(ActivityEventPO::getOccurredAt);

        IPage<ActivityEventPO> poPage = activityEventMapper.selectPage(page, wrapper);

        return poPage.convert(this::toDTO);
    }

    /**
     * 查询资源的活动历史
     */
    public List<ActivityEventDTO> findByResource(String resourceType, String resourceId, int limit) {
        LambdaQueryWrapper<ActivityEventPO> wrapper = new LambdaQueryWrapper<ActivityEventPO>()
                .eq(ActivityEventPO::getResourceType, resourceType)
                .eq(ActivityEventPO::getResourceId, resourceId)
                .orderByDesc(ActivityEventPO::getOccurredAt)
                .last("LIMIT " + limit);

        return activityEventMapper.selectList(wrapper).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 查询用户的活动历史
     */
    public List<ActivityEventDTO> findByUser(Long userId, int limit) {
        LambdaQueryWrapper<ActivityEventPO> wrapper = new LambdaQueryWrapper<ActivityEventPO>()
                .eq(ActivityEventPO::getUserId, userId)
                .orderByDesc(ActivityEventPO::getOccurredAt)
                .last("LIMIT " + limit);

        return activityEventMapper.selectList(wrapper).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 统计
     */
    public Map<String, Object> getStats(String startTime, String endTime) {
        Map<String, Object> stats = new HashMap<>();

        LambdaQueryWrapper<ActivityEventPO> baseWrapper = new LambdaQueryWrapper<>();
        if (startTime != null) {
            baseWrapper.ge(ActivityEventPO::getOccurredAt, LocalDateTime.parse(startTime));
        }
        if (endTime != null) {
            baseWrapper.le(ActivityEventPO::getOccurredAt, LocalDateTime.parse(endTime));
        }

        Long total = activityEventMapper.selectCount(baseWrapper);
        stats.put("total", total);

        LambdaQueryWrapper<ActivityEventPO> successWrapper = new LambdaQueryWrapper<ActivityEventPO>()
                .eq(ActivityEventPO::getResult, "SUCCESS");
        if (startTime != null) successWrapper.ge(ActivityEventPO::getOccurredAt, LocalDateTime.parse(startTime));
        if (endTime != null) successWrapper.le(ActivityEventPO::getOccurredAt, LocalDateTime.parse(endTime));
        stats.put("success", activityEventMapper.selectCount(successWrapper));

        LambdaQueryWrapper<ActivityEventPO> failWrapper = new LambdaQueryWrapper<ActivityEventPO>()
                .eq(ActivityEventPO::getResult, "FAILURE");
        if (startTime != null) failWrapper.ge(ActivityEventPO::getOccurredAt, LocalDateTime.parse(startTime));
        if (endTime != null) failWrapper.le(ActivityEventPO::getOccurredAt, LocalDateTime.parse(endTime));
        stats.put("failed", activityEventMapper.selectCount(failWrapper));

        // 今日
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LambdaQueryWrapper<ActivityEventPO> todayWrapper = new LambdaQueryWrapper<ActivityEventPO>()
                .ge(ActivityEventPO::getOccurredAt, todayStart);
        stats.put("today", activityEventMapper.selectCount(todayWrapper));

        return stats;
    }

    private LambdaQueryWrapper<ActivityEventPO> buildWrapper(ActivityEventQuery query) {
        LambdaQueryWrapper<ActivityEventPO> wrapper = new LambdaQueryWrapper<>();

        if (query.getModule() != null && !query.getModule().isEmpty()) {
            wrapper.eq(ActivityEventPO::getModule, query.getModule());
        }
        if (query.getResourceType() != null && !query.getResourceType().isEmpty()) {
            wrapper.eq(ActivityEventPO::getResourceType, query.getResourceType());
        }
        if (query.getResourceId() != null && !query.getResourceId().isEmpty()) {
            wrapper.eq(ActivityEventPO::getResourceId, query.getResourceId());
        }
        if (query.getAction() != null && !query.getAction().isEmpty()) {
            wrapper.eq(ActivityEventPO::getAction, query.getAction());
        }
        if (query.getUserId() != null) {
            wrapper.eq(ActivityEventPO::getUserId, query.getUserId());
        }
        if (query.getResult() != null && !query.getResult().isEmpty()) {
            wrapper.eq(ActivityEventPO::getResult, query.getResult());
        }
        if (query.getStartTime() != null && !query.getStartTime().isEmpty()) {
            wrapper.ge(ActivityEventPO::getOccurredAt, LocalDateTime.parse(query.getStartTime()));
        }
        if (query.getEndTime() != null && !query.getEndTime().isEmpty()) {
            wrapper.le(ActivityEventPO::getOccurredAt, LocalDateTime.parse(query.getEndTime()));
        }
        if (query.getKeyword() != null && !query.getKeyword().isEmpty()) {
            wrapper.and(w -> w
                    .like(ActivityEventPO::getResourceName, query.getKeyword())
                    .or().like(ActivityEventPO::getActionLabel, query.getKeyword())
                    .or().like(ActivityEventPO::getUserName, query.getKeyword())
            );
        }

        return wrapper;
    }

    private ActivityEventDTO toDTO(ActivityEventPO po) {
        ActivityEventDTO dto = new ActivityEventDTO();
        dto.setId(po.getId() != null ? po.getId().toString() : null);
        dto.setRequestId(po.getRequestId());
        dto.setResourceType(po.getResourceType());
        dto.setResourceId(po.getResourceId());
        dto.setResourceName(po.getResourceName());
        dto.setAction(po.getAction());
        dto.setActionLabel(po.getActionLabel());
        dto.setResult(po.getResult());
        dto.setErrorMessage(po.getErrorMessage());
        dto.setUserId(po.getUserId());
        dto.setUserName(po.getUserName());
        dto.setSourceIp(po.getSourceIp());
        dto.setUserAgent(po.getUserAgent());
        dto.setApiEndpoint(po.getApiEndpoint());
        dto.setHttpMethod(po.getHttpMethod());
        dto.setReason(po.getReason());
        dto.setModule(po.getModule());
        dto.setOccurredAt(po.getOccurredAt());
        dto.setBeforeSnapshot(po.getBeforeSnapshot());
        dto.setAfterSnapshot(po.getAfterSnapshot());

        // Parse JSON fields
        if (po.getChangedFields() != null && !po.getChangedFields().isEmpty()) {
            try {
                dto.setChangedFields(objectMapper.readValue(po.getChangedFields(),
                        new TypeReference<List<ActivityEventDTO.FieldChangeDTO>>() {}));
            } catch (Exception e) {
                log.debug("解析 changedFields 失败: {}", po.getChangedFields());
            }
        }
        if (po.getTags() != null && !po.getTags().isEmpty()) {
            try {
                dto.setTags(objectMapper.readValue(po.getTags(),
                        new TypeReference<Map<String, String>>() {}));
            } catch (Exception e) {
                log.debug("解析 tags 失败: {}", po.getTags());
            }
        }

        return dto;
    }
}
