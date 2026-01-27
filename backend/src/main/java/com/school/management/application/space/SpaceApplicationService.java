package com.school.management.application.space;

import com.school.management.application.space.command.CheckInCommand;
import com.school.management.application.space.command.CreateSpaceCommand;
import com.school.management.application.space.command.UpdateSpaceCommand;
import com.school.management.application.space.query.SpaceDTO;
import com.school.management.application.space.query.SpaceOccupantDTO;
import com.school.management.application.space.query.SpaceQueryCriteria;
import com.school.management.application.space.query.SpaceStatisticsDTO;
import com.school.management.domain.space.model.aggregate.Space;
import com.school.management.domain.space.model.entity.SpaceClassAssignment;
import com.school.management.domain.space.model.entity.SpaceOccupant;
import com.school.management.domain.space.model.valueobject.*;
import com.school.management.domain.space.repository.SpaceClassAssignmentRepository;
import com.school.management.domain.space.repository.SpaceOccupantRepository;
import com.school.management.domain.space.repository.SpaceRepository;
import com.school.management.exception.BusinessException;
import com.school.management.infrastructure.persistence.space.SpacePO;
import com.school.management.infrastructure.persistence.space.SpaceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 场所应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpaceApplicationService {

    private final SpaceRepository spaceRepository;
    private final SpaceOccupantRepository occupantRepository;
    private final SpaceClassAssignmentRepository classAssignmentRepository;
    private final SpaceMapper spaceMapper;

    /**
     * 创建场所
     */
    @Transactional
    public Long createSpace(CreateSpaceCommand command) {
        // 验证编码唯一性
        if (command.getSpaceCode() != null && spaceRepository.existsByCode(command.getSpaceCode())) {
            throw new BusinessException("场所编码已存在: " + command.getSpaceCode());
        }

        Space space;
        switch (command.getSpaceType()) {
            case CAMPUS:
                String campusCode = command.getSpaceCode() != null ? command.getSpaceCode()
                    : generateCode("CAMPUS");
                space = Space.createCampus(campusCode, command.getSpaceName());
                break;

            case BUILDING:
                Space campus = getParentSpace(command.getParentId(), SpaceType.CAMPUS);
                // 校验楼号唯一性
                if (command.getBuildingNo() != null &&
                    spaceRepository.existsByBuildingNoInCampus(command.getBuildingNo(), campus.getId(), null)) {
                    throw new BusinessException("该校区内楼号\"" + command.getBuildingNo() + "\"已存在");
                }
                String buildingCode = command.getSpaceCode() != null ? command.getSpaceCode()
                    : generateCode("BLDG");
                space = Space.createBuilding(buildingCode, command.getSpaceName(),
                    command.getBuildingType(), command.getBuildingNo(), campus);
                break;

            case FLOOR:
                Space building = getParentSpace(command.getParentId(), SpaceType.BUILDING);
                if (command.getFloorNumber() == null) {
                    throw new BusinessException("楼层号不能为空");
                }
                space = Space.createFloor(command.getFloorNumber(), building);
                break;

            case ROOM:
                Space floor = getParentSpace(command.getParentId(), SpaceType.FLOOR);
                // 校验房间号唯一性（同一楼栋内）
                if (command.getRoomNo() != null && floor.getBuildingId() != null &&
                    spaceRepository.existsByRoomNoInBuilding(command.getRoomNo(), floor.getBuildingId(), null)) {
                    throw new BusinessException("该楼栋内房间号\"" + command.getRoomNo() + "\"已存在");
                }
                String roomCode = command.getSpaceCode() != null ? command.getSpaceCode()
                    : generateRoomCode(floor);
                space = Space.createRoom(roomCode, command.getSpaceName(),
                    command.getRoomType(), command.getCapacity(), command.getRoomNo(), floor);
                break;

            default:
                throw new BusinessException("不支持的场所类型");
        }

        // 设置其他属性
        if (command.getOrgUnitId() != null) {
            space.assignToOrgUnit(command.getOrgUnitId());
        }
        if (command.getClassId() != null) {
            space.assignToClass(command.getClassId());
        }
        if (command.getResponsibleUserId() != null) {
            space.assignResponsible(command.getResponsibleUserId());
        }
        if (command.getDescription() != null) {
            space.updateInfo(space.getSpaceName(), command.getDescription());
        }
        if (command.getAttributes() != null) {
            space.updateAttributes(command.getAttributes());
        }
        // 设置性别限制
        if (command.getGenderType() != null) {
            space.setGenderRestriction(GenderType.fromCode(command.getGenderType()));
        }

        // 保存
        spaceRepository.save(space);

        // TODO: 保存扩展属性（宿舍、教室等）

        return space.getId();
    }

    /**
     * 更新场所
     */
    @Transactional
    public void updateSpace(UpdateSpaceCommand command) {
        Space space = spaceRepository.findById(command.getId())
            .orElseThrow(() -> new BusinessException("场所不存在"));

        if (command.getSpaceName() != null) {
            space.updateInfo(command.getSpaceName(), command.getDescription());
        }
        // 更新楼号（仅BUILDING类型）
        if (command.getBuildingNo() != null && !command.getBuildingNo().equals(space.getBuildingNo())) {
            // 校验楼号唯一性
            if (space.getCampusId() != null &&
                spaceRepository.existsByBuildingNoInCampus(command.getBuildingNo(), space.getCampusId(), space.getId())) {
                throw new BusinessException("该校区内楼号\"" + command.getBuildingNo() + "\"已存在");
            }
            space.updateBuildingNo(command.getBuildingNo());
        }
        // 更新房间号（仅ROOM类型）
        if (command.getRoomNo() != null && !command.getRoomNo().equals(space.getRoomNo())) {
            // 校验房间号唯一性
            if (space.getBuildingId() != null &&
                spaceRepository.existsByRoomNoInBuilding(command.getRoomNo(), space.getBuildingId(), space.getId())) {
                throw new BusinessException("该楼栋内房间号\"" + command.getRoomNo() + "\"已存在");
            }
            space.updateRoomNo(command.getRoomNo());
        }
        if (command.getCapacity() != null) {
            space.updateCapacity(command.getCapacity());
        }
        if (command.getOrgUnitId() != null) {
            space.assignToOrgUnit(command.getOrgUnitId());
        }
        // 班级分配（允许设置为null来取消分配）
        if (command.getClassId() != null) {
            space.assignToClass(command.getClassId());
        }
        if (command.getResponsibleUserId() != null) {
            space.assignResponsible(command.getResponsibleUserId());
        }
        if (command.getAttributes() != null) {
            space.updateAttributes(command.getAttributes());
        }
        // 性别限制
        if (command.getGenderType() != null) {
            space.setGenderRestriction(GenderType.fromCode(command.getGenderType()));
        }

        spaceRepository.save(space);

        // TODO: 更新扩展属性
    }

    /**
     * 删除场所
     */
    @Transactional
    public void deleteSpace(Long id, boolean force) {
        Space space = spaceRepository.findById(id)
            .orElseThrow(() -> new BusinessException("场所不存在"));

        // 检查是否有子节点
        if (spaceRepository.hasChildren(id)) {
            if (force) {
                // 递归删除子节点
                List<Space> descendants = spaceRepository.findByPathPrefix(space.getPath().getValue());
                for (Space descendant : descendants) {
                    spaceRepository.delete(descendant.getId());
                }
            } else {
                throw new BusinessException("该场所有子节点，无法删除");
            }
        }

        // 检查是否有占用者
        if (space.getCurrentOccupancy() > 0) {
            throw new BusinessException("该场所有占用者，无法删除");
        }

        spaceRepository.delete(id);
    }

    /**
     * 变更状态
     */
    @Transactional
    public void changeStatus(Long id, SpaceStatus status) {
        Space space = spaceRepository.findById(id)
            .orElseThrow(() -> new BusinessException("场所不存在"));

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

        spaceRepository.save(space);
    }

    /**
     * 入住
     */
    @Transactional
    public Long checkIn(CheckInCommand command) {
        Space space = spaceRepository.findById(command.getSpaceId())
            .orElseThrow(() -> new BusinessException("场所不存在"));

        // 验证是否可入住
        if (!space.canCheckIn()) {
            throw new BusinessException("该场所无法入住");
        }

        // 验证位置是否已被占用
        if (command.getPositionNo() != null &&
            occupantRepository.isPositionOccupied(command.getSpaceId(), command.getPositionNo())) {
            throw new BusinessException("该位置已被占用");
        }

        // 验证占用者是否已有其他在住记录
        if (occupantRepository.hasActiveOccupancy(command.getOccupantType(), command.getOccupantId())) {
            throw new BusinessException("该" + command.getOccupantType().getDescription() + "已有在住记录");
        }

        // 创建占用记录
        SpaceOccupant occupant = SpaceOccupant.create(
            command.getSpaceId(),
            command.getOccupantType(),
            command.getOccupantId(),
            command.getPositionNo()
        );
        if (command.getRemark() != null) {
            occupant.setRemark(command.getRemark());
        }

        occupantRepository.save(occupant);

        // 更新场所占用数
        space.checkIn();
        spaceRepository.save(space);

        return occupant.getId();
    }

    /**
     * 退出
     */
    @Transactional
    public void checkOut(Long spaceId, Long occupantId) {
        Space space = spaceRepository.findById(spaceId)
            .orElseThrow(() -> new BusinessException("场所不存在"));

        // 查找在住记录
        // 这里的 occupantId 是占用记录的ID，不是占用者ID
        SpaceOccupant occupant = occupantRepository.findById(occupantId)
            .orElseThrow(() -> new BusinessException("占用记录不存在"));

        if (!occupant.getSpaceId().equals(spaceId)) {
            throw new BusinessException("占用记录不属于该场所");
        }

        if (!occupant.isActive()) {
            throw new BusinessException("该记录已退出");
        }

        // 退出
        occupant.checkOut();
        occupantRepository.save(occupant);

        // 更新场所占用数
        space.checkOut();
        spaceRepository.save(space);
    }

    /**
     * 批量分配组织单元
     */
    @Transactional
    public void batchAssignOrgUnit(List<Long> spaceIds, Long orgUnitId) {
        spaceRepository.batchUpdateOrgUnit(spaceIds, orgUnitId);
    }

    /**
     * 批量分配班级（直接分配到space表）
     */
    @Transactional
    public void batchAssignClass(List<Long> spaceIds, Long classId) {
        for (Long spaceId : spaceIds) {
            Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new BusinessException("场所不存在: " + spaceId));
            space.assignToClass(classId);
            spaceRepository.save(space);
        }
    }

    /**
     * 添加场所-班级分配（多对多关系）
     */
    @Transactional
    public Long addClassAssignment(Long spaceId, Long classId, Long orgUnitId,
                                    Integer assignedBeds, Long assignedBy) {
        // 检查是否已存在
        if (classAssignmentRepository.existsBySpaceIdAndClassId(spaceId, classId)) {
            throw new BusinessException("该场所已分配给此班级");
        }

        SpaceClassAssignment assignment = SpaceClassAssignment.create(
            spaceId, classId, orgUnitId, assignedBeds, assignedBy);
        classAssignmentRepository.save(assignment);
        return assignment.getId();
    }

    /**
     * 移除场所-班级分配
     */
    @Transactional
    public void removeClassAssignment(Long spaceId, Long classId) {
        classAssignmentRepository.findBySpaceIdAndClassId(spaceId, classId)
            .ifPresent(a -> classAssignmentRepository.delete(a.getId()));
    }

    /**
     * 获取场所的班级分配列表
     */
    public List<SpaceClassAssignment> getClassAssignmentsBySpace(Long spaceId) {
        return classAssignmentRepository.findBySpaceId(spaceId);
    }

    /**
     * 获取班级的场所分配列表
     */
    public List<SpaceClassAssignment> getClassAssignmentsByClass(Long classId) {
        return classAssignmentRepository.findByClassId(classId);
    }

    /**
     * 取消班级分配
     */
    @Transactional
    public void unassignClass(Long spaceId) {
        Space space = spaceRepository.findById(spaceId)
            .orElseThrow(() -> new BusinessException("场所不存在"));
        space.unassignFromClass();
        spaceRepository.save(space);
    }

    /**
     * 设置性别限制
     */
    @Transactional
    public void setGenderRestriction(Long spaceId, Integer genderType) {
        Space space = spaceRepository.findById(spaceId)
            .orElseThrow(() -> new BusinessException("场所不存在"));
        space.setGenderRestriction(GenderType.fromCode(genderType));
        spaceRepository.save(space);
    }

    // ========== 查询方法 ==========

    /**
     * 获取场所详情（带关联信息）
     */
    public SpaceDTO getById(Long id) {
        // 使用带关联信息的查询
        SpacePO po = spaceMapper.selectByIdWithRelations(id);
        if (po == null) {
            throw new BusinessException("场所不存在");
        }
        return toDTO(po);
    }

    /**
     * 获取场所树
     */
    public List<SpaceDTO> getTree(BuildingType buildingType, boolean includeStatistics) {
        List<Space> campuses = spaceRepository.findAllCampuses();
        List<SpaceDTO> result = new ArrayList<>();

        for (Space campus : campuses) {
            SpaceDTO campusDTO = toDTO(campus);
            campusDTO.setChildren(new ArrayList<>());

            List<Space> buildings = spaceRepository.findChildren(campus.getId());
            for (Space building : buildings) {
                if (buildingType != null && building.getBuildingType() != buildingType) {
                    continue;
                }

                SpaceDTO buildingDTO = toDTO(building);
                buildingDTO.setChildren(new ArrayList<>());

                if (includeStatistics) {
                    SpaceRepository.SpaceBuildingStats stats = spaceRepository.getBuildingStats(building.getId());
                    buildingDTO.setCapacity(stats.getTotalCapacity());
                    buildingDTO.setCurrentOccupancy(stats.getTotalOccupancy());
                    if (stats.getTotalCapacity() > 0) {
                        buildingDTO.setOccupancyRate((double) stats.getTotalOccupancy() / stats.getTotalCapacity() * 100);
                    }
                }

                List<Space> floors = spaceRepository.findFloorsByBuildingId(building.getId());
                for (Space floor : floors) {
                    SpaceDTO floorDTO = toDTO(floor);
                    floorDTO.setChildren(new ArrayList<>());

                    List<Space> rooms = spaceRepository.findRoomsByFloorId(floor.getId());
                    for (Space room : rooms) {
                        floorDTO.getChildren().add(toDTO(room));
                    }

                    buildingDTO.getChildren().add(floorDTO);
                }

                campusDTO.getChildren().add(buildingDTO);
            }

            result.add(campusDTO);
        }

        return result;
    }

    /**
     * 获取楼宇列表
     */
    public List<SpaceDTO> getBuildings(BuildingType buildingType, SpaceStatus status) {
        return spaceRepository.findAllBuildings(buildingType, status).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * 分页查询
     */
    public List<SpaceDTO> query(SpaceQueryCriteria criteria) {
        return spaceRepository.findByConditions(
            criteria.getSpaceType(),
            criteria.getRoomType(),
            criteria.getBuildingType(),
            criteria.getBuildingId(),
            criteria.getFloorNumber(),
            criteria.getOrgUnitId(),
            criteria.getStatus(),
            criteria.getKeyword(),
            criteria.getOffset(),
            criteria.getPageSize()
        ).stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 统计数量
     */
    public long count(SpaceQueryCriteria criteria) {
        return spaceRepository.countByConditions(
            criteria.getSpaceType(),
            criteria.getRoomType(),
            criteria.getBuildingType(),
            criteria.getBuildingId(),
            criteria.getFloorNumber(),
            criteria.getOrgUnitId(),
            criteria.getStatus(),
            criteria.getKeyword()
        );
    }

    /**
     * 获取子节点
     */
    public List<SpaceDTO> getChildren(Long parentId) {
        return spaceRepository.findChildren(parentId).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * 获取祖先链
     */
    public List<SpaceDTO> getAncestors(Long id) {
        return spaceRepository.findAncestors(id).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * 获取场所占用者列表
     */
    public List<SpaceOccupantDTO> getOccupants(Long spaceId) {
        return occupantRepository.findActiveBySpaceId(spaceId).stream()
            .map(this::toOccupantDTO)
            .collect(Collectors.toList());
    }

    /**
     * 获取场所占用历史
     */
    public List<SpaceOccupantDTO> getOccupantHistory(Long spaceId) {
        return occupantRepository.findAllBySpaceId(spaceId).stream()
            .map(this::toOccupantDTO)
            .collect(Collectors.toList());
    }

    // ========== 私有方法 ==========

    private Space getParentSpace(Long parentId, SpaceType expectedType) {
        if (parentId == null) {
            throw new BusinessException("父级ID不能为空");
        }
        Space parent = spaceRepository.findById(parentId)
            .orElseThrow(() -> new BusinessException("父级场所不存在"));
        if (parent.getSpaceType() != expectedType) {
            throw new BusinessException("父级场所类型不正确，期望: " + expectedType.getDescription());
        }
        return parent;
    }

    private String generateCode(String prefix) {
        // 简单的编码生成，实际应使用序列或UUID
        return prefix + "-" + System.currentTimeMillis() % 100000;
    }

    private String generateRoomCode(Space floor) {
        // 基于楼层生成房间编码
        String buildingCode = floor.getPath().getAncestorIds().get(1).toString();
        return floor.getFloorNumber() + String.format("%02d", System.currentTimeMillis() % 100);
    }

    private SpaceDTO toDTO(Space space) {
        SpaceDTO dto = new SpaceDTO();
        dto.setId(space.getId());
        dto.setSpaceCode(space.getSpaceCode());
        dto.setSpaceName(space.getSpaceName());
        dto.setSpaceType(space.getSpaceType().name());
        dto.setRoomType(space.getRoomType() != null ? space.getRoomType().name() : null);
        dto.setBuildingType(space.getBuildingType() != null ? space.getBuildingType().name() : null);
        // 楼号和房间号
        dto.setBuildingNo(space.getBuildingNo());
        dto.setRoomNo(space.getRoomNo());
        dto.setParentId(space.getParentId());
        dto.setPath(space.getPath() != null ? space.getPath().getValue() : null);
        dto.setLevel(space.getLevel());
        dto.setCampusId(space.getCampusId());
        dto.setBuildingId(space.getBuildingId());
        dto.setFloorNumber(space.getFloorNumber());
        dto.setCapacity(space.getMaxCapacity());
        dto.setCurrentOccupancy(space.getCurrentOccupancy());
        dto.setAvailableCapacity(space.getAvailableCapacity());
        dto.setOccupancyRate(space.getOccupancyRate());
        dto.setOrgUnitId(space.getOrgUnitId());
        dto.setClassId(space.getClassId());
        dto.setResponsibleUserId(space.getResponsibleUserId());
        // 性别类型
        if (space.getGenderType() != null) {
            dto.setGenderType(space.getGenderType().getCode());
            dto.setGenderTypeText(space.getGenderType().getDescription());
        }
        dto.setStatus(space.getStatus().getCode());
        dto.setStatusText(space.getStatus().getDescription());
        dto.setAttributes(space.getAttributes());
        dto.setDescription(space.getDescription());
        dto.setCreatedAt(space.getCreatedAt());
        dto.setUpdatedAt(space.getUpdatedAt());

        // 如果是房间，查询关联信息（楼栋号、楼层、班级、班主任等）
        if (space.getSpaceType() == SpaceType.ROOM) {
            SpacePO poWithRelations = spaceMapper.selectByIdWithRelations(space.getId());
            if (poWithRelations != null) {
                dto.setParentBuildingNo(poWithRelations.getParentBuildingNo());
                dto.setBuildingName(poWithRelations.getBuildingName());
                dto.setFloorId(poWithRelations.getFloorId());
                dto.setFloorName(poWithRelations.getFloorName());
                dto.setOrgUnitName(poWithRelations.getOrgUnitName());
                if (space.getClassId() != null) {
                    dto.setClassName(poWithRelations.getClassName());
                    dto.setClassTeacherId(poWithRelations.getClassTeacherId());
                    dto.setClassTeacherName(poWithRelations.getClassTeacherName());
                    dto.setClassTeacherPhone(poWithRelations.getClassTeacherPhone());
                }
            }
        }
        return dto;
    }

    /**
     * 从SpacePO转换为DTO（带关联信息）
     */
    private SpaceDTO toDTO(SpacePO po) {
        SpaceDTO dto = new SpaceDTO();
        dto.setId(po.getId());
        dto.setSpaceCode(po.getSpaceCode());
        dto.setSpaceName(po.getSpaceName());
        dto.setSpaceType(po.getSpaceType());
        dto.setRoomType(po.getRoomType());
        dto.setBuildingType(po.getBuildingType());
        // 楼号和房间号
        dto.setBuildingNo(po.getBuildingNo());
        dto.setRoomNo(po.getRoomNo());
        dto.setParentBuildingNo(po.getParentBuildingNo());
        dto.setParentId(po.getParentId());
        dto.setParentName(po.getParentName());
        dto.setPath(po.getPath());
        dto.setLevel(po.getLevel());
        dto.setCampusId(po.getCampusId());
        dto.setCampusName(po.getCampusName());
        dto.setBuildingId(po.getBuildingId());
        dto.setBuildingName(po.getBuildingName());
        dto.setFloorId(po.getFloorId());
        dto.setFloorName(po.getFloorName());
        dto.setFloorNumber(po.getFloorNumber());
        dto.setCapacity(po.getCapacity());
        dto.setCurrentOccupancy(po.getCurrentOccupancy() != null ? po.getCurrentOccupancy() : 0);
        int available = (po.getCapacity() != null ? po.getCapacity() : 0) - dto.getCurrentOccupancy();
        dto.setAvailableCapacity(Math.max(0, available));
        if (po.getCapacity() != null && po.getCapacity() > 0) {
            dto.setOccupancyRate((double) dto.getCurrentOccupancy() / po.getCapacity() * 100);
        } else {
            dto.setOccupancyRate(0.0);
        }
        dto.setOrgUnitId(po.getOrgUnitId());
        dto.setOrgUnitName(po.getOrgUnitName());
        dto.setClassId(po.getClassId());
        dto.setClassName(po.getClassName());
        dto.setClassTeacherId(po.getClassTeacherId());
        dto.setClassTeacherName(po.getClassTeacherName());
        dto.setClassTeacherPhone(po.getClassTeacherPhone());
        dto.setResponsibleUserId(po.getResponsibleUserId());
        dto.setResponsibleUserName(po.getResponsibleUserName());
        // 性别类型
        GenderType genderType = GenderType.fromCode(po.getGenderType());
        dto.setGenderType(genderType.getCode());
        dto.setGenderTypeText(genderType.getDescription());
        // 状态
        SpaceStatus status = SpaceStatus.fromCode(po.getStatus() != null ? po.getStatus() : 1);
        dto.setStatus(status.getCode());
        dto.setStatusText(status.getDescription());
        dto.setDescription(po.getDescription());
        dto.setCreatedAt(po.getCreatedAt());
        dto.setUpdatedAt(po.getUpdatedAt());
        return dto;
    }

    private SpaceOccupantDTO toOccupantDTO(SpaceOccupant occupant) {
        SpaceOccupantDTO dto = new SpaceOccupantDTO();
        dto.setId(occupant.getId());
        dto.setSpaceId(occupant.getSpaceId());
        dto.setOccupantType(occupant.getOccupantType().name());
        dto.setOccupantId(occupant.getOccupantId());
        dto.setOccupantName(occupant.getOccupantName());
        dto.setOccupantNo(occupant.getOccupantNo());
        dto.setPositionNo(occupant.getPositionNo());
        dto.setCheckInDate(occupant.getCheckInDate());
        dto.setCheckOutDate(occupant.getCheckOutDate());
        dto.setStatus(occupant.getStatus());
        dto.setStatusText(occupant.isActive() ? "在住" : "已退出");
        dto.setRemark(occupant.getRemark());
        dto.setCreatedAt(occupant.getCreatedAt());
        return dto;
    }
}
