package com.school.management.infrastructure.persistence.space;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.space.model.aggregate.Space;
import com.school.management.domain.space.model.valueobject.*;
import com.school.management.domain.space.repository.SpaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 场所仓储实现
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SpaceRepositoryImpl implements SpaceRepository {

    private final SpaceMapper spaceMapper;
    private final ObjectMapper objectMapper;

    @Override
    public Space save(Space space) {
        SpacePO po = toPO(space);
        if (space.getId() == null) {
            spaceMapper.insert(po);
            space.setId(po.getId());
            // 更新路径
            if (space.getParentId() != null) {
                SpacePO parent = spaceMapper.selectById(space.getParentId());
                String newPath = (parent != null ? parent.getPath() : "/") + po.getId() + "/";
                po.setPath(newPath);
                spaceMapper.updateById(po);
                space.updatePath(SpacePath.of(newPath));
            } else {
                String newPath = "/" + po.getId() + "/";
                po.setPath(newPath);
                spaceMapper.updateById(po);
                space.updatePath(SpacePath.of(newPath));
            }
        } else {
            spaceMapper.updateById(po);
        }
        return space;
    }

    @Override
    public Optional<Space> findById(Long id) {
        SpacePO po = spaceMapper.selectByIdWithRelations(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<Space> findByCode(String spaceCode) {
        SpacePO po = spaceMapper.selectByCode(spaceCode);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public boolean existsByCode(String spaceCode) {
        return spaceMapper.selectByCode(spaceCode) != null;
    }

    @Override
    public boolean existsByBuildingNoInCampus(Integer buildingNo, Long campusId, Long excludeId) {
        return spaceMapper.countByBuildingNoInCampus(buildingNo, campusId, excludeId) > 0;
    }

    @Override
    public boolean existsByRoomNoInBuilding(Integer roomNo, Long buildingId, Long excludeId) {
        return spaceMapper.countByRoomNoInBuilding(roomNo, buildingId, excludeId) > 0;
    }

    @Override
    public void delete(Long id) {
        SpacePO po = new SpacePO();
        po.setId(id);
        po.setDeleted(1);
        spaceMapper.updateById(po);
    }

    @Override
    public List<Space> findAllCampuses() {
        return spaceMapper.selectAllCampuses().stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Space> findAllBuildings(BuildingType buildingType, SpaceStatus status) {
        String bt = buildingType != null ? buildingType.name() : null;
        Integer st = status != null ? status.getCode() : null;
        return spaceMapper.selectAllBuildings(bt, st).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Space> findChildren(Long parentId) {
        return spaceMapper.selectChildren(parentId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Space> findFloorsByBuildingId(Long buildingId) {
        return spaceMapper.selectFloorsByBuildingId(buildingId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Space> findRoomsByBuildingId(Long buildingId, RoomType roomType, Integer floorNumber) {
        String rt = roomType != null ? roomType.name() : null;
        return spaceMapper.selectRoomsByBuildingId(buildingId, rt, floorNumber).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Space> findRoomsByFloorId(Long floorId) {
        return spaceMapper.selectChildren(floorId).stream()
            .filter(po -> "ROOM".equals(po.getSpaceType()))
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Space> findRoomsByType(RoomType roomType, Long buildingId, SpaceStatus status) {
        String rt = roomType != null ? roomType.name() : null;
        Integer st = status != null ? status.getCode() : null;
        return spaceMapper.selectByConditions("ROOM", rt, null, buildingId, null, null, st, null, 0, Integer.MAX_VALUE)
            .stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Space> findByOrgUnitId(Long orgUnitId) {
        return spaceMapper.selectByOrgUnitId(orgUnitId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Space> findByPathPrefix(String pathPrefix) {
        return spaceMapper.selectByPathPrefix(pathPrefix).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Space> findByConditions(SpaceType spaceType, RoomType roomType, BuildingType buildingType,
                                         Long buildingId, Integer floorNumber, Long orgUnitId,
                                         SpaceStatus status, String keyword, int offset, int limit) {
        String st = spaceType != null ? spaceType.name() : null;
        String rt = roomType != null ? roomType.name() : null;
        String bt = buildingType != null ? buildingType.name() : null;
        Integer statusCode = status != null ? status.getCode() : null;

        return spaceMapper.selectByConditions(st, rt, bt, buildingId, floorNumber, orgUnitId, statusCode, keyword, offset, limit)
            .stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public long countByConditions(SpaceType spaceType, RoomType roomType, BuildingType buildingType,
                                  Long buildingId, Integer floorNumber, Long orgUnitId,
                                  SpaceStatus status, String keyword) {
        String st = spaceType != null ? spaceType.name() : null;
        String rt = roomType != null ? roomType.name() : null;
        String bt = buildingType != null ? buildingType.name() : null;
        Integer statusCode = status != null ? status.getCode() : null;

        return spaceMapper.countByConditions(st, rt, bt, buildingId, floorNumber, orgUnitId, statusCode, keyword);
    }

    @Override
    public void updateOccupancy(Long spaceId, int occupancy) {
        spaceMapper.updateOccupancy(spaceId, occupancy);
    }

    @Override
    public void batchUpdateOrgUnit(List<Long> spaceIds, Long orgUnitId) {
        if (spaceIds != null && !spaceIds.isEmpty()) {
            spaceMapper.batchUpdateOrgUnit(spaceIds, orgUnitId);
        }
    }

    @Override
    public boolean hasChildren(Long parentId) {
        return spaceMapper.countChildren(parentId) > 0;
    }

    @Override
    public List<Space> findAncestors(Long spaceId) {
        Optional<Space> space = findById(spaceId);
        if (space.isEmpty() || space.get().getPath() == null) {
            return Collections.emptyList();
        }
        return spaceMapper.selectAncestors(space.get().getPath().getValue()).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public SpaceBuildingStats getBuildingStats(Long buildingId) {
        Map<String, Object> stats = spaceMapper.selectBuildingStats(buildingId);
        if (stats == null) {
            return new SpaceBuildingStatsImpl(0, 0, 0, 0);
        }
        return new SpaceBuildingStatsImpl(
            ((Number) stats.getOrDefault("totalFloors", 0)).intValue(),
            ((Number) stats.getOrDefault("totalRooms", 0)).intValue(),
            ((Number) stats.getOrDefault("totalCapacity", 0)).intValue(),
            ((Number) stats.getOrDefault("totalOccupancy", 0)).intValue()
        );
    }

    // ========== 转换方法 ==========

    private SpacePO toPO(Space space) {
        SpacePO po = new SpacePO();
        po.setId(space.getId());
        po.setSpaceCode(space.getSpaceCode());
        po.setSpaceName(space.getSpaceName());
        po.setSpaceType(space.getSpaceType().name());
        po.setCategoryId(space.getCategoryId());  // V10: 分类ID
        po.setRoomType(space.getRoomType() != null ? space.getRoomType().name() : null);
        po.setBuildingType(space.getBuildingType() != null ? space.getBuildingType().name() : null);
        po.setBuildingNo(space.getBuildingNo());
        po.setRoomNo(space.getRoomNo());
        po.setFloorCount(space.getFloorCount());  // V10: 楼层数
        po.setParentId(space.getParentId());
        po.setPath(space.getPath() != null ? space.getPath().getValue() : null);
        po.setLevel(space.getLevel());
        po.setCampusId(space.getCampusId());
        po.setBuildingId(space.getBuildingId());
        po.setFloorNumber(space.getFloorNumber());
        po.setCapacity(space.getMaxCapacity());
        po.setCurrentOccupancy(space.getCurrentOccupancy());
        po.setOrgUnitId(space.getOrgUnitId());
        po.setClassId(space.getClassId());
        po.setResponsibleUserId(space.getResponsibleUserId());
        po.setGenderType(space.getGenderType() != null ? space.getGenderType().getCode() : 0);
        po.setStatus(space.getStatus().getCode());
        po.setAttributes(toJson(space.getAttributes()));
        po.setDescription(space.getDescription());
        po.setCreatedBy(space.getCreatedBy());
        po.setCreatedAt(space.getCreatedAt());
        po.setUpdatedBy(space.getUpdatedBy());
        po.setUpdatedAt(space.getUpdatedAt());
        po.setDeleted(0);
        return po;
    }

    private Space toDomain(SpacePO po) {
        return Space.reconstitute(
            po.getId(),
            po.getSpaceCode(),
            po.getSpaceName(),
            SpaceType.valueOf(po.getSpaceType()),
            po.getCategoryId(),  // V10: 分类ID
            po.getRoomType() != null ? RoomType.valueOf(po.getRoomType()) : null,
            po.getBuildingType() != null ? BuildingType.valueOf(po.getBuildingType()) : null,
            po.getBuildingNo(),
            po.getRoomNo(),
            po.getFloorCount(),  // V10: 楼层数
            po.getParentId(),
            po.getPath(),
            po.getLevel(),
            po.getCampusId(),
            po.getBuildingId(),
            po.getFloorNumber(),
            po.getCapacity(),
            po.getCurrentOccupancy(),
            po.getOrgUnitId(),
            po.getClassId(),
            po.getResponsibleUserId(),
            GenderType.fromCode(po.getGenderType()),
            SpaceStatus.fromCode(po.getStatus() != null ? po.getStatus() : 1),
            fromJson(po.getAttributes()),
            po.getDescription(),
            po.getCreatedBy(),
            po.getCreatedAt(),
            po.getUpdatedBy(),
            po.getUpdatedAt()
        );
    }

    private String toJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize attributes", e);
            return null;
        }
    }

    private Map<String, Object> fromJson(String json) {
        if (json == null || json.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize attributes", e);
            return new HashMap<>();
        }
    }

    /**
     * 楼宇统计实现
     */
    private static class SpaceBuildingStatsImpl implements SpaceBuildingStats {
        private final int totalFloors;
        private final int totalRooms;
        private final int totalCapacity;
        private final int totalOccupancy;

        SpaceBuildingStatsImpl(int totalFloors, int totalRooms, int totalCapacity, int totalOccupancy) {
            this.totalFloors = totalFloors;
            this.totalRooms = totalRooms;
            this.totalCapacity = totalCapacity;
            this.totalOccupancy = totalOccupancy;
        }

        @Override
        public int getTotalFloors() { return totalFloors; }
        @Override
        public int getTotalRooms() { return totalRooms; }
        @Override
        public int getTotalCapacity() { return totalCapacity; }
        @Override
        public int getTotalOccupancy() { return totalOccupancy; }
    }
}
