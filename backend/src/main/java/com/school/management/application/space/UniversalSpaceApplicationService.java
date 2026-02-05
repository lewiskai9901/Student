package com.school.management.application.space;

import com.school.management.domain.space.model.aggregate.UniversalSpace;
import com.school.management.domain.space.model.entity.UniversalSpaceType;
import com.school.management.domain.space.model.valueobject.SpaceStatus;
import com.school.management.domain.space.repository.UniversalSpaceRepository;
import com.school.management.domain.space.repository.UniversalSpaceTypeRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 通用空间应用服务
 */
@Service
@RequiredArgsConstructor
public class UniversalSpaceApplicationService {

    private final UniversalSpaceRepository spaceRepository;
    private final UniversalSpaceTypeRepository spaceTypeRepository;

    // ==================== 查询 ====================

    /**
     * 获取空间树
     */
    public List<SpaceTreeNode> getSpaceTree() {
        List<UniversalSpace> roots = spaceRepository.findAllRoots();
        return roots.stream()
                .map(this::buildTreeNode)
                .collect(Collectors.toList());
    }

    /**
     * 获取指定类型的空间树
     */
    public List<SpaceTreeNode> getSpaceTreeByType(String typeCode) {
        List<UniversalSpace> spaces = spaceRepository.findByTypeCode(typeCode);
        List<UniversalSpace> roots = spaces.stream()
                .filter(s -> s.getParentId() == null)
                .collect(Collectors.toList());
        return roots.stream()
                .map(this::buildTreeNode)
                .collect(Collectors.toList());
    }

    /**
     * 获取子空间
     */
    public List<SpaceDTO> getChildren(Long parentId) {
        List<UniversalSpace> children = spaceRepository.findChildren(parentId);
        return children.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取空间
     */
    public SpaceDTO getSpaceById(Long id) {
        return spaceRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new IllegalArgumentException("空间不存在: " + id));
    }

    /**
     * 根据编码获取空间
     */
    public SpaceDTO getSpaceByCode(String spaceCode) {
        return spaceRepository.findBySpaceCode(spaceCode)
                .map(this::toDTO)
                .orElseThrow(() -> new IllegalArgumentException("空间不存在: " + spaceCode));
    }

    /**
     * 获取允许创建的子类型
     */
    public List<UniversalSpaceType> getAllowedChildTypes(Long parentId) {
        if (parentId == null) {
            // 根空间，返回所有根类型
            return spaceTypeRepository.findAllRootTypes();
        }
        UniversalSpace parent = spaceRepository.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("父空间不存在"));
        UniversalSpaceType parentType = spaceTypeRepository.findByTypeCode(parent.getTypeCode())
                .orElseThrow(() -> new IllegalArgumentException("父空间类型不存在"));
        return spaceTypeRepository.findAllowedChildTypes(parentType.getTypeCode());
    }

    /**
     * 获取统计数据
     */
    public SpaceStatistics getStatistics() {
        List<UniversalSpace> allSpaces = spaceRepository.findAll();

        SpaceStatistics stats = new SpaceStatistics();
        stats.setTotalCount(allSpaces.size());

        // 按类型统计
        Map<String, Long> typeCount = allSpaces.stream()
                .collect(Collectors.groupingBy(UniversalSpace::getTypeCode, Collectors.counting()));
        stats.setCountByType(typeCount);

        // 计算总容量和占用
        int totalCapacity = 0;
        int totalOccupancy = 0;
        for (UniversalSpace space : allSpaces) {
            if (space.getCapacity() != null) {
                totalCapacity += space.getCapacity();
                totalOccupancy += space.getCurrentOccupancy() != null ? space.getCurrentOccupancy() : 0;
            }
        }
        stats.setTotalCapacity(totalCapacity);
        stats.setTotalOccupancy(totalOccupancy);

        // 按状态统计
        Map<Integer, Long> statusCount = allSpaces.stream()
                .collect(Collectors.groupingBy(s -> s.getStatus().getValue(), Collectors.counting()));
        stats.setCountByStatus(statusCount);

        return stats;
    }

    // ==================== 创建 ====================

    /**
     * 创建空间
     */
    @Transactional
    public SpaceDTO createSpace(CreateSpaceCommand command) {
        // 验证类型
        UniversalSpaceType spaceType = spaceTypeRepository.findByTypeCode(command.getTypeCode())
                .orElseThrow(() -> new IllegalArgumentException("空间类型不存在: " + command.getTypeCode()));

        if (!spaceType.isEnabled()) {
            throw new IllegalArgumentException("空间类型已禁用");
        }

        // 验证父空间
        UniversalSpace parent = null;
        if (command.getParentId() != null) {
            parent = spaceRepository.findById(command.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("父空间不存在"));

            // 验证是否允许创建此类型的子空间
            UniversalSpaceType parentType = spaceTypeRepository.findByTypeCode(parent.getTypeCode())
                    .orElseThrow(() -> new IllegalArgumentException("父空间类型不存在"));

            List<String> allowedChildren = parentType.getAllowedChildTypes();
            if (allowedChildren == null || !allowedChildren.contains(command.getTypeCode())) {
                throw new IllegalArgumentException("父空间不允许创建此类型的子空间");
            }
        } else {
            // 根空间必须是根类型
            if (!spaceType.isRootType()) {
                throw new IllegalArgumentException("此类型不能作为根空间");
            }
        }

        // 创建空间
        UniversalSpace space = UniversalSpace.create(
                command.getSpaceName(),
                command.getTypeCode(),
                command.getParentId()
        );

        space.setDescription(command.getDescription());
        space.setCapacity(command.getCapacity());
        space.setOrgUnitId(command.getOrgUnitId());
        space.setResponsibleUserId(command.getResponsibleUserId());

        if (command.getAttributes() != null) {
            command.getAttributes().forEach(space::setAttribute);
        }

        // 计算层级信息
        if (parent != null) {
            space.setLevel(parent.getLevel() + 1);
        } else {
            space.setLevel(0);
        }

        // 保存
        UniversalSpace saved = spaceRepository.save(space);

        // 更新路径（需要ID）
        if (parent != null) {
            saved.setPath(parent.getPath() + saved.getId() + "/");
        } else {
            saved.setPath("/" + saved.getId() + "/");
        }
        saved = spaceRepository.save(saved);

        return toDTO(saved);
    }

    // ==================== 更新 ====================

    /**
     * 更新空间
     */
    @Transactional
    public SpaceDTO updateSpace(Long id, UpdateSpaceCommand command) {
        UniversalSpace space = spaceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("空间不存在"));

        if (command.getSpaceName() != null) {
            space.setSpaceName(command.getSpaceName());
        }
        if (command.getDescription() != null) {
            space.setDescription(command.getDescription());
        }
        if (command.getCapacity() != null) {
            space.setCapacity(command.getCapacity());
        }
        if (command.getOrgUnitId() != null) {
            space.setOrgUnitId(command.getOrgUnitId());
        }
        if (command.getResponsibleUserId() != null) {
            space.setResponsibleUserId(command.getResponsibleUserId());
        }
        if (command.getAttributes() != null) {
            command.getAttributes().forEach(space::setAttribute);
        }

        UniversalSpace saved = spaceRepository.save(space);
        return toDTO(saved);
    }

    /**
     * 更改空间状态
     */
    @Transactional
    public SpaceDTO changeStatus(Long id, SpaceStatus status) {
        UniversalSpace space = spaceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("空间不存在"));

        switch (status) {
            case NORMAL:
                space.enable();
                break;
            case DISABLED:
                space.disable();
                break;
            case MAINTENANCE:
                space.startMaintenance();
                break;
        }

        UniversalSpace saved = spaceRepository.save(space);
        return toDTO(saved);
    }

    // ==================== 删除 ====================

    /**
     * 删除空间
     */
    @Transactional
    public void deleteSpace(Long id) {
        UniversalSpace space = spaceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("空间不存在"));

        // 检查是否有子空间
        int childCount = spaceRepository.countChildren(id);
        if (childCount > 0) {
            throw new IllegalArgumentException("空间有子空间，不能删除");
        }

        // 检查是否有占用
        if (space.getCurrentOccupancy() != null && space.getCurrentOccupancy() > 0) {
            throw new IllegalArgumentException("空间有占用者，不能删除");
        }

        spaceRepository.deleteById(id);
    }

    // ==================== 私有方法 ====================

    private SpaceTreeNode buildTreeNode(UniversalSpace space) {
        SpaceTreeNode node = new SpaceTreeNode();
        node.setId(space.getId());
        node.setSpaceCode(space.getSpaceCode());
        node.setSpaceName(space.getSpaceName());
        node.setTypeCode(space.getTypeCode());
        node.setDescription(space.getDescription());
        node.setParentId(space.getParentId());
        node.setLevel(space.getLevel());
        node.setCapacity(space.getCapacity());
        node.setCurrentOccupancy(space.getCurrentOccupancy());
        node.setOrgUnitId(space.getOrgUnitId());
        node.setResponsibleUserId(space.getResponsibleUserId());
        node.setStatus(space.getStatus().getValue());
        node.setAttributes(space.getAttributes());

        // 获取类型信息
        spaceTypeRepository.findByTypeCode(space.getTypeCode()).ifPresent(type -> {
            node.setTypeName(type.getTypeName());
            node.setTypeIcon(type.getIcon());
            node.setHasCapacity(type.isHasCapacity());
            node.setBookable(type.isBookable());
            node.setAssignable(type.isAssignable());
            node.setOccupiable(type.isOccupiable());
            node.setCapacityUnit(type.getCapacityUnit());
            node.setLeaf(type.getAllowedChildTypes() == null || type.getAllowedChildTypes().isEmpty());
        });

        // 递归加载子节点
        List<UniversalSpace> children = spaceRepository.findChildren(space.getId());
        if (!children.isEmpty()) {
            node.setChildren(children.stream()
                    .map(this::buildTreeNode)
                    .collect(Collectors.toList()));
        }

        return node;
    }

    private SpaceDTO toDTO(UniversalSpace space) {
        SpaceDTO dto = new SpaceDTO();
        dto.setId(space.getId());
        dto.setSpaceCode(space.getSpaceCode());
        dto.setSpaceName(space.getSpaceName());
        dto.setTypeCode(space.getTypeCode());
        dto.setDescription(space.getDescription());
        dto.setParentId(space.getParentId());
        dto.setPath(space.getPath());
        dto.setLevel(space.getLevel());
        dto.setCapacity(space.getCapacity());
        dto.setCurrentOccupancy(space.getCurrentOccupancy());
        dto.setOrgUnitId(space.getOrgUnitId());
        dto.setResponsibleUserId(space.getResponsibleUserId());
        dto.setStatus(space.getStatus().getValue());
        dto.setAttributes(space.getAttributes());

        // 获取类型信息
        spaceTypeRepository.findByTypeCode(space.getTypeCode()).ifPresent(type -> {
            dto.setTypeName(type.getTypeName());
            dto.setTypeIcon(type.getIcon());
            dto.setHasCapacity(type.isHasCapacity());
            dto.setBookable(type.isBookable());
            dto.setAssignable(type.isAssignable());
            dto.setOccupiable(type.isOccupiable());
            dto.setCapacityUnit(type.getCapacityUnit());
        });

        return dto;
    }

    // ==================== DTO 类 ====================

    @Data
    public static class SpaceDTO {
        private Long id;
        private String spaceCode;
        private String spaceName;
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
    public static class SpaceTreeNode {
        private Long id;
        private String spaceCode;
        private String spaceName;
        private String typeCode;
        private String typeName;
        private String typeIcon;
        private String description;
        private Long parentId;
        private Integer level;
        private Integer capacity;
        private Integer currentOccupancy;
        private Long orgUnitId;
        private Long responsibleUserId;
        private Integer status;
        private Map<String, Object> attributes;

        // 类型特性
        private boolean hasCapacity;
        private boolean bookable;
        private boolean assignable;
        private boolean occupiable;
        private String capacityUnit;
        private boolean leaf;

        // 子节点
        private List<SpaceTreeNode> children;
    }

    @Data
    public static class SpaceStatistics {
        private int totalCount;
        private int totalCapacity;
        private int totalOccupancy;
        private Map<String, Long> countByType;
        private Map<Integer, Long> countByStatus;

        public double getOccupancyRate() {
            if (totalCapacity == 0) return 0;
            return (double) totalOccupancy / totalCapacity * 100;
        }
    }

    @Data
    public static class CreateSpaceCommand {
        private String spaceName;
        private String typeCode;
        private String description;
        private Long parentId;
        private Integer capacity;
        private Long orgUnitId;
        private Long responsibleUserId;
        private Map<String, Object> attributes;
    }

    @Data
    public static class UpdateSpaceCommand {
        private String spaceName;
        private String description;
        private Integer capacity;
        private Long orgUnitId;
        private Long responsibleUserId;
        private Map<String, Object> attributes;
    }
}
