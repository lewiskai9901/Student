package com.school.management.application.place;

import com.school.management.application.place.command.CheckInCommand;
import com.school.management.application.place.command.CreatePlaceCommand;
import com.school.management.application.place.command.UpdatePlaceCommand;
import com.school.management.application.place.query.PlaceDTO;
import com.school.management.application.place.query.PlaceOccupantDTO;
import com.school.management.application.place.query.PlaceQueryCriteria;
import com.school.management.application.place.query.PlaceStatisticsDTO;
import com.school.management.domain.place.model.aggregate.Place;
import com.school.management.domain.place.model.entity.PlaceClassAssignment;
import com.school.management.domain.place.model.entity.PlaceOccupant;
import com.school.management.domain.place.model.valueobject.*;
import com.school.management.domain.place.repository.PlaceClassAssignmentRepository;
import com.school.management.domain.place.repository.PlaceOccupantRepository;
import com.school.management.domain.place.repository.PlaceRepository;
import com.school.management.domain.access.repository.AccessRelationRepository;
import com.school.management.exception.BusinessException;
import com.school.management.infrastructure.persistence.place.PlacePO;
import com.school.management.infrastructure.persistence.place.PlaceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 场所应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceApplicationService {

    private static final AtomicLong CODE_SEQUENCE = new AtomicLong(System.currentTimeMillis());

    private final PlaceRepository placeRepository;
    private final PlaceOccupantRepository occupantRepository;
    private final PlaceClassAssignmentRepository classAssignmentRepository;
    private final AccessRelationRepository accessRelationRepository;
    private final PlaceMapper placeMapper;

    /**
     * 创建场所
     */
    @Transactional
    public Long createPlace(CreatePlaceCommand command) {
        // 验证编码唯一性
        if (command.getPlaceCode() != null && placeRepository.existsByCode(command.getPlaceCode())) {
            throw new BusinessException("场所编码已存在: " + command.getPlaceCode());
        }

        Place place;
        switch (command.getPlaceType()) {
            case CAMPUS:
                String campusCode = command.getPlaceCode() != null ? command.getPlaceCode()
                    : generateCode("CAMPUS");
                place = Place.createCampus(campusCode, command.getPlaceName());
                break;

            case BUILDING:
                Place campus = getParentPlace(command.getParentId(), PlaceType.CAMPUS);
                // 校验楼号唯一性
                Integer buildingNoInt = parseInteger(command.getBuildingNo());
                if (buildingNoInt != null &&
                    placeRepository.existsByBuildingNoInCampus(buildingNoInt, campus.getId(), null)) {
                    throw new BusinessException("该校区内楼号\"" + command.getBuildingNo() + "\"已存在");
                }
                String buildingCode = command.getPlaceCode() != null ? command.getPlaceCode()
                    : generateCode("BLDG");
                place = Place.createBuilding(buildingCode, command.getPlaceName(),
                    command.getBuildingType(), command.getBuildingNo(), campus);
                break;

            case FLOOR:
                Place building = getParentPlace(command.getParentId(), PlaceType.BUILDING);
                if (command.getFloorNumber() == null) {
                    throw new BusinessException("楼层号不能为空");
                }
                place = Place.createFloor(command.getFloorNumber(), building);
                break;

            case ROOM:
                Place floor = getParentPlace(command.getParentId(), PlaceType.FLOOR);
                // 校验房间号唯一性（同一楼栋内）
                Integer roomNoInt = parseInteger(command.getRoomNo());
                if (roomNoInt != null && floor.getBuildingId() != null &&
                    placeRepository.existsByRoomNoInBuilding(roomNoInt, floor.getBuildingId(), null)) {
                    throw new BusinessException("该楼栋内房间号\"" + command.getRoomNo() + "\"已存在");
                }
                String roomCode = command.getPlaceCode() != null ? command.getPlaceCode()
                    : generateRoomCode(floor);
                place = Place.createRoom(roomCode, command.getPlaceName(),
                    command.getRoomType(), command.getCapacity(), command.getRoomNo(), floor);
                break;

            default:
                throw new BusinessException("不支持的场所类型");
        }

        // 设置其他属性
        if (command.getOrgUnitId() != null) {
            place.assignToOrgUnit(command.getOrgUnitId());
        }
        if (command.getClassId() != null) {
            place.assignToClass(command.getClassId());
        }
        if (command.getResponsibleUserId() != null) {
            place.assignResponsible(command.getResponsibleUserId());
        }
        if (command.getDescription() != null) {
            place.updateInfo(place.getPlaceName(), command.getDescription());
        }
        if (command.getAttributes() != null) {
            place.updateAttributes(command.getAttributes());
        }
        // 设置性别限制
        if (command.getGenderType() != null) {
            place.setGenderRestriction(GenderType.fromCode(command.getGenderType()));
        }

        // 保存
        placeRepository.save(place);

        // TODO: 保存扩展属性（宿舍、教室等）

        return place.getId();
    }

    /**
     * 更新场所
     */
    @Transactional
    public void updatePlace(UpdatePlaceCommand command) {
        Place place = placeRepository.findById(command.getId())
            .orElseThrow(() -> new BusinessException("场所不存在"));

        if (command.getPlaceName() != null) {
            place.updateInfo(command.getPlaceName(), command.getDescription());
        }
        // 更新楼号（仅BUILDING类型）
        Integer updateBuildingNo = parseInteger(command.getBuildingNo());
        if (updateBuildingNo != null && !updateBuildingNo.equals(place.getBuildingNo())) {
            // 校验楼号唯一性
            if (place.getCampusId() != null &&
                placeRepository.existsByBuildingNoInCampus(updateBuildingNo, place.getCampusId(), place.getId())) {
                throw new BusinessException("该校区内楼号\"" + command.getBuildingNo() + "\"已存在");
            }
            place.updateBuildingNo(updateBuildingNo);
        }
        // 更新房间号（仅ROOM类型）
        Integer updateRoomNo = parseInteger(command.getRoomNo());
        if (updateRoomNo != null && !updateRoomNo.equals(place.getRoomNo())) {
            // 校验房间号唯一性
            if (place.getBuildingId() != null &&
                placeRepository.existsByRoomNoInBuilding(updateRoomNo, place.getBuildingId(), place.getId())) {
                throw new BusinessException("该楼栋内房间号\"" + command.getRoomNo() + "\"已存在");
            }
            place.updateRoomNo(updateRoomNo);
        }
        if (command.getCapacity() != null) {
            place.updateCapacity(command.getCapacity());
        }
        if (command.getOrgUnitId() != null) {
            place.assignToOrgUnit(command.getOrgUnitId());
        }
        // 班级分配（允许设置为null来取消分配）
        if (command.getClassId() != null) {
            place.assignToClass(command.getClassId());
        }
        if (command.getResponsibleUserId() != null) {
            place.assignResponsible(command.getResponsibleUserId());
        }
        if (command.getAttributes() != null) {
            place.updateAttributes(command.getAttributes());
        }
        // 性别限制
        if (command.getGenderType() != null) {
            place.setGenderRestriction(GenderType.fromCode(command.getGenderType()));
        }

        placeRepository.save(place);

        // TODO: 更新扩展属性
    }

    /**
     * 删除场所
     */
    @Transactional
    public void deletePlace(Long id, boolean force) {
        Place place = placeRepository.findById(id)
            .orElseThrow(() -> new BusinessException("场所不存在"));

        // 检查是否有子节点
        if (placeRepository.hasChildren(id)) {
            if (force) {
                // 递归删除子节点及其关系
                List<Place> descendants = placeRepository.findByPathPrefix(place.getPath().getValue());
                for (Place descendant : descendants) {
                    accessRelationRepository.deleteByResource("place", descendant.getId());
                    placeRepository.delete(descendant.getId());
                }
            } else {
                throw new BusinessException("该场所有子节点，无法删除");
            }
        }

        // Clean up relations for the target place
        accessRelationRepository.deleteByResource("place", id);

        placeRepository.delete(id);
    }

    /**
     * 变更状态
     */
    @Transactional
    public void changeStatus(Long id, PlaceStatus status) {
        Place place = placeRepository.findById(id)
            .orElseThrow(() -> new BusinessException("场所不存在"));

        switch (status) {
            case NORMAL:
                place.enable();
                break;
            case DISABLED:
                place.disable();
                break;
            case MAINTENANCE:
                place.startMaintenance();
                break;
        }

        placeRepository.save(place);
    }

    /**
     * 入住
     */
    @Transactional
    public Long checkIn(CheckInCommand command) {
        Place place = placeRepository.findById(command.getPlaceId())
            .orElseThrow(() -> new BusinessException("场所不存在"));

        // 验证是否可入住
        if (!place.canCheckIn()) {
            throw new BusinessException("该场所无法入住");
        }

        // 验证位置是否已被占用
        if (command.getPositionNo() != null &&
            occupantRepository.isPositionOccupied(command.getPlaceId(), command.getPositionNo())) {
            throw new BusinessException("该位置已被占用");
        }

        // 验证占用者是否已有其他在住记录
        if (occupantRepository.hasActiveOccupancy(command.getOccupantType(), command.getOccupantId())) {
            throw new BusinessException("该" + command.getOccupantType().getDescription() + "已有在住记录");
        }

        // 创建占用记录
        PlaceOccupant occupant = PlaceOccupant.create(
            command.getPlaceId(),
            command.getOccupantType(),
            command.getOccupantId(),
            command.getPositionNo()
        );
        if (command.getRemark() != null) {
            occupant.setRemark(command.getRemark());
        }

        occupantRepository.save(occupant);

        // 更新场所占用数
        place.checkIn();
        placeRepository.save(place);

        return occupant.getId();
    }

    /**
     * 退出
     */
    @Transactional
    public void checkOut(Long placeId, Long occupantId) {
        Place place = placeRepository.findById(placeId)
            .orElseThrow(() -> new BusinessException("场所不存在"));

        // 查找在住记录
        // 这里的 occupantId 是占用记录的ID，不是占用者ID
        PlaceOccupant occupant = occupantRepository.findById(occupantId)
            .orElseThrow(() -> new BusinessException("占用记录不存在"));

        if (!occupant.getPlaceId().equals(placeId)) {
            throw new BusinessException("占用记录不属于该场所");
        }

        if (!occupant.isActive()) {
            throw new BusinessException("该记录已退出");
        }

        // 退出
        occupant.checkOut();
        occupantRepository.save(occupant);

        // 更新场所占用数
        place.checkOut();
        placeRepository.save(place);
    }

    /**
     * 批量分配组织单元
     */
    @Transactional
    public void batchAssignOrgUnit(List<Long> placeIds, Long orgUnitId) {
        placeRepository.batchUpdateOrgUnit(placeIds, orgUnitId);
    }

    /**
     * 批量分配班级（直接分配到place表）
     */
    @Transactional
    public void batchAssignClass(List<Long> placeIds, Long classId) {
        for (Long placeId : placeIds) {
            Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new BusinessException("场所不存在: " + placeId));
            place.assignToClass(classId);
            placeRepository.save(place);
        }
    }

    /**
     * 添加场所-班级分配（多对多关系）
     */
    @Transactional
    public Long addClassAssignment(Long placeId, Long classId, Long orgUnitId,
                                    Integer assignedBeds, Long assignedBy) {
        // 检查是否已存在
        if (classAssignmentRepository.existsByPlaceIdAndClassId(placeId, classId)) {
            throw new BusinessException("该场所已分配给此班级");
        }

        PlaceClassAssignment assignment = PlaceClassAssignment.create(
            placeId, classId, orgUnitId, assignedBeds, assignedBy);
        classAssignmentRepository.save(assignment);
        return assignment.getId();
    }

    /**
     * 移除场所-班级分配
     */
    @Transactional
    public void removeClassAssignment(Long placeId, Long classId) {
        classAssignmentRepository.findByPlaceIdAndClassId(placeId, classId)
            .ifPresent(a -> classAssignmentRepository.delete(a.getId()));
    }

    /**
     * 获取场所的班级分配列表
     */
    public List<PlaceClassAssignment> getClassAssignmentsByPlace(Long placeId) {
        return classAssignmentRepository.findByPlaceId(placeId);
    }

    /**
     * 获取班级的场所分配列表
     */
    public List<PlaceClassAssignment> getClassAssignmentsByClass(Long classId) {
        return classAssignmentRepository.findByClassId(classId);
    }

    /**
     * 取消班级分配
     */
    @Transactional
    public void unassignClass(Long placeId) {
        Place place = placeRepository.findById(placeId)
            .orElseThrow(() -> new BusinessException("场所不存在"));
        place.unassignFromClass();
        placeRepository.save(place);
    }

    /**
     * 设置性别限制
     */
    @Transactional
    public void setGenderRestriction(Long placeId, Integer genderType) {
        Place place = placeRepository.findById(placeId)
            .orElseThrow(() -> new BusinessException("场所不存在"));
        place.setGenderRestriction(GenderType.fromCode(genderType));
        placeRepository.save(place);
    }

    // ========== 查询方法 ==========

    /**
     * 获取场所详情（带关联信息）
     */
    public PlaceDTO getById(Long id) {
        // 使用带关联信息的查询
        PlacePO po = placeMapper.selectByIdWithRelations(id);
        if (po == null) {
            throw new BusinessException("场所不存在");
        }
        return toDTO(po);
    }

    /**
     * 获取场所树
     */
    public List<PlaceDTO> getTree(BuildingType buildingType, boolean includeStatistics) {
        List<Place> campuses = placeRepository.findAllCampuses();
        List<PlaceDTO> result = new ArrayList<>();

        for (Place campus : campuses) {
            PlaceDTO campusDTO = toDTO(campus);
            campusDTO.setChildren(new ArrayList<>());

            List<Place> buildings = placeRepository.findChildren(campus.getId());
            for (Place building : buildings) {
                if (buildingType != null && building.getBuildingType() != buildingType) {
                    continue;
                }

                PlaceDTO buildingDTO = toDTO(building);
                buildingDTO.setChildren(new ArrayList<>());

                if (includeStatistics) {
                    PlaceRepository.PlaceBuildingStats stats = placeRepository.getBuildingStats(building.getId());
                    buildingDTO.setCapacity(stats.getTotalCapacity());
                    buildingDTO.setCurrentOccupancy(stats.getTotalOccupancy());
                    if (stats.getTotalCapacity() > 0) {
                        buildingDTO.setOccupancyRate((double) stats.getTotalOccupancy() / stats.getTotalCapacity() * 100);
                    }
                }

                List<Place> floors = placeRepository.findFloorsByBuildingId(building.getId());
                for (Place floor : floors) {
                    PlaceDTO floorDTO = toDTO(floor);
                    floorDTO.setChildren(new ArrayList<>());

                    List<Place> rooms = placeRepository.findRoomsByFloorId(floor.getId());
                    for (Place room : rooms) {
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
    public List<PlaceDTO> getBuildings(BuildingType buildingType, PlaceStatus status) {
        return placeRepository.findAllBuildings(buildingType, status).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * 分页查询
     */
    public List<PlaceDTO> query(PlaceQueryCriteria criteria) {
        return placeRepository.findByConditions(
            criteria.getPlaceType(),
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
    public long count(PlaceQueryCriteria criteria) {
        return placeRepository.countByConditions(
            criteria.getPlaceType(),
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
    public List<PlaceDTO> getChildren(Long parentId) {
        return placeRepository.findChildren(parentId).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * 获取祖先链
     */
    public List<PlaceDTO> getAncestors(Long id) {
        return placeRepository.findAncestors(id).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * 获取场所占用者列表
     */
    public List<PlaceOccupantDTO> getOccupants(Long placeId) {
        return occupantRepository.findActiveByPlaceId(placeId).stream()
            .map(this::toOccupantDTO)
            .collect(Collectors.toList());
    }

    /**
     * 获取场所占用历史
     */
    public List<PlaceOccupantDTO> getOccupantHistory(Long placeId) {
        return occupantRepository.findAllByPlaceId(placeId).stream()
            .map(this::toOccupantDTO)
            .collect(Collectors.toList());
    }

    // ========== 私有方法 ==========

    private Place getParentPlace(Long parentId, PlaceType expectedType) {
        if (parentId == null) {
            throw new BusinessException("父级ID不能为空");
        }
        Place parent = placeRepository.findById(parentId)
            .orElseThrow(() -> new BusinessException("父级场所不存在"));
        if (parent.getPlaceType() != expectedType) {
            throw new BusinessException("父级场所类型不正确，期望: " + expectedType.getDescription());
        }
        return parent;
    }

    /**
     * 安全地将字符串解析为整数
     */
    private Integer parseInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String generateCode(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String generateRoomCode(Place floor) {
        // 基于楼层生成房间编码，使用AtomicLong保证唯一性
        List<Long> ancestorIds = floor.getPath() != null ? floor.getPath().getAncestorIds() : List.of();
        String buildingCode = ancestorIds.size() > 1
            ? ancestorIds.get(1).toString()
            : "0";
        long seq = CODE_SEQUENCE.incrementAndGet() % 10000;
        return buildingCode + "-" + floor.getFloorNumber() + String.format("%04d", seq);
    }

    private PlaceDTO toDTO(Place place) {
        PlaceDTO dto = new PlaceDTO();
        dto.setId(place.getId());
        dto.setPlaceCode(place.getPlaceCode());
        dto.setPlaceName(place.getPlaceName());
        dto.setPlaceType(place.getPlaceType().name());
        dto.setRoomType(place.getRoomType() != null ? place.getRoomType().name() : null);
        dto.setBuildingType(place.getBuildingType() != null ? place.getBuildingType().name() : null);
        // 楼号和房间号
        dto.setBuildingNo(place.getBuildingNo() != null ? String.valueOf(place.getBuildingNo()) : null);
        dto.setRoomNo(place.getRoomNo() != null ? String.valueOf(place.getRoomNo()) : null);
        dto.setParentId(place.getParentId());
        dto.setPath(place.getPath() != null ? place.getPath().getValue() : null);
        dto.setLevel(place.getLevel());
        dto.setCampusId(place.getCampusId());
        dto.setBuildingId(place.getBuildingId());
        dto.setFloorNumber(place.getFloorNumber());
        dto.setCapacity(place.getMaxCapacity());
        dto.setCurrentOccupancy(place.getCurrentOccupancy());
        dto.setAvailableCapacity(place.getAvailableCapacity());
        dto.setOccupancyRate(place.getOccupancyRate());
        dto.setOrgUnitId(place.getOrgUnitId());
        dto.setClassId(place.getClassId());
        dto.setResponsibleUserId(place.getResponsibleUserId());
        // 性别类型
        if (place.getGenderType() != null) {
            dto.setGenderType(place.getGenderType().getCode());
            dto.setGenderTypeText(place.getGenderType().getDescription());
        }
        dto.setStatus(place.getStatus().getCode());
        dto.setStatusText(place.getStatus().getDescription());
        dto.setAttributes(place.getAttributes());
        dto.setDescription(place.getDescription());
        dto.setCreatedAt(place.getCreatedAt());
        dto.setUpdatedAt(place.getUpdatedAt());

        // 如果是房间，查询关联信息（楼栋号、楼层、班级、班主任等）
        if (place.getPlaceType() == PlaceType.ROOM) {
            PlacePO poWithRelations = placeMapper.selectByIdWithRelations(place.getId());
            if (poWithRelations != null) {
                dto.setParentBuildingNo(poWithRelations.getParentBuildingNo());
                dto.setBuildingName(poWithRelations.getBuildingName());
                dto.setFloorId(poWithRelations.getFloorId());
                dto.setFloorName(poWithRelations.getFloorName());
                dto.setOrgUnitName(poWithRelations.getOrgUnitName());
                if (place.getClassId() != null) {
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
     * 从PlacePO转换为DTO（带关联信息）
     */
    private PlaceDTO toDTO(PlacePO po) {
        PlaceDTO dto = new PlaceDTO();
        dto.setId(po.getId());
        dto.setPlaceCode(po.getPlaceCode());
        dto.setPlaceName(po.getPlaceName());
        dto.setPlaceType(po.getPlaceType());
        dto.setRoomType(po.getRoomType());
        dto.setBuildingType(po.getBuildingType());
        // 楼号和房间号
        dto.setBuildingNo(po.getBuildingNo() != null ? String.valueOf(po.getBuildingNo()) : null);
        dto.setRoomNo(po.getRoomNo() != null ? String.valueOf(po.getRoomNo()) : null);
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
        PlaceStatus status = PlaceStatus.fromCode(po.getStatus() != null ? po.getStatus() : 1);
        dto.setStatus(status.getCode());
        dto.setStatusText(status.getDescription());
        dto.setDescription(po.getDescription());
        dto.setCreatedAt(po.getCreatedAt());
        dto.setUpdatedAt(po.getUpdatedAt());
        return dto;
    }

    private PlaceOccupantDTO toOccupantDTO(PlaceOccupant occupant) {
        PlaceOccupantDTO dto = new PlaceOccupantDTO();
        dto.setId(occupant.getId());
        dto.setPlaceId(occupant.getPlaceId());
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
