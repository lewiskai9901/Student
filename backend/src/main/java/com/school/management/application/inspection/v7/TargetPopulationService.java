package com.school.management.application.inspection.v7;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.inspection.model.v7.execution.ScopeType;
import com.school.management.domain.inspection.model.v7.execution.TargetType;
import com.school.management.domain.organization.model.OrgUnit;
import com.school.management.domain.organization.repository.OrgUnitRepository;
import com.school.management.domain.place.model.aggregate.Place;
import com.school.management.domain.place.model.entity.PlaceOccupant;
import com.school.management.domain.place.repository.PlaceOccupantRepository;
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
    private final PlaceOccupantRepository placeOccupantRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * 解析 scopeConfig 中的 ID 列表
     * 支持两种格式：
     * - 数组: [1, 2, 3]
     * - 对象: {"orgUnitIds": ["1", "2"]} 或 {"placeIds": [...]} 或 {"userIds": [...]}
     */
    private List<Long> parseScopeIds(String scopeConfig) {
        if (scopeConfig == null || scopeConfig.isBlank()) {
            return Collections.emptyList();
        }
        try {
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(scopeConfig);
            if (root.isArray()) {
                // 数组格式: [1, 2, 3]
                List<Long> ids = new ArrayList<>();
                for (com.fasterxml.jackson.databind.JsonNode n : root) {
                    ids.add(n.asLong());
                }
                return ids;
            }
            if (root.isObject()) {
                // 对象格式: {"orgUnitIds": [...]} 或 {"placeIds": [...]} 等
                for (String key : List.of("orgUnitIds", "placeIds", "userIds", "ids")) {
                    if (root.has(key) && root.get(key).isArray()) {
                        List<Long> ids = new ArrayList<>();
                        for (com.fasterxml.jackson.databind.JsonNode n : root.get(key)) {
                            ids.add(Long.parseLong(n.asText()));
                        }
                        return ids;
                    }
                }
            }
            return Collections.emptyList();
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
     * 目标类型=ORG：范围内的组织单元及其后代即为目标
     * 目标名称包含父组织上下文（"父名-子名"格式），便于区分同名组织
     */
    private List<TargetInfo> resolveOrgTargets(ScopeType scopeType, List<Long> scopeIds) {
        Set<Long> visited = new LinkedHashSet<>();
        List<TargetInfo> result = new ArrayList<>();

        for (Long orgId : scopeIds) {
            collectOrgAndDescendants(orgId, visited, result);
        }
        return result;
    }

    private void collectOrgAndDescendants(Long orgId, Set<Long> visited, List<TargetInfo> result) {
        if (orgId == null || visited.contains(orgId)) return;
        visited.add(orgId);

        orgUnitRepository.findById(orgId).ifPresent(org -> {
            String displayName = buildOrgDisplayName(org);
            result.add(new TargetInfo(org.getId(), displayName, org.getParentId()));
            // 递归查找子节点
            List<OrgUnit> children = orgUnitRepository.findByParentId(orgId);
            for (OrgUnit child : children) {
                collectOrgAndDescendants(child.getId(), visited, result);
            }
        });
    }

    /**
     * 构建带父组织上下文的显示名称（"父名-子名"格式）
     * 便于区分不同年级下的同名班级
     */
    private String buildOrgDisplayName(OrgUnit org) {
        if (org.getParentId() == null) {
            return org.getUnitName();
        }
        return orgUnitRepository.findById(org.getParentId())
                .map(parent -> parent.getUnitName() + "-" + org.getUnitName())
                .orElse(org.getUnitName());
    }

    /**
     * 过滤目标列表，仅保留叶子组织（没有子节点的组织）
     */
    public List<TargetInfo> filterLeafOrgs(List<TargetInfo> targets) {
        if (targets == null || targets.isEmpty()) {
            return targets;
        }
        return targets.stream()
                .filter(t -> orgUnitRepository.countByParentId(t.getId()) == 0)
                .collect(Collectors.toList());
    }

    /**
     * 目标类型=PLACE：根据范围的组织单元（含后代）找所属场所
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

        // ORG 范围 → 先展开所有后代组织 → 再查每个组织的场所
        Set<Long> allOrgIds = new LinkedHashSet<>();
        for (Long orgId : scopeIds) {
            collectOrgIds(orgId, allOrgIds);
        }

        List<TargetInfo> targets = new ArrayList<>();
        log.info("resolvePlaceTargets: {} org IDs expanded from scope", allOrgIds.size());
        for (Long orgId : allOrgIds) {
            List<Place> places = placeRepository.findByOrgUnitId(orgId);
            if (!places.isEmpty()) {
                log.info("  orgId={} → {} places", orgId, places.size());
            }
            for (Place p : places) {
                targets.add(new TargetInfo(p.getId(), p.getPlaceName(), p.getOrgUnitId()));
            }
        }
        log.info("resolvePlaceTargets: total {} place targets", targets.size());
        return targets;
    }

    private void collectOrgIds(Long orgId, Set<Long> collected) {
        if (orgId == null || collected.contains(orgId)) return;
        collected.add(orgId);
        List<OrgUnit> children = orgUnitRepository.findByParentId(orgId);
        for (OrgUnit child : children) {
            collectOrgIds(child.getId(), collected);
        }
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
     * 根据目标类型和目标ID解析关联人员列表
     * 用于 PERSON_SCORE 字段类型：获取某个检查目标关联的人员
     *
     * @param targetType 目标类型：PLACE 或 ORG
     * @param targetId   目标ID
     * @return 人员列表
     */
    public List<PersonInfo> resolvePersonsForTarget(String targetType, Long targetId) {
        if ("PLACE".equals(targetType)) {
            // 获取场所的在住占用者
            return placeOccupantRepository.findActiveByPlaceId(targetId).stream()
                    .map(o -> new PersonInfo(o.getOccupantId(), o.getOccupantName(), null))
                    .collect(Collectors.toList());
        } else if ("ORG".equals(targetType)) {
            // 获取组织单元下的用户
            return userRepository.findByOrgUnitIdIn(List.of(targetId)).stream()
                    .map(u -> new PersonInfo(u.getId(), u.getRealName(), u.getOrgUnitId()))
                    .collect(Collectors.toList());
        }
        log.warn("不支持的目标类型: {}", targetType);
        return Collections.emptyList();
    }

    // ==================== Design B: 层级目标派生 ====================

    /**
     * Design B: 根据分区的目标配置，从父目标列表派生子目标
     *
     * @param parentTargets    父分区的目标列表（可以是 ORG/PLACE/USER）
     * @param parentTargetType 父目标的实体类型: ORG/PLACE/USER
     * @param targetEntityType 要派生的实体类型: ORG/PLACE/USER
     * @param targetTypeFilter 类型过滤（如 "org_type=班级", "place_category=宿舍", "user_type=学生"）
     * @return 派生的目标列表（已去重）
     */
    public List<TargetInfo> deriveFromParentTargets(
            List<TargetInfo> parentTargets,
            String parentTargetType,
            String targetEntityType,
            String targetTypeFilter) {

        if (parentTargets == null || parentTargets.isEmpty()) {
            return Collections.emptyList();
        }

        List<TargetInfo> result = new ArrayList<>();

        for (TargetInfo parent : parentTargets) {
            List<TargetInfo> derived = deriveForSingleTarget(
                    parent.getId(), parentTargetType, targetEntityType, targetTypeFilter);
            result.addAll(derived);
        }

        // 按 id 去重，保留第一个
        return result.stream()
                .collect(Collectors.toMap(TargetInfo::getId, t -> t, (a, b) -> a, LinkedHashMap::new))
                .values().stream()
                .collect(Collectors.toList());
    }

    /**
     * 对单个父目标派生子目标
     */
    private List<TargetInfo> deriveForSingleTarget(
            Long parentId, String parentType, String targetEntityType, String filter) {

        if (parentId == null || parentType == null || targetEntityType == null) {
            return Collections.emptyList();
        }

        if ("ORG".equals(targetEntityType)) {
            return deriveOrgTargets(parentId, parentType, filter);
        } else if ("PLACE".equals(targetEntityType)) {
            return derivePlaceTargets(parentId, parentType, filter);
        } else if ("USER".equals(targetEntityType)) {
            return deriveUserTargets(parentId, parentType, filter);
        }

        log.warn("deriveForSingleTarget: 不支持的目标实体类型 {}", targetEntityType);
        return Collections.emptyList();
    }

    /**
     * 派生 ORG 目标：
     * - 父=ORG → 查子组织
     * - 父=PLACE → 查场所归属的组织
     */
    private List<TargetInfo> deriveOrgTargets(Long parentId, String parentType, String filter) {
        if ("ORG".equals(parentType)) {
            List<OrgUnit> children = orgUnitRepository.findByParentId(parentId);
            return applyOrgFilter(children, filter);
        } else if ("PLACE".equals(parentType)) {
            return placeRepository.findById(parentId)
                    .filter(p -> p.getOrgUnitId() != null)
                    .flatMap(p -> orgUnitRepository.findById(p.getOrgUnitId()))
                    .map(o -> List.of(new TargetInfo(o.getId(), buildOrgDisplayName(o), o.getParentId())))
                    .orElse(Collections.emptyList());
        }
        log.warn("deriveOrgTargets: 不支持从 {} 派生 ORG", parentType);
        return Collections.emptyList();
    }

    /**
     * 派生 PLACE 目标：
     * - 父=ORG → 查组织下的场所
     * - 父=PLACE → 查子场所
     */
    private List<TargetInfo> derivePlaceTargets(Long parentId, String parentType, String filter) {
        if ("ORG".equals(parentType)) {
            List<Place> places = placeRepository.findByOrgUnitId(parentId);
            return applyPlaceFilter(places, filter);
        } else if ("PLACE".equals(parentType)) {
            List<Place> children = placeRepository.findChildren(parentId);
            return applyPlaceFilter(children, filter);
        }
        log.warn("derivePlaceTargets: 不支持从 {} 派生 PLACE", parentType);
        return Collections.emptyList();
    }

    /**
     * 派生 USER 目标：
     * - 父=ORG → 查组织下的用户
     * - 父=PLACE → 查场所的在住占用者
     */
    private List<TargetInfo> deriveUserTargets(Long parentId, String parentType, String filter) {
        if ("ORG".equals(parentType)) {
            List<User> users = userRepository.findByOrgUnitIdIn(List.of(parentId));
            return applyUserFilter(users, filter);
        } else if ("PLACE".equals(parentType)) {
            List<PlaceOccupant> occupants = placeOccupantRepository.findActiveByPlaceId(parentId);
            return applyOccupantFilter(occupants, filter);
        }
        log.warn("deriveUserTargets: 不支持从 {} 派生 USER", parentType);
        return Collections.emptyList();
    }

    // ==================== 过滤方法 ====================

    /**
     * 解析过滤字符串为类型代码集合
     * 支持两种格式：
     * - 新格式: "code1 && code2"（类型代码用 && 连接）
     * - 旧格式: "key=value"（兼容）
     */
    private Set<String> parseFilterCodes(String filter) {
        if (filter == null || filter.isBlank()) return Collections.emptySet();
        if (filter.contains("&&")) {
            return java.util.Arrays.stream(filter.split("&&"))
                    .map(String::trim).filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
        }
        // 兼容旧格式 key=value
        String[] parts = filter.split("=", 2);
        if (parts.length == 2) return Set.of(parts[1].trim());
        return Set.of(filter.trim());
    }

    /**
     * 对组织列表应用过滤条件
     * 支持多类型 AND: 只保留 unitType 在过滤集合中的
     */
    private List<TargetInfo> applyOrgFilter(List<OrgUnit> orgs, String filter) {
        Set<String> codes = parseFilterCodes(filter);
        if (!codes.isEmpty()) {
            orgs = orgs.stream()
                    .filter(o -> o.getUnitType() != null && codes.contains(o.getUnitType()))
                    .collect(Collectors.toList());
        }
        return orgs.stream()
                .map(o -> new TargetInfo(o.getId(), buildOrgDisplayName(o), o.getParentId()))
                .collect(Collectors.toList());
    }

    /**
     * 对场所列表应用过滤条件
     */
    private List<TargetInfo> applyPlaceFilter(List<Place> places, String filter) {
        Set<String> codes = parseFilterCodes(filter);
        if (!codes.isEmpty()) {
            places = places.stream()
                    .filter(p -> {
                        // 匹配 categoryId 或 placeType
                        if (p.getCategoryId() != null && codes.contains(String.valueOf(p.getCategoryId()))) return true;
                        if (p.getPlaceType() != null && codes.contains(p.getPlaceType().name())) return true;
                        return false;
                    })
                    .collect(Collectors.toList());
        }
        return places.stream()
                .map(p -> new TargetInfo(p.getId(), p.getPlaceName(), p.getOrgUnitId()))
                .collect(Collectors.toList());
    }

    /**
     * 对用户列表应用过滤条件
     */
    private List<TargetInfo> applyUserFilter(List<User> users, String filter) {
        Set<String> codes = parseFilterCodes(filter);
        if (!codes.isEmpty()) {
            users = users.stream()
                    .filter(u -> u.getUserTypeCode() != null && codes.contains(u.getUserTypeCode()))
                    .collect(Collectors.toList());
        }
        return users.stream()
                .map(u -> new TargetInfo(u.getId(), u.getRealName(), u.getOrgUnitId()))
                .collect(Collectors.toList());
    }

    /**
     * 对占用者列表应用过滤条件（占用者本身就是用户/学生）
     * 目前不做额外过滤，直接映射为 TargetInfo
     */
    private List<TargetInfo> applyOccupantFilter(List<PlaceOccupant> occupants, String filter) {
        return occupants.stream()
                .map(o -> new TargetInfo(o.getOccupantId(), o.getOccupantName(), null))
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

    /**
     * 人员信息（用于 PERSON_SCORE 字段类型）
     */
    public static class PersonInfo {
        private final Long id;
        private final String name;
        private final Long orgUnitId;

        public PersonInfo(Long id, String name, Long orgUnitId) {
            this.id = id;
            this.name = name;
            this.orgUnitId = orgUnitId;
        }

        public Long getId() { return id; }
        public String getName() { return name; }
        public Long getOrgUnitId() { return orgUnitId; }
    }
}
