package com.school.management.application.inspection.v7;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.inspection.model.v7.execution.ScopeType;
import com.school.management.domain.inspection.model.v7.execution.TargetType;
import com.school.management.domain.organization.model.OrgUnit;
import com.school.management.domain.organization.repository.OrgUnitRepository;
import com.school.management.domain.place.model.aggregate.Place;
import com.school.management.domain.place.repository.PlaceRepository;
import com.school.management.domain.user.model.aggregate.User;
import com.school.management.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 目标人口服务
 * 根据 scopeType + scopeConfig + targetType 派生检查目标列表
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class TargetPopulationService {

    private final OrgUnitRepository orgUnitRepository;
    private final PlaceRepository placeRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * 解析 scopeConfig 中的 ID 列表
     */
    private List<Long> parseScopeIds(String scopeConfig) {
        if (scopeConfig == null || scopeConfig.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(scopeConfig, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            log.warn("解析 scopeConfig 失败: {}", scopeConfig, e);
            return Collections.emptyList();
        }
    }

    /**
     * 根据范围和目标类型派生目标列表
     *
     * @return 目标列表，每个 TargetInfo 包含 id + name + orgUnitId
     */
    public List<TargetInfo> resolveTargets(ScopeType scopeType, String scopeConfig, TargetType targetType) {
        List<Long> scopeIds = parseScopeIds(scopeConfig);

        switch (targetType) {
            case ORG:
                return resolveOrgTargets(scopeType, scopeIds);
            case PLACE:
                return resolvePlaceTargets(scopeType, scopeIds);
            case USER:
                return resolveUserTargets(scopeType, scopeIds);
            default:
                log.warn("不支持的目标类型: {}", targetType);
                return Collections.emptyList();
        }
    }

    /**
     * 目标类型=ORG：范围内的组织单元即为目标
     */
    private List<TargetInfo> resolveOrgTargets(ScopeType scopeType, List<Long> scopeIds) {
        Set<OrgUnit> units = new LinkedHashSet<>();

        for (Long orgId : scopeIds) {
            orgUnitRepository.findById(orgId).ifPresent(org -> {
                units.add(org);
                // 包含后代
                if (org.getTreePath() != null) {
                    units.addAll(orgUnitRepository.findDescendants(org.getTreePath()));
                }
            });
        }

        return units.stream()
                .map(u -> new TargetInfo(u.getId(), u.getUnitName(), u.getParentId()))
                .collect(Collectors.toList());
    }

    /**
     * 目标类型=PLACE：根据范围的组织单元找所属场所
     */
    private List<TargetInfo> resolvePlaceTargets(ScopeType scopeType, List<Long> scopeIds) {
        if (scopeType == ScopeType.PLACE) {
            // 直接指定场所 ID
            return scopeIds.stream()
                    .map(placeId -> placeRepository.findById(placeId).orElse(null))
                    .filter(Objects::nonNull)
                    .map(p -> new TargetInfo(p.getId(), p.getPlaceName(), p.getOrgUnitId()))
                    .collect(Collectors.toList());
        }

        // ORG 范围 → 找关联场所
        List<TargetInfo> targets = new ArrayList<>();
        for (Long orgId : scopeIds) {
            List<Place> places = placeRepository.findByOrgUnitId(orgId);
            for (Place p : places) {
                targets.add(new TargetInfo(p.getId(), p.getPlaceName(), p.getOrgUnitId()));
            }
        }
        return targets;
    }

    /**
     * 目标类型=USER：范围内的组织单元下的用户
     */
    private List<TargetInfo> resolveUserTargets(ScopeType scopeType, List<Long> scopeIds) {
        if (scopeIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<User> users = userRepository.findByOrgUnitIdIn(scopeIds);
        return users.stream()
                .map(u -> new TargetInfo(u.getId(), u.getRealName(), u.getOrgUnitId()))
                .collect(Collectors.toList());
    }

    /**
     * 目标信息
     */
    public static class TargetInfo {
        private final Long id;
        private final String name;
        private final Long orgUnitId;

        public TargetInfo(Long id, String name, Long orgUnitId) {
            this.id = id;
            this.name = name;
            this.orgUnitId = orgUnitId;
        }

        public Long getId() { return id; }
        public String getName() { return name; }
        public Long getOrgUnitId() { return orgUnitId; }
    }
}
