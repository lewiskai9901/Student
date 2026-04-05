package com.school.management.application.place;

import com.school.management.domain.organization.model.OrgUnit;
import com.school.management.domain.organization.repository.OrgUnitRepository;
import com.school.management.domain.place.model.aggregate.UniversalPlace;
import com.school.management.domain.place.model.entity.UniversalPlaceOccupant;
import com.school.management.domain.place.model.entity.UniversalPlaceType;
import com.school.management.domain.place.model.valueobject.PlaceStatus;
import com.school.management.domain.place.repository.UniversalPlaceOccupantRepository;
import com.school.management.domain.place.repository.UniversalPlaceRepository;
import com.school.management.domain.place.repository.UniversalPlaceTypeRepository;
import com.school.management.domain.place.service.PlaceInheritanceService;
import com.school.management.domain.access.model.entity.AccessRelation;
import com.school.management.domain.access.repository.AccessRelationRepository;
import com.school.management.domain.shared.event.DomainEventPublisher;
import com.school.management.domain.shared.model.valueobject.FieldChange;
import com.school.management.domain.user.model.aggregate.User;
import com.school.management.domain.user.model.entity.UserType;
import com.school.management.domain.user.repository.UserRepository;
import com.school.management.domain.user.repository.UserTypeRepository;
import com.school.management.infrastructure.activity.ActivityEventPublisher;
import com.school.management.application.event.TriggerService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 通用空间应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UniversalPlaceApplicationService {

    private final UniversalPlaceRepository placeRepository;
    private final UniversalPlaceTypeRepository placeTypeRepository;
    private final UniversalPlaceOccupantRepository occupantRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final UserRepository userRepository;
    private final UserTypeRepository userTypeRepository;
    private final AccessRelationRepository accessRelationRepository;
    private final DomainEventPublisher eventPublisher;
    private final PlaceInheritanceService inheritanceService;
    private final ActivityEventPublisher activityEventPublisher;

    @Autowired(required = false)
    private TriggerService triggerService;

    // ==================== 查询 ====================

    /**
     * 获取空间树
     */
    public List<PlaceTreeNode> getPlaceTree() {
        List<UniversalPlace> roots = placeRepository.findAllRoots();
        return roots.stream()
                .map(this::buildTreeNode)
                .collect(Collectors.toList());
    }

    /**
     * 获取指定类型的空间树
     */
    public List<PlaceTreeNode> getPlaceTreeByType(String typeCode) {
        List<UniversalPlace> places = placeRepository.findByTypeCode(typeCode);
        List<UniversalPlace> roots = places.stream()
                .filter(s -> s.getParentId() == null)
                .collect(Collectors.toList());
        return roots.stream()
                .map(this::buildTreeNode)
                .collect(Collectors.toList());
    }

    /**
     * 获取子空间
     */
    public List<PlaceDTO> getChildren(Long parentId) {
        List<UniversalPlace> children = placeRepository.findChildren(parentId);
        return children.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取空间
     */
    public PlaceDTO getPlaceById(Long id) {
        return placeRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new IllegalArgumentException("空间不存在: " + id));
    }

    /**
     * 根据编码获取空间
     */
    public PlaceDTO getPlaceByCode(String placeCode) {
        return placeRepository.findByPlaceCode(placeCode)
                .map(this::toDTO)
                .orElseThrow(() -> new IllegalArgumentException("空间不存在: " + placeCode));
    }

    /**
     * 获取允许创建的子类型
     */
    public List<UniversalPlaceType> getAllowedChildTypes(Long parentId) {
        if (parentId == null) {
            // 根空间，返回所有根类型
            return placeTypeRepository.findAllRootTypes();
        }
        UniversalPlace parent = placeRepository.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("父空间不存在"));
        UniversalPlaceType parentType = placeTypeRepository.findByTypeCode(parent.getTypeCode())
                .orElseThrow(() -> new IllegalArgumentException("父空间类型不存在"));
        List<String> allowedCodes = parentType.getAllowedChildTypeCodes();
        if (allowedCodes == null || allowedCodes.isEmpty()) {
            return new ArrayList<>();
        }
        return placeTypeRepository.findByTypeCodes(allowedCodes);
    }

    /**
     * 获取统计数据
     */
    public PlaceStatistics getStatistics() {
        List<UniversalPlace> allPlaces = placeRepository.findAll();

        PlaceStatistics stats = new PlaceStatistics();
        stats.setTotalCount(allPlaces.size());

        // 按类型统计
        Map<String, Long> typeCount = allPlaces.stream()
                .collect(Collectors.groupingBy(UniversalPlace::getTypeCode, Collectors.counting()));
        stats.setCountByType(typeCount);

        // 计算总容量和占用
        int totalCapacity = 0;
        int totalOccupancy = 0;
        for (UniversalPlace place : allPlaces) {
            if (place.getCapacity() != null) {
                totalCapacity += place.getCapacity();
                totalOccupancy += place.getCurrentOccupancy() != null ? place.getCurrentOccupancy() : 0;
            }
        }
        stats.setTotalCapacity(totalCapacity);
        stats.setTotalOccupancy(totalOccupancy);

        // 按状态统计
        Map<Integer, Long> statusCount = allPlaces.stream()
                .collect(Collectors.groupingBy(s -> s.getStatus().getValue(), Collectors.counting()));
        stats.setCountByStatus(statusCount);

        // 按所属组织统计（使用有效组织ID，含继承）
        Map<Long, List<UniversalPlace>> byOrg = new java.util.HashMap<>();
        for (UniversalPlace place : allPlaces) {
            Long effectiveOrgId = inheritanceService.getEffectiveOrgUnitId(place);
            if (effectiveOrgId != null) {
                byOrg.computeIfAbsent(effectiveOrgId, k -> new java.util.ArrayList<>()).add(place);
            }
        }
        List<OrgPlaceStats> orgStatsList = new java.util.ArrayList<>();
        for (Map.Entry<Long, List<UniversalPlace>> entry : byOrg.entrySet()) {
            OrgPlaceStats os = new OrgPlaceStats();
            os.setOrgUnitId(entry.getKey());
            orgUnitRepository.findById(entry.getKey())
                    .ifPresent(org -> os.setOrgUnitName(org.getUnitName()));
            List<UniversalPlace> places = entry.getValue();
            os.setPlaceCount(places.size());
            int cap = 0, occ = 0;
            for (UniversalPlace p : places) {
                if (p.getCapacity() != null) {
                    cap += p.getCapacity();
                    occ += p.getCurrentOccupancy() != null ? p.getCurrentOccupancy() : 0;
                }
            }
            os.setTotalCapacity(cap);
            os.setTotalOccupancy(occ);
            orgStatsList.add(os);
        }
        orgStatsList.sort((a, b) -> b.getPlaceCount() - a.getPlaceCount());
        stats.setStatsByOrg(orgStatsList);

        return stats;
    }

    // ==================== 创建 ====================

    /**
     * 创建空间
     */
    @Transactional
    public PlaceDTO createPlace(CreatePlaceCommand command) {
        // 验证类型
        UniversalPlaceType placeType = placeTypeRepository.findByTypeCode(command.getTypeCode())
                .orElseThrow(() -> new IllegalArgumentException("空间类型不存在: " + command.getTypeCode()));

        if (!placeType.isEnabled()) {
            throw new IllegalArgumentException("空间类型已禁用");
        }

        // 验证编码在同父节点下唯一
        if (command.getParentId() != null) {
            if (placeRepository.countByParentIdAndPlaceCode(command.getParentId(), command.getPlaceCode()) > 0) {
                throw new IllegalArgumentException("同级场所下编码已存在: " + command.getPlaceCode());
            }
        } else {
            if (placeRepository.countRootByPlaceCode(command.getPlaceCode()) > 0) {
                throw new IllegalArgumentException("根场所下编码已存在: " + command.getPlaceCode());
            }
        }

        // 验证父空间
        UniversalPlace parent = null;
        if (command.getParentId() != null) {
            parent = placeRepository.findById(command.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("父空间不存在"));

            // 验证是否允许创建此类型的子空间
            UniversalPlaceType parentType = placeTypeRepository.findByTypeCode(parent.getTypeCode())
                    .orElseThrow(() -> new IllegalArgumentException("父空间类型不存在"));

            List<String> allowedChildren = parentType.getAllowedChildTypeCodes();
            if (allowedChildren == null || !allowedChildren.contains(command.getTypeCode())) {
                throw new IllegalArgumentException("父空间不允许创建此类型的子空间");
            }
        } else {
            // 根空间必须是根类型
            if (!placeType.isRootType()) {
                throw new IllegalArgumentException("此类型不能作为根空间");
            }
        }

        // 创建空间
        UniversalPlace place = UniversalPlace.create(
                command.getPlaceCode(),
                command.getPlaceName(),
                command.getTypeCode(),
                command.getParentId()
        );

        place.setDescription(command.getDescription());
        place.setCapacity(command.getCapacity());
        place.setOrgUnitId(command.getOrgUnitId());
        place.setResponsibleUserId(command.getResponsibleUserId());

        if (command.getAttributes() != null) {
            command.getAttributes().forEach(place::setAttribute);
        }

        // 计算层级信息
        if (parent != null) {
            place.setLevel(parent.getLevel() + 1);
        } else {
            place.setLevel(0);
        }

        // 保存
        UniversalPlace saved = placeRepository.save(place);

        // 更新路径（需要ID）
        if (parent != null) {
            saved.setPath(parent.getPath() + saved.getId() + "/");
        } else {
            saved.setPath("/" + saved.getId() + "/");
        }
        saved = placeRepository.save(saved);

        // 事件在原始对象上（save()会返回新对象，丢失事件）
        publishEvents(place);
        return toDTO(saved);
    }

    // ==================== 更新 ====================

    /**
     * 更新空间
     */
    @Transactional
    public PlaceDTO updatePlace(Long id, UpdatePlaceCommand command) {
        UniversalPlace place = placeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("空间不存在"));

        // 场所类型创建后不可修改
        if (command.getTypeCode() != null && !command.getTypeCode().equals(place.getTypeCode())) {
            throw new IllegalArgumentException("场所类型创建后不可修改");
        }

        String reason = command.getReason();

        // 简单属性直接设置
        if (command.getPlaceName() != null) {
            place.setPlaceName(command.getPlaceName());
        }
        if (command.getDescription() != null) {
            place.setDescription(command.getDescription());
        }
        if (command.getCapacity() != null) {
            place.setCapacity(command.getCapacity());
        }
        if (command.getAttributes() != null) {
            command.getAttributes().forEach(place::setAttribute);
        }

        // 使用领域方法处理组织分配（会发布事件）
        if (Boolean.TRUE.equals(command.getClearOrgOverride())) {
            place.clearOrganizationOverride(reason);
        } else if (command.getOrgUnitId() != null) {
            place.assignOrganization(command.getOrgUnitId(), reason);
        }

        // 使用领域方法处理负责人分配（会发布事件）
        if (command.getResponsibleUserId() != null) {
            place.assignResponsible(command.getResponsibleUserId(), reason);
        }

        // 使用领域方法处理状态变更（会发布事件）
        if (command.getStatus() != null) {
            PlaceStatus newStatus = PlaceStatus.fromValue(command.getStatus());
            place.changeStatus(newStatus, reason);
        }

        // 事件在原始对象上（save()会返回新对象，丢失事件）
        publishEvents(place);
        UniversalPlace saved = placeRepository.save(place);
        return toDTO(saved);
    }

    /**
     * 更改空间状态
     */
    @Transactional
    public PlaceDTO changeStatus(Long id, PlaceStatus status) {
        UniversalPlace place = placeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("空间不存在"));

        place.changeStatus(status, "状态变更");

        // 事件在原始对象上（save()会返回新对象，丢失事件）
        publishEvents(place);
        UniversalPlace saved = placeRepository.save(place);
        return toDTO(saved);
    }

    // ==================== 删除 ====================

    /**
     * 删除空间
     */
    @Transactional
    public void deletePlace(Long id) {
        UniversalPlace place = placeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("空间不存在"));

        // 检查是否有子空间
        int childCount = placeRepository.countChildren(id);
        if (childCount > 0) {
            throw new IllegalArgumentException("空间有子空间，不能删除");
        }

        // 检查是否有活跃占用记录（比 currentOccupancy 字段更可靠）
        List<UniversalPlaceOccupant> activeOccupants = occupantRepository.findActiveByPlaceId(id);
        if (!activeOccupants.isEmpty()) {
            throw new IllegalArgumentException("场所有占用记录，请先清退后再删除");
        }

        // 检查 currentOccupancy 字段（双重保险）
        if (place.getCurrentOccupancy() != null && place.getCurrentOccupancy() > 0) {
            throw new IllegalArgumentException("场所有占用记录，请先清退后再删除");
        }

        placeRepository.deleteById(id);
    }

    // ==================== 事件发布 ====================

    private void publishEvents(UniversalPlace place) {
        place.getDomainEvents().forEach(eventPublisher::publish);
        place.clearDomainEvents();
    }

    // ==================== 私有方法 ====================

    private PlaceTreeNode buildTreeNode(UniversalPlace place) {
        PlaceTreeNode node = new PlaceTreeNode();
        node.setId(place.getId());
        node.setPlaceCode(place.getPlaceCode());
        node.setPlaceName(place.getPlaceName());
        node.setTypeCode(place.getTypeCode());
        node.setDescription(place.getDescription());
        node.setParentId(place.getParentId());
        node.setLevel(place.getLevel());
        node.setCapacity(place.getCapacity());
        node.setCurrentOccupancy(place.getCurrentOccupancy());
        node.setOrgUnitId(place.getOrgUnitId());
        if (place.getOrgUnitId() != null) {
            orgUnitRepository.findById(place.getOrgUnitId())
                    .ifPresent(org -> node.setOrgUnitName(org.getUnitName()));
        }
        node.setResponsibleUserId(place.getResponsibleUserId());
        if (place.getResponsibleUserId() != null) {
            userRepository.findById(place.getResponsibleUserId())
                    .ifPresent(user -> node.setResponsibleUserName(user.getRealName()));
        }
        node.setStatus(place.getStatus().getValue());
        node.setAttributes(place.getAttributes());

        // 组织继承计算（防御性null检查）
        try {
            Long effectiveOrgId = inheritanceService.getEffectiveOrgUnitId(place);
            node.setEffectiveOrgUnitId(effectiveOrgId);
            node.setIsOrgInherited(inheritanceService.isOrgInherited(place));
            if (effectiveOrgId != null && place.getOrgUnitId() == null) {
                orgUnitRepository.findById(effectiveOrgId)
                        .ifPresent(org -> node.setEffectiveOrgUnitName(org.getUnitName()));
            }
        } catch (Exception e) {
            log.warn("计算场所[{}]组织继承时出错: {}", place.getId(), e.getMessage());
            node.setIsOrgInherited(false);
        }
        if (place.getParentId() != null) {
            placeRepository.findById(place.getParentId()).ifPresent(parent ->
                    node.setParentOrgUnitId(parent.getOrgUnitId())
            );
        }

        // 负责人继承计算（防御性null检查）
        try {
            node.setIsResponsibleInherited(inheritanceService.isResponsibleInherited(place));
            Long effectiveResponsibleId = inheritanceService.getEffectiveResponsibleUserId(place);
            node.setEffectiveResponsibleUserId(effectiveResponsibleId);
            if (effectiveResponsibleId != null) {
                userRepository.findById(effectiveResponsibleId)
                        .ifPresent(user -> node.setEffectiveResponsibleUserName(user.getRealName()));
            }
        } catch (Exception e) {
            log.warn("计算场所[{}]负责人继承时出错: {}", place.getId(), e.getMessage());
            node.setIsResponsibleInherited(false);
        }

        // 获取类型信息
        placeTypeRepository.findByTypeCode(place.getTypeCode()).ifPresent(type -> {
            node.setTypeName(type.getTypeName());
            node.setHasCapacity(type.isHasCapacity());
            node.setBookable(type.isBookable());
            node.setAssignable(type.isAssignable());
            node.setOccupiable(type.isOccupiable());
            node.setCapacityUnit(type.getCapacityUnit());
            node.setLeaf(type.getAllowedChildTypeCodes() == null || type.getAllowedChildTypeCodes().isEmpty());
        });

        // 递归加载子节点
        List<UniversalPlace> children = placeRepository.findChildren(place.getId());
        if (!children.isEmpty()) {
            node.setChildren(children.stream()
                    .map(this::buildTreeNode)
                    .collect(Collectors.toList()));
        }

        return node;
    }

    private PlaceDTO toDTO(UniversalPlace place) {
        PlaceDTO dto = new PlaceDTO();
        dto.setId(place.getId());
        dto.setPlaceCode(place.getPlaceCode());
        dto.setPlaceName(place.getPlaceName());
        dto.setTypeCode(place.getTypeCode());
        dto.setDescription(place.getDescription());
        dto.setParentId(place.getParentId());
        dto.setPath(place.getPath());
        dto.setLevel(place.getLevel());
        dto.setCapacity(place.getCapacity());
        dto.setCurrentOccupancy(place.getCurrentOccupancy());
        dto.setOrgUnitId(place.getOrgUnitId());
        dto.setResponsibleUserId(place.getResponsibleUserId());
        dto.setStatus(place.getStatus().getValue());
        dto.setAttributes(place.getAttributes());

        // 获取类型信息
        placeTypeRepository.findByTypeCode(place.getTypeCode()).ifPresent(type -> {
            dto.setTypeName(type.getTypeName());
            dto.setHasCapacity(type.isHasCapacity());
            dto.setBookable(type.isBookable());
            dto.setAssignable(type.isAssignable());
            dto.setOccupiable(type.isOccupiable());
            dto.setCapacityUnit(type.getCapacityUnit());
        });

        return dto;
    }

    // ==================== 入住管理 ====================

    public List<OccupantDTO> getOccupants(Long placeId) {
        return occupantRepository.findActiveByPlaceId(placeId).stream()
                .map(this::toOccupantDTO)
                .collect(Collectors.toList());
    }

    public List<OccupantDTO> getOccupantHistory(Long placeId) {
        return occupantRepository.findAllByPlaceId(placeId).stream()
                .map(this::toOccupantDTO)
                .collect(Collectors.toList());
    }

    /**
     * 查询指定场所列表中的所有活跃占用记录（带场所信息），用于住宿管理列表视图
     */
    public List<OccupantWithPlaceDTO> getOccupantsForPlaces(List<Long> placeIds, String occupantType) {
        List<UniversalPlaceOccupant> occupants = occupantRepository.findActiveByPlaceIds(placeIds, occupantType);
        // 批量加载场所信息
        Set<Long> uniquePlaceIds = occupants.stream()
                .map(UniversalPlaceOccupant::getPlaceId)
                .collect(Collectors.toSet());
        Map<Long, UniversalPlace> placeMap = new HashMap<>();
        // Also build parent-name map for building names
        Map<Long, String> placeParentNameMap = new HashMap<>();
        for (Long pid : uniquePlaceIds) {
            placeRepository.findById(pid).ifPresent(p -> {
                placeMap.put(pid, p);
                // Get building name (parent or grandparent)
                if (p.getParentId() != null) {
                    placeRepository.findById(p.getParentId()).ifPresent(parent -> {
                        String parentType = parent.getTypeCode() != null ? parent.getTypeCode().toLowerCase() : "";
                        if (parentType.contains("floor") || parentType.contains("楼层")) {
                            // Parent is a floor, grandparent is the building
                            if (parent.getParentId() != null) {
                                placeRepository.findById(parent.getParentId())
                                        .ifPresent(gp -> placeParentNameMap.put(pid, gp.getPlaceName()));
                            }
                        } else {
                            // Parent is the building
                            placeParentNameMap.put(pid, parent.getPlaceName());
                        }
                    });
                }
            });
        }

        return occupants.stream().map(occ -> {
            OccupantDTO base = toOccupantDTO(occ);
            OccupantWithPlaceDTO dto = new OccupantWithPlaceDTO();
            dto.setId(base.getId());
            dto.setPlaceId(base.getPlaceId());
            dto.setOccupantType(base.getOccupantType());
            dto.setOccupantId(base.getOccupantId());
            dto.setOccupantName(base.getOccupantName());
            dto.setUsername(base.getUsername());
            dto.setOrgUnitName(base.getOrgUnitName());
            dto.setUserTypeName(base.getUserTypeName());
            dto.setGender(base.getGender());
            dto.setPositionNo(base.getPositionNo());
            dto.setCheckInTime(base.getCheckInTime());
            dto.setCheckOutTime(base.getCheckOutTime());
            dto.setStatus(base.getStatus());
            dto.setRemark(base.getRemark());
            // Enrich with place info
            UniversalPlace place = placeMap.get(occ.getPlaceId());
            if (place != null) {
                dto.setPlaceName(place.getPlaceName());
                dto.setPlaceCode(place.getPlaceCode());
            }
            dto.setBuildingName(placeParentNameMap.get(occ.getPlaceId()));
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * 查询某个占用者的所有占用历史（跨场所），带场所信息
     */
    public List<OccupantWithPlaceDTO> getOccupantHistoryByOccupant(String occupantType, Long occupantId) {
        List<UniversalPlaceOccupant> records = occupantRepository.findAllByOccupant(occupantType, occupantId);
        Set<Long> uniquePlaceIds = records.stream()
                .map(UniversalPlaceOccupant::getPlaceId)
                .collect(Collectors.toSet());
        Map<Long, UniversalPlace> placeMap = new HashMap<>();
        Map<Long, String> placeParentNameMap = new HashMap<>();
        for (Long pid : uniquePlaceIds) {
            placeRepository.findById(pid).ifPresent(p -> {
                placeMap.put(pid, p);
                if (p.getParentId() != null) {
                    placeRepository.findById(p.getParentId()).ifPresent(parent -> {
                        String parentType = parent.getTypeCode() != null ? parent.getTypeCode().toLowerCase() : "";
                        if (parentType.contains("floor") || parentType.contains("楼层")) {
                            if (parent.getParentId() != null) {
                                placeRepository.findById(parent.getParentId())
                                        .ifPresent(gp -> placeParentNameMap.put(pid, gp.getPlaceName()));
                            }
                        } else {
                            placeParentNameMap.put(pid, parent.getPlaceName());
                        }
                    });
                }
            });
        }
        return records.stream().map(occ -> {
            OccupantDTO base = toOccupantDTO(occ);
            OccupantWithPlaceDTO dto = new OccupantWithPlaceDTO();
            dto.setId(base.getId());
            dto.setPlaceId(base.getPlaceId());
            dto.setOccupantType(base.getOccupantType());
            dto.setOccupantId(base.getOccupantId());
            dto.setOccupantName(base.getOccupantName());
            dto.setUsername(base.getUsername());
            dto.setOrgUnitName(base.getOrgUnitName());
            dto.setUserTypeName(base.getUserTypeName());
            dto.setGender(base.getGender());
            dto.setPositionNo(base.getPositionNo());
            dto.setCheckInTime(base.getCheckInTime());
            dto.setCheckOutTime(base.getCheckOutTime());
            dto.setStatus(base.getStatus());
            dto.setRemark(base.getRemark());
            UniversalPlace place = placeMap.get(occ.getPlaceId());
            if (place != null) {
                dto.setPlaceName(place.getPlaceName());
                dto.setPlaceCode(place.getPlaceCode());
            }
            dto.setBuildingName(placeParentNameMap.get(occ.getPlaceId()));
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional
    public OccupantDTO checkIn(Long placeId, CheckInCommand command) {
        UniversalPlace place = placeRepository.findById(placeId)
                .orElseThrow(() -> new IllegalArgumentException("场所不存在"));

        // 验证容量
        if (place.getCapacity() != null && place.getCapacity() > 0) {
            int current = place.getCurrentOccupancy() != null ? place.getCurrentOccupancy() : 0;
            if (current >= place.getCapacity()) {
                throw new IllegalStateException("场所已满，无法入住");
            }
        }

        // 验证位置是否被占用
        if (command.getPositionNo() != null && !command.getPositionNo().isEmpty()) {
            if (occupantRepository.isPositionOccupied(placeId, command.getPositionNo())) {
                throw new IllegalStateException("位置 " + command.getPositionNo() + " 已被占用");
            }
        }

        // 校验用户必须存在于系统中，并从实时数据填充快照字段
        User user = userRepository.findById(command.getOccupantId())
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + command.getOccupantId()));

        String occupantName = user.getRealName() != null ? user.getRealName() : user.getUsername();
        String username = user.getUsername();
        Integer gender = user.getGender();

        // 从 access_relations 查询用户当前主要组织
        String orgUnitName = null;
        List<AccessRelation> userOrgRels = accessRelationRepository
                .findBySubjectAndResourceType("user", user.getId(), "org_unit");
        Optional<AccessRelation> primaryRel = userOrgRels.stream()
                .filter(r -> r.getBooleanMeta("isPrimary"))
                .findFirst();
        if (primaryRel.isPresent()) {
            orgUnitName = orgUnitRepository.findById(primaryRel.get().getResourceId())
                    .map(OrgUnit::getUnitName)
                    .orElse(null);
        }

        // 创建占用记录（使用实时数据作为快照）
        UniversalPlaceOccupant occupant = UniversalPlaceOccupant.create(
                placeId,
                command.getOccupantType(),
                command.getOccupantId(),
                occupantName,
                username,
                orgUnitName,
                gender,
                command.getPositionNo()
        );
        if (command.getRemark() != null) {
            occupant.setRemark(command.getRemark());
        }
        occupant = occupantRepository.save(occupant);

        // 更新场所占用数
        place.checkIn();
        placeRepository.save(place);
        publishEvents(place);

        // 发布审计事件
        activityEventPublisher.newEvent("place", "PLACE", "CHECK_IN", "入住")
                .resourceId(placeId)
                .resourceName(place.getPlaceName())
                .changedFields(List.of(
                        new FieldChange("occupant", null, occupantName),
                        new FieldChange("username", null, username),
                        new FieldChange("positionNo", null, command.getPositionNo()),
                        new FieldChange("orgUnitName", null, orgUnitName)
                ))
                .publish();

        // 触发事件
        if (triggerService != null) {
            try {
                triggerService.fire("DORM_CHECKIN", Map.of(
                    "occupantId", command.getOccupantId(),
                    "occupantName", occupantName != null ? occupantName : "",
                    "placeId", placeId,
                    "placeName", place.getPlaceName() != null ? place.getPlaceName() : ""
                ));
            } catch (Exception ignored) {}
        }

        return toOccupantDTO(occupant);
    }

    @Transactional
    public List<OccupantDTO> batchCheckIn(Long placeId, List<CheckInCommand> commands) {
        List<OccupantDTO> results = new ArrayList<>();
        for (CheckInCommand cmd : commands) {
            results.add(checkIn(placeId, cmd));
        }
        return results;
    }

    @Transactional
    public void checkOut(Long placeId, Long recordId) {
        UniversalPlace place = placeRepository.findById(placeId)
                .orElseThrow(() -> new IllegalArgumentException("场所不存在"));

        UniversalPlaceOccupant occupant = occupantRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("占用记录不存在"));

        if (!occupant.getPlaceId().equals(placeId)) {
            throw new IllegalArgumentException("占用记录不属于该场所");
        }
        if (!occupant.isActive()) {
            throw new IllegalStateException("该记录已退出");
        }

        String occupantName = occupant.getOccupantName();
        occupant.checkOut();
        occupantRepository.save(occupant);

        place.checkOut();
        placeRepository.save(place);
        publishEvents(place);

        // 发布审计事件
        activityEventPublisher.newEvent("place", "PLACE", "CHECK_OUT", "退出")
                .resourceId(placeId)
                .resourceName(place.getPlaceName())
                .changedFields(List.of(
                        new FieldChange("occupant", occupantName, null),
                        new FieldChange("username", occupant.getUsername(), null),
                        new FieldChange("positionNo", occupant.getPositionNo(), null)
                ))
                .publish();

        // 触发事件
        if (triggerService != null) {
            try {
                triggerService.fire("DORM_CHECKOUT", Map.of(
                    "occupantId", occupant.getOccupantId(),
                    "occupantName", occupantName != null ? occupantName : "",
                    "placeId", placeId,
                    "placeName", place.getPlaceName() != null ? place.getPlaceName() : ""
                ));
            } catch (Exception ignored) {}
        }
    }

    @Transactional
    public void swapPositions(Long placeId, Long recordId1, Long recordId2) {
        UniversalPlaceOccupant occ1 = occupantRepository.findById(recordId1)
                .orElseThrow(() -> new IllegalArgumentException("占用记录1不存在"));
        UniversalPlaceOccupant occ2 = occupantRepository.findById(recordId2)
                .orElseThrow(() -> new IllegalArgumentException("占用记录2不存在"));

        if (!occ1.getPlaceId().equals(placeId) || !occ2.getPlaceId().equals(placeId)) {
            throw new IllegalArgumentException("占用记录不属于该场所");
        }

        String tempPos = occ1.getPositionNo();
        occ1.setPositionNo(occ2.getPositionNo());
        occ2.setPositionNo(tempPos);

        occupantRepository.save(occ1);
        occupantRepository.save(occ2);
    }

    private OccupantDTO toOccupantDTO(UniversalPlaceOccupant occ) {
        OccupantDTO dto = new OccupantDTO();
        dto.setId(occ.getId());
        dto.setPlaceId(occ.getPlaceId());
        dto.setOccupantType(occ.getOccupantType());
        dto.setOccupantId(occ.getOccupantId());
        dto.setPositionNo(occ.getPositionNo());
        dto.setCheckInTime(occ.getCheckInTime());
        dto.setCheckOutTime(occ.getCheckOutTime());
        dto.setStatus(occ.getStatus());
        dto.setRemark(occ.getRemark());

        // 在住人员：实时查用户最新信息（姓名/组织/性别都可能变）
        // 已退出人员：用入住时的快照（反映当时状态）
        if (occ.isActive() && occ.getUsername() != null && !occ.getUsername().isEmpty()) {
            Optional<User> userOpt = userRepository.findByUsername(occ.getUsername());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                dto.setOccupantName(user.getRealName());
                dto.setUsername(user.getUsername());
                dto.setGender(user.getGender());
                // 查用户类型名称
                if (user.getUserTypeCode() != null) {
                    userTypeRepository.findByTypeCode(user.getUserTypeCode())
                            .ifPresent(ut -> dto.setUserTypeName(ut.getTypeName()));
                }
                // 查当前组织
                List<AccessRelation> userOrgRels = accessRelationRepository
                        .findBySubjectAndResourceType("user", user.getId(), "org_unit");
                userOrgRels.stream()
                        .filter(r -> r.getBooleanMeta("isPrimary"))
                        .findFirst()
                        .ifPresent(rel -> orgUnitRepository.findById(rel.getResourceId())
                                .ifPresent(org -> dto.setOrgUnitName(org.getUnitName())));
                return dto;
            }
        }

        // fallback：用快照数据（已退出记录 或 查不到用户时）
        dto.setOccupantName(occ.getOccupantName());
        dto.setUsername(occ.getUsername());
        dto.setGender(occ.getGender());
        dto.setOrgUnitName(occ.getOrgUnitName());
        return dto;
    }

    // ==================== DTO 类 ====================

    @Data
    public static class PlaceDTO {
        private Long id;
        private String placeCode;
        private String placeName;
        private String typeCode;
        private String typeName;
        private String typeIcon;
        private String description;
        private Long parentId;
        private String parentName;
        private String path;
        private Integer level;
        private Integer capacity;
        private Integer currentOccupancy;
        private Long orgUnitId;
        private String orgUnitName;
        private Long responsibleUserId;
        private String responsibleUserName;
        private Integer status;
        private Map<String, Object> attributes;

        // 类型特性
        private boolean hasCapacity;
        private boolean bookable;
        private boolean assignable;
        private boolean occupiable;
        private String capacityUnit;
    }

    @Data
    public static class PlaceTreeNode {
        private Long id;
        private String placeCode;
        private String placeName;
        private String typeCode;
        private String typeName;
        private String typeIcon;
        private String description;
        private Long parentId;
        private Integer level;
        private Integer capacity;
        private Integer currentOccupancy;
        private Long orgUnitId;
        private String orgUnitName;
        private Long responsibleUserId;
        private String responsibleUserName;
        private Integer status;
        private Map<String, Object> attributes;

        // 组织继承
        private Long effectiveOrgUnitId;
        private String effectiveOrgUnitName;
        private Boolean isOrgInherited;
        private Long parentOrgUnitId;

        // 负责人继承
        private Long effectiveResponsibleUserId;
        private String effectiveResponsibleUserName;
        private Boolean isResponsibleInherited;

        // 类型特性
        private boolean hasCapacity;
        private boolean bookable;
        private boolean assignable;
        private boolean occupiable;
        private String capacityUnit;
        private boolean leaf;

        // 子节点
        private List<PlaceTreeNode> children;
    }

    @Data
    public static class PlaceStatistics {
        private int totalCount;
        private int totalCapacity;
        private int totalOccupancy;
        private Map<String, Long> countByType;
        private Map<Integer, Long> countByStatus;
        private List<OrgPlaceStats> statsByOrg;

        public double getOccupancyRate() {
            if (totalCapacity == 0) return 0;
            return (double) totalOccupancy / totalCapacity * 100;
        }
    }

    @Data
    public static class OrgPlaceStats {
        private Long orgUnitId;
        private String orgUnitName;
        private int placeCount;
        private int totalCapacity;
        private int totalOccupancy;

        public double getOccupancyRate() {
            if (totalCapacity == 0) return 0;
            return (double) totalOccupancy / totalCapacity * 100;
        }
    }

    @Data
    public static class CreatePlaceCommand {
        private String placeCode;
        private String placeName;
        private String typeCode;
        private String description;
        private Long parentId;
        private Integer status;
        private Integer capacity;
        private String gender;
        private Long orgUnitId;
        private Long responsibleUserId;
        private Map<String, Object> attributes;
    }

    @Data
    public static class UpdatePlaceCommand {
        private String placeCode;
        private String placeName;
        private String typeCode;
        private String description;
        private Integer status;
        private Integer capacity;
        private String gender;
        private Long orgUnitId;
        private Boolean clearOrgOverride;
        private Long responsibleUserId;
        private String reason;
        private Map<String, Object> attributes;
    }

    @Data
    public static class CheckInCommand {
        private String occupantType;
        private Long occupantId;
        private String occupantName;
        private String username;
        private String orgUnitName;
        private Integer gender;
        private String positionNo;
        private String remark;
    }

    @Data
    public static class OccupantDTO {
        private Long id;
        private Long placeId;
        private String occupantType;
        private Long occupantId;
        private String occupantName;
        private String username;
        private String orgUnitName;
        private String userTypeName;
        private Integer gender;
        private String positionNo;
        private java.time.LocalDateTime checkInTime;
        private java.time.LocalDateTime checkOutTime;
        private Integer status;
        private String remark;
    }

    @Data
    public static class OccupantWithPlaceDTO {
        private Long id;
        private Long placeId;
        private String placeName;
        private String placeCode;
        private String buildingName;
        private String occupantType;
        private Long occupantId;
        private String occupantName;
        private String username;
        private String orgUnitName;
        private String userTypeName;
        private Integer gender;
        private String positionNo;
        private java.time.LocalDateTime checkInTime;
        private java.time.LocalDateTime checkOutTime;
        private Integer status;
        private String remark;
    }
}
