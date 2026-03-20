package com.school.management.infrastructure.persistence.place;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.place.model.aggregate.Place;
import com.school.management.domain.place.model.valueobject.*;
import com.school.management.domain.place.repository.PlaceRepository;
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
public class PlaceRepositoryImpl implements PlaceRepository {

    private final PlaceMapper placeMapper;
    private final ObjectMapper objectMapper;

    @Override
    public Place save(Place place) {
        PlacePO po = toPO(place);
        if (place.getId() == null) {
            placeMapper.insert(po);
            place.setId(po.getId());
            // 更新路径
            if (place.getParentId() != null) {
                PlacePO parent = placeMapper.selectById(place.getParentId());
                String newPath = (parent != null ? parent.getPath() : "/") + po.getId() + "/";
                po.setPath(newPath);
                placeMapper.updateById(po);
                place.updatePath(PlacePath.of(newPath));
            } else {
                String newPath = "/" + po.getId() + "/";
                po.setPath(newPath);
                placeMapper.updateById(po);
                place.updatePath(PlacePath.of(newPath));
            }
        } else {
            placeMapper.updateById(po);
        }
        return place;
    }

    @Override
    public Optional<Place> findById(Long id) {
        PlacePO po = placeMapper.selectByIdWithRelations(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<Place> findByCode(String placeCode) {
        PlacePO po = placeMapper.selectByCode(placeCode);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public boolean existsByCode(String placeCode) {
        return placeMapper.selectByCode(placeCode) != null;
    }

    @Override
    public boolean existsByBuildingNoInCampus(Integer buildingNo, Long campusId, Long excludeId) {
        return placeMapper.countByBuildingNoInCampus(buildingNo, campusId, excludeId) > 0;
    }

    @Override
    public boolean existsByRoomNoInBuilding(Integer roomNo, Long buildingId, Long excludeId) {
        return placeMapper.countByRoomNoInBuilding(roomNo, buildingId, excludeId) > 0;
    }

    @Override
    public void delete(Long id) {
        PlacePO po = new PlacePO();
        po.setId(id);
        po.setDeleted(1);
        placeMapper.updateById(po);
    }

    @Override
    public List<Place> findAllCampuses() {
        return placeMapper.selectAllCampuses().stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Place> findAllBuildings(BuildingType buildingType, PlaceStatus status) {
        String bt = buildingType != null ? buildingType.name() : null;
        Integer st = status != null ? status.getCode() : null;
        return placeMapper.selectAllBuildings(bt, st).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Place> findChildren(Long parentId) {
        return placeMapper.selectChildren(parentId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Place> findFloorsByBuildingId(Long buildingId) {
        return placeMapper.selectFloorsByBuildingId(buildingId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Place> findRoomsByBuildingId(Long buildingId, RoomType roomType, Integer floorNumber) {
        String rt = roomType != null ? roomType.name() : null;
        return placeMapper.selectRoomsByBuildingId(buildingId, rt, floorNumber).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Place> findRoomsByFloorId(Long floorId) {
        return placeMapper.selectChildren(floorId).stream()
            .filter(po -> "ROOM".equals(po.getPlaceType()))
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Place> findRoomsByType(RoomType roomType, Long buildingId, PlaceStatus status) {
        String rt = roomType != null ? roomType.name() : null;
        Integer st = status != null ? status.getCode() : null;
        return placeMapper.selectByConditions("ROOM", rt, null, buildingId, null, null, st, null, 0, Integer.MAX_VALUE)
            .stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Place> findByOrgUnitId(Long orgUnitId) {
        return placeMapper.selectByOrgUnitId(orgUnitId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Place> findByPathPrefix(String pathPrefix) {
        return placeMapper.selectByPathPrefix(pathPrefix).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Place> findByConditions(PlaceType placeType, RoomType roomType, BuildingType buildingType,
                                         Long buildingId, Integer floorNumber, Long orgUnitId,
                                         PlaceStatus status, String keyword, int offset, int limit) {
        String st = placeType != null ? placeType.name() : null;
        String rt = roomType != null ? roomType.name() : null;
        String bt = buildingType != null ? buildingType.name() : null;
        Integer statusCode = status != null ? status.getCode() : null;

        return placeMapper.selectByConditions(st, rt, bt, buildingId, floorNumber, orgUnitId, statusCode, keyword, offset, limit)
            .stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public long countByConditions(PlaceType placeType, RoomType roomType, BuildingType buildingType,
                                  Long buildingId, Integer floorNumber, Long orgUnitId,
                                  PlaceStatus status, String keyword) {
        String st = placeType != null ? placeType.name() : null;
        String rt = roomType != null ? roomType.name() : null;
        String bt = buildingType != null ? buildingType.name() : null;
        Integer statusCode = status != null ? status.getCode() : null;

        return placeMapper.countByConditions(st, rt, bt, buildingId, floorNumber, orgUnitId, statusCode, keyword);
    }

    @Override
    public void updateOccupancy(Long placeId, int occupancy) {
        placeMapper.updateOccupancy(placeId, occupancy);
    }

    @Override
    public void batchUpdateOrgUnit(List<Long> placeIds, Long orgUnitId) {
        if (placeIds != null && !placeIds.isEmpty()) {
            placeMapper.batchUpdateOrgUnit(placeIds, orgUnitId);
        }
    }

    @Override
    public boolean hasChildren(Long parentId) {
        return placeMapper.countChildren(parentId) > 0;
    }

    @Override
    public List<Place> findAncestors(Long placeId) {
        Optional<Place> place = findById(placeId);
        if (place.isEmpty() || place.get().getPath() == null) {
            return Collections.emptyList();
        }
        return placeMapper.selectAncestors(place.get().getPath().getValue()).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public PlaceBuildingStats getBuildingStats(Long buildingId) {
        Map<String, Object> stats = placeMapper.selectBuildingStats(buildingId);
        if (stats == null) {
            return new PlaceBuildingStatsImpl(0, 0, 0, 0);
        }
        return new PlaceBuildingStatsImpl(
            ((Number) stats.getOrDefault("totalFloors", 0)).intValue(),
            ((Number) stats.getOrDefault("totalRooms", 0)).intValue(),
            ((Number) stats.getOrDefault("totalCapacity", 0)).intValue(),
            ((Number) stats.getOrDefault("totalOccupancy", 0)).intValue()
        );
    }

    @Override
    public boolean incrementOccupancyIfAvailable(Long placeId, int increment) {
        int affectedRows = placeMapper.incrementOccupancyIfAvailable(placeId, increment);
        return affectedRows > 0;
    }

    @Override
    public boolean decrementOccupancy(Long placeId, int decrement) {
        int affectedRows = placeMapper.decrementOccupancy(placeId, decrement);
        return affectedRows > 0;
    }

    // ========== 转换方法 ==========

    private PlacePO toPO(Place place) {
        PlacePO po = new PlacePO();
        po.setId(place.getId());
        po.setPlaceCode(place.getPlaceCode());
        po.setPlaceName(place.getPlaceName());
        po.setPlaceType(place.getPlaceType().name());
        po.setCategoryId(place.getCategoryId());  // V10: 分类ID
        po.setRoomType(place.getRoomType() != null ? place.getRoomType().name() : null);
        po.setBuildingType(place.getBuildingType() != null ? place.getBuildingType().name() : null);
        po.setBuildingNo(place.getBuildingNo());
        po.setRoomNo(place.getRoomNo());
        po.setFloorCount(place.getFloorCount());  // V10: 楼层数
        po.setParentId(place.getParentId());
        po.setPath(place.getPath() != null ? place.getPath().getValue() : null);
        po.setLevel(place.getLevel());
        po.setCampusId(place.getCampusId());
        po.setBuildingId(place.getBuildingId());
        po.setFloorNumber(place.getFloorNumber());
        po.setCapacity(place.getMaxCapacity());
        po.setCurrentOccupancy(place.getCurrentOccupancy());
        po.setOrgUnitId(place.getOrgUnitId());
        po.setClassId(place.getClassId());
        po.setResponsibleUserId(place.getResponsibleUserId());
        po.setGenderType(place.getGenderType() != null ? place.getGenderType().getCode() : 0);
        po.setStatus(place.getStatus().getCode());
        po.setAttributes(toJson(place.getAttributes()));
        po.setDescription(place.getDescription());
        po.setCreatedBy(place.getCreatedBy());
        po.setCreatedAt(place.getCreatedAt());
        po.setUpdatedBy(place.getUpdatedBy());
        po.setUpdatedAt(place.getUpdatedAt());
        po.setDeleted(0);
        return po;
    }

    private Place toDomain(PlacePO po) {
        return Place.reconstitute(
            po.getId(),
            po.getPlaceCode(),
            po.getPlaceName(),
            PlaceType.valueOf(po.getPlaceType()),
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
            PlaceStatus.fromCode(po.getStatus() != null ? po.getStatus() : 1),
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
    private static class PlaceBuildingStatsImpl implements PlaceBuildingStats {
        private final int totalFloors;
        private final int totalRooms;
        private final int totalCapacity;
        private final int totalOccupancy;

        PlaceBuildingStatsImpl(int totalFloors, int totalRooms, int totalCapacity, int totalOccupancy) {
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
